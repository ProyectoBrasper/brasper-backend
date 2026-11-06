package com.brasper.platform.treasury.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PartnerTest {

    @Test
    void trimsPartnerDetailsAndKeepsContributionsReadOnly() {
        Partner partner = new Partner(new RegisterPartnerCommand(" Ana Perez ", " Engineer "));
        assertThat(partner.getFullName()).isEqualTo("Ana Perez");
        assertThat(partner.getRole()).isEqualTo("Engineer");
        assertThat(partner.getContributions()).isEmpty();
        var contribution = partner.registerContribution(new RegisterPartnerContributionCommand(null,
                LocalDate.of(2026, 1, 1), new Money(new BigDecimal("999999999999.99"), Currency.USD),
                false, null, null, " capital "));
        assertThat(partner.getContributions()).containsExactly(contribution);
        assertThat(contribution.getConcept()).isEqualTo("capital");
        assertThat(contribution.isReimbursable()).isFalse();
        assertThatThrownBy(() -> partner.getContributions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidNamesRolesAndContributionAmountsWithoutAddingChildren() {
        for (String name : new String[]{null, " ", "x".repeat(201)}) {
            assertThatThrownBy(() -> new Partner(new RegisterPartnerCommand(name, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new Partner(new RegisterPartnerCommand("Ana", "x".repeat(101))))
                .isInstanceOf(IllegalArgumentException.class);
        Partner partner = new Partner(new RegisterPartnerCommand("Ana", " "));
        assertThat(partner.getRole()).isNull();
        for (String amount : new String[]{"0", "-1", "0.004", "999999999999.999"}) {
            assertThatThrownBy(() -> partner.registerContribution(new RegisterPartnerContributionCommand(null,
                    LocalDate.of(2026, 1, 1), new Money(new BigDecimal(amount), Currency.PEN), true, null, 9L, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(partner.getContributions()).isEmpty();
    }
}
