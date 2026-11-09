package com.brasper.platform.accounting.application.queryservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountTotals;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalance;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LedgerQueryServiceTest {

    private static final Period NOVEMBER = new Period(2026, 11);
    private static final AccountCode CASH = new AccountCode("10");
    private static final AccountCode SALES = new AccountCode("70");

    private final LedgerAccountRepository accounts = mock(LedgerAccountRepository.class);
    private final JournalEntryRepository entries = mock(JournalEntryRepository.class);
    private final LedgerQueryService service = new LedgerQueryService(accounts, entries);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static LedgerAccount account(AccountCode code, String name, LedgerAccountType type) {
        return new LedgerAccount(new CreateLedgerAccountCommand(code, name, type));
    }

    /** What the database sums for sales of the given total: cash debited, sales credited. */
    private static List<AccountTotals> sales(String amount) {
        return List.of(new AccountTotals(CASH, Currency.PEN, new BigDecimal(amount), BigDecimal.ZERO),
                new AccountTotals(SALES, Currency.PEN, BigDecimal.ZERO, new BigDecimal(amount)));
    }

    @Test
    void sumsThePeriodMovementPerAccountAndListsAccountsByCode() {
        LedgerAccount sales = account(SALES, "Sales", LedgerAccountType.REVENUE);
        LedgerAccount cash = account(CASH, "Cash", LedgerAccountType.ASSET);
        when(accounts.findAll()).thenReturn(List.of(sales, cash));
        when(entries.totalsByPeriod(NOVEMBER)).thenReturn(sales("150.50"));

        TrialBalance balance = service.trialBalance(NOVEMBER);

        assertThat(service.findAllAccounts()).extracting(LedgerAccount::getName).containsExactly("Cash", "Sales");
        assertThat(balance.lines()).hasSize(2);
        assertThat(balance.lines().get(0).accountName()).isEqualTo("Cash");
        assertThat(balance.lines().get(0).debit().amount()).isEqualByComparingTo("150.50");
        assertThat(balance.lines().get(0).credit().amount()).isEqualByComparingTo("0");
        assertThat(balance.lines().get(1).credit().amount()).isEqualByComparingTo("150.50");
    }

    @Test
    void anEmptyPeriodHasNoLines() {
        when(accounts.findAll()).thenReturn(List.of());
        when(entries.totalsByPeriod(NOVEMBER)).thenReturn(List.of());

        assertThat(service.trialBalance(NOVEMBER).lines()).isEmpty();
    }

    @Test
    void balancesAreSignedByAccountTypeAndTheResultIsRevenueMinusExpense() {
        when(accounts.findAll()).thenReturn(List.of(account(CASH, "Cash", LedgerAccountType.ASSET),
                account(SALES, "Sales", LedgerAccountType.REVENUE)));
        LocalDate day = LocalDate.of(2026, 11, 30);
        when(entries.totalsUpTo(day)).thenReturn(sales("150.50"));
        when(entries.totalsByPeriod(NOVEMBER)).thenReturn(sales("100"));

        assertThat(service.accountBalancesAt(day).get("10")).isEqualByComparingTo("150.50");
        assertThat(service.accountBalancesAt(day).get("70")).isEqualByComparingTo("150.50");
        assertThat(service.balancesByTypeAt(day)).containsOnlyKeys("ASSET", "REVENUE");
        assertThat(service.resultByPeriod(NOVEMBER).get("RESULT")).isEqualByComparingTo("100");
        assertThat(service.resultByPeriod(NOVEMBER).get("EXPENSE")).isEqualByComparingTo("0");
    }

    @Test
    void movementsCoverTheDateRange() {
        when(accounts.findAll()).thenReturn(List.of(account(CASH, "Cash", LedgerAccountType.ASSET),
                account(SALES, "Sales", LedgerAccountType.REVENUE)));
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);
        when(entries.totalsBetween(from, to)).thenReturn(sales("40"));

        assertThat(service.accountMovementsBetween(from, to).get("70")).isEqualByComparingTo("40");
    }
}
