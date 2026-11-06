package com.brasper.platform.accounting.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import org.junit.jupiter.api.Test;

class LedgerAccountTest {
    private static final AccountCode CODE = new AccountCode("10");
    private static LedgerAccount create(String name) {
        return new LedgerAccount(new CreateLedgerAccountCommand(CODE, name, LedgerAccountType.ASSET));
    }
    @Test
    void createsAndDeactivatesIdempotently() {
        LedgerAccount account = create(" Cash ");
        assertThat(account).extracting(LedgerAccount::getCode, LedgerAccount::getName,
                LedgerAccount::getType, LedgerAccount::isActive)
                .containsExactly(CODE, "Cash", LedgerAccountType.ASSET, true);
        account.deactivate();
        assertThat(account.isActive()).isFalse();
        account.deactivate();
        assertThat(account.isActive()).isFalse();
        assertThat(create("x".repeat(200)).getName()).hasSize(200);
    }
    @Test
    void rejectsInvalidNameAndMissingValues() {
        for (String name : new String[]{null, "", " ", "x".repeat(201)})
            assertThatThrownBy(() -> create(name)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LedgerAccount(new CreateLedgerAccountCommand(null, "Cash", LedgerAccountType.ASSET)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new LedgerAccount(new CreateLedgerAccountCommand(CODE, "Cash", null)))
                .isInstanceOf(NullPointerException.class);
    }
}
