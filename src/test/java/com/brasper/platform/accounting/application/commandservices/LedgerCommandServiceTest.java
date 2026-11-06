package com.brasper.platform.accounting.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.aggregates.LedgerAccount;
import com.brasper.platform.accounting.domain.model.commands.CreateLedgerAccountCommand;
import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.commands.ReverseJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;
import com.brasper.platform.accounting.domain.repositories.JournalEntryRepository;
import com.brasper.platform.accounting.domain.repositories.LedgerAccountRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LedgerCommandServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 30);
    private static final AccountCode CASH = new AccountCode("10");
    private static final AccountCode SALES = new AccountCode("70");

    private final LedgerAccountRepository accounts = mock(LedgerAccountRepository.class);
    private final JournalEntryRepository entries = mock(JournalEntryRepository.class);
    private final LedgerCommandService service = new LedgerCommandService(accounts, entries);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static LedgerAccount account(AccountCode code) {
        return new LedgerAccount(new CreateLedgerAccountCommand(code, "Account " + code.value(), LedgerAccountType.ASSET));
    }

    private static RegisterJournalEntryCommand entry(Long sourceId, String credit) {
        return new RegisterJournalEntryCommand(DAY, "Sale", EntrySource.MANUAL, sourceId, List.of(
                new JournalLineData(CASH, pen("100"), pen("0"), null),
                new JournalLineData(SALES, pen("0"), pen(credit), null)));
    }

    private void accountsExist() {
        when(accounts.findAllByCodeIn(any())).thenReturn(List.of(account(CASH), account(SALES)));
        when(entries.save(any(JournalEntry.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createsAnAccountAndRejectsADuplicateCode() {
        when(accounts.save(any(LedgerAccount.class))).thenAnswer(call -> call.getArgument(0));
        when(accounts.existsByCode(CASH)).thenReturn(false, true);
        var command = new CreateLedgerAccountCommand(CASH, "Cash", LedgerAccountType.ASSET);

        assertThat(service.handle(command).isSuccess()).isTrue();
        assertThat(service.handle(command).failure()).contains(LedgerCommandFailure.DUPLICATE_ACCOUNT_CODE);
    }

    @Test
    void registersABalancedEntryAndRejectsTheRest() {
        accountsExist();

        assertThat(service.handle(entry(null, "100")).isSuccess()).isTrue();
        assertThat(service.handle(entry(null, "90")).failure()).contains(LedgerCommandFailure.INVALID_ENTRY);
        verify(entries).save(any(JournalEntry.class));
    }

    @Test
    void rejectsUnknownOrInactiveAccountsAndRepeatedSources() {
        accountsExist();
        LedgerAccount inactive = account(SALES);
        inactive.deactivate();
        when(entries.existsBySourceAndSourceId(EntrySource.MANUAL, 7L)).thenReturn(true);

        assertThat(service.handle(entry(7L, "100")).failure()).contains(LedgerCommandFailure.DUPLICATE_SOURCE);
        when(accounts.findAllByCodeIn(any())).thenReturn(List.of(account(CASH)));
        assertThat(service.handle(entry(null, "100")).failure()).contains(LedgerCommandFailure.ACCOUNT_NOT_FOUND);
        when(accounts.findAllByCodeIn(any())).thenReturn(List.of(account(CASH), inactive));
        assertThat(service.handle(entry(null, "100")).failure()).contains(LedgerCommandFailure.ACCOUNT_INACTIVE);
        verify(entries, never()).save(any());
    }

    @Test
    void reversesOnceAndRejectsUnknownEntries() {
        accountsExist();
        JournalEntry original = service.handle(entry(null, "100")).success().orElseThrow();
        ReflectionTestUtils.setField(original, "id", 1L);
        when(entries.findById(1L)).thenReturn(Optional.of(original));
        when(entries.findById(9L)).thenReturn(Optional.empty());

        JournalEntry reversal = service.handle(new ReverseJournalEntryCommand(1L, DAY.plusDays(1))).success().orElseThrow();

        assertThat(reversal.getSource()).isEqualTo(EntrySource.REVERSAL);
        assertThat(original.isReversed()).isTrue();
        assertThat(service.handle(new ReverseJournalEntryCommand(1L, DAY)).failure())
                .contains(LedgerCommandFailure.ALREADY_REVERSED);
        assertThat(service.handle(new ReverseJournalEntryCommand(9L, DAY)).failure())
                .contains(LedgerCommandFailure.ENTRY_NOT_FOUND);
    }
}
