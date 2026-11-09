package com.brasper.platform.treasury.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.commandservices.LoanCommandService;
import com.brasper.platform.treasury.application.commandservices.PayableCommandService;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterCashMovementCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.ScheduleLoanInstallmentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementSource;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Uses dates in 1999 so that accounts and movements from other tests never fall inside the ranges. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class CashFiguresTest {

    private static final LocalDate OPENED = LocalDate.of(1999, 1, 1);

    @Autowired private CashFigures cash;
    @Autowired private BankAccountCommandService accounts;
    @Autowired private PayableCommandService payables;
    @Autowired private LoanCommandService loans;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private Long bankAccount() {
        return accounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("995-1"), AccountType.CHECKING,
                Currency.PEN, pen("1000"), OPENED)).success().orElseThrow().getId();
    }

    private void move(Long bank, int day, MovementDirection direction, String amount, MovementSource source) {
        accounts.handle(new RegisterCashMovementCommand(bank, LocalDate.of(1999, 3, day), direction, pen(amount),
                "test", source, null));
    }

    @Test
    void sumsBalancesAndMovementsByDateIgnoringTransfers() {
        Long bank = bankAccount();
        move(bank, 10, MovementDirection.INFLOW, "200", MovementSource.ADJUSTMENT);
        move(bank, 12, MovementDirection.OUTFLOW, "50", MovementSource.EXPENSE);
        move(bank, 13, MovementDirection.OUTFLOW, "999", MovementSource.TRANSFER);

        assertThat(cash.balanceAt(LocalDate.of(1998, 12, 31))).isEqualByComparingTo("0");
        assertThat(cash.balanceAt(LocalDate.of(1999, 3, 11))).isEqualByComparingTo("1200");
        assertThat(cash.balanceAt(LocalDate.of(1999, 3, 31))).isEqualByComparingTo("151");
        assertThat(cash.movementsBetween(LocalDate.of(1999, 3, 1), LocalDate.of(1999, 3, 31)))
                .containsOnlyKeys("INFLOW:ADJUSTMENT", "OUTFLOW:EXPENSE")
                .containsEntry("INFLOW:ADJUSTMENT", new BigDecimal("200.00"));
        assertThat(cash.movementsBetween(LocalDate.of(1999, 3, 11), LocalDate.of(1999, 3, 31)))
                .containsOnlyKeys("OUTFLOW:EXPENSE");
    }

    @Test
    void sumsWhatIsScheduledForAWeek() {
        Long bank = bankAccount();
        CashFlowWeek week = CashFlowWeek.from(LocalDate.of(1999, 3, 10));
        payables.handle(new CreatePayableCommand("Supplier", PayableType.SUPPLIER, pen("300"), null, null, null, week));
        var loan = loans.handle(new RegisterLoanCommand(LoanType.RECEIVED, "Bank", pen("5000"),
                new Percentage(new BigDecimal("0.10")), OPENED, bank)).success().orElseThrow();
        loans.handle(new ScheduleLoanInstallmentCommand(loan.getId(), LocalDate.of(1999, 3, 10), pen("400"), pen("50")));

        assertThat(cash.scheduledPayables(week)).isEqualByComparingTo("300");
        assertThat(cash.outstandingPayables()).isGreaterThanOrEqualTo(new BigDecimal("300"));
        assertThat(cash.scheduledPayables(CashFlowWeek.from(LocalDate.of(1999, 3, 20)))).isEqualByComparingTo("0");
        assertThat(cash.scheduledInstallments(week)).containsOnlyKeys("RECEIVED")
                .containsEntry("RECEIVED", new BigDecimal("450.00"));
    }
}
