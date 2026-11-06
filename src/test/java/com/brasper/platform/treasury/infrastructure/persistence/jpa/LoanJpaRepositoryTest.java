package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.commands.PayLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class LoanJpaRepositoryTest {
    @Autowired private LoanRepository loans;
    @Autowired private EntityManager em;
    @Test
    void roundTripsLoansAndInstallmentsAddedAfterLoading() {
        Long bankId = ((Number) em.createNativeQuery("""
                insert into bank_accounts (bank, account_number, type, currency, opening_balance, opened_on, created_at, updated_at)
                values ('BCP', 'loans-test', 'CHECKING', 'USD', 0, current_date, now(), now()) returning id
                """, Long.class).getSingleResult()).longValue();
        Money principal = new Money(new BigDecimal("123.45"), Currency.USD);
        Percentage rate = new Percentage(new BigDecimal("0.1234"));
        LocalDate date = LocalDate.of(2026, 11, 30);
        for (LoanType type : LoanType.values()) loans.save(new Loan(new RegisterLoanCommand(type, " lender ", principal, rate, date, bankId)));
        em.flush(); em.clear();
        assertThat(loans.findAllByType(LoanType.GRANTED)).hasSize(1);
        Loan found = loans.findAllByType(LoanType.RECEIVED).getFirst();
        assertThat(found).extracting(Loan::getCounterparty, Loan::getPrincipal, Loan::getInterestRate, Loan::getDisbursedOn, Loan::getBankAccountId)
                .containsExactly("lender", principal, rate, date, bankId);
        assertThat(found.getCreatedAt()).isNotNull().isEqualTo(found.getUpdatedAt());
        found.scheduleInstallment(new ScheduleLoanInstallmentCommand(found.getId(), date, principal, principal));
        loans.save(found); em.flush(); em.clear();
        Loan loaded = loans.findById(found.getId()).orElseThrow();
        assertThat(loaded.getInstallments()).hasSize(1);
        assertThat(loaded.getInstallments().getFirst()).extracting("dueOn", "dueWeek", "principal", "interest", "paid")
                .containsExactly(date, new CashFlowWeek(2026, 11, 6), principal, principal, false);
        loaded.payInstallment(new PayLoanInstallmentCommand(loaded.getId(), loaded.getInstallments().getFirst().getId(), date, bankId));
        loans.save(loaded); em.flush(); em.clear();
        Loan paid = loans.findById(loaded.getId()).orElseThrow();
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.outstandingBalance()).isEqualTo(Money.zero(Currency.USD));
        assertThat(paid.getInstallments().getFirst()).extracting("paid", "paidOn", "bankAccountId").containsExactly(true, date, bankId);
    }
}
