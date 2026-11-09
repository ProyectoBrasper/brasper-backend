package com.brasper.platform.accounting.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.brasper.platform.accounting.domain.model.aggregates.*;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.domain.repositories.*;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.*;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FixedAssetDepreciationTest {
    private final FixedAssetRepository assets = mock(FixedAssetRepository.class);
    private final LedgerCommandService ledger = mock(LedgerCommandService.class);
    private final FixedAssetCommandService service = new FixedAssetCommandService(
            assets, mock(TaxParametersRepository.class), ledger, mock(JournalEntryRepository.class));
    private FixedAsset asset(Currency currency) {
        return new FixedAsset(new RegisterFixedAssetCommand("Laptop", LocalDate.of(2026, 1, 10),
                new Money(new BigDecimal("1200"), currency), 12, null), TaxRates.standard());
    }
    private RunMonthlyDepreciationCommand month(int month) {
        return new RunMonthlyDepreciationCommand(new Period(2026, month));
    }
    @Test void groupsMonthlyChargesByCurrencyAndPostsBalancedEntries() {
        when(assets.findAll()).thenReturn(List.of(asset(Currency.PEN), asset(Currency.PEN), asset(Currency.USD)));
        when(ledger.handle(any(RegisterJournalEntryCommand.class)))
                .thenAnswer(call -> Result.success(new JournalEntry(call.getArgument(0))));
        var entries = service.handle(month(3)).success().orElseThrow();
        assertThat(entries).hasSize(2);
        var captor = ArgumentCaptor.forClass(RegisterJournalEntryCommand.class);
        verify(ledger, times(2)).handle(captor.capture());
        for (var command : captor.getAllValues()) {
            var total = command.lines().getFirst().debit();
            assertThat(total.amount()).isEqualByComparingTo(total.currency() == Currency.PEN ? "200" : "100");
            assertThat(command.lines().getFirst().accountCode()).isEqualTo(new AccountCode("68"));
            assertThat(command.lines().getLast().accountCode()).isEqualTo(new AccountCode("39"));
            assertThat(command.lines().getLast().credit()).isEqualTo(total);
            assertThat(command.sourceId()).isEqualTo(2026030L + total.currency().ordinal());
            assertThat(command.source()).isEqualTo(EntrySource.DEPRECIATION);
            assertThat(command.description()).isEqualTo("Depreciation 2026-03");
            assertThat(command.entryDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        }
    }
    @Test void acquisitionMonthAndMonthsAfterDisposalHaveNoCharge() {
        var asset = asset(Currency.PEN);
        when(assets.findAll()).thenReturn(List.of(asset));
        assertThat(service.handle(month(1)).failure()).contains(FixedAssetCommandFailure.NOTHING_TO_DEPRECIATE);
        asset.dispose(LocalDate.of(2026, 2, 10));
        assertThat(service.handle(month(3)).failure()).contains(FixedAssetCommandFailure.NOTHING_TO_DEPRECIATE);
        verifyNoInteractions(ledger);
    }
    @Test void rollsBackEarlierCurrencyWhenALaterPostingFails() {
        when(assets.findAll()).thenReturn(List.of(asset(Currency.PEN), asset(Currency.USD)));
        when(ledger.handle(any(RegisterJournalEntryCommand.class))).thenAnswer(call -> {
            RegisterJournalEntryCommand command = call.getArgument(0);
            return command.lines().getFirst().debit().currency() == Currency.PEN
                    ? Result.success(new JournalEntry(command)) : Result.failure(LedgerCommandFailure.ACCOUNT_NOT_FOUND);
        });
        var status = new org.springframework.transaction.support.SimpleTransactionStatus();
        try (var transactions = mockStatic(org.springframework.transaction.interceptor.TransactionAspectSupport.class)) {
            transactions.when(org.springframework.transaction.interceptor.TransactionAspectSupport::currentTransactionStatus)
                    .thenReturn(status);
            assertThat(service.handle(month(3)).failure()).contains(FixedAssetCommandFailure.ACCOUNTS_NOT_CONFIGURED);
            assertThat(status.isRollbackOnly()).isTrue();
            verify(ledger, times(2)).handle(any(RegisterJournalEntryCommand.class));
        }
    }
    @Test void translatesLedgerFailures() {
        when(assets.findAll()).thenReturn(List.of(asset(Currency.PEN)));
        when(ledger.handle(any(RegisterJournalEntryCommand.class)))
                .thenReturn(Result.failure(LedgerCommandFailure.DUPLICATE_SOURCE));
        assertThat(service.handle(month(3)).failure()).contains(FixedAssetCommandFailure.DEPRECIATION_ALREADY_RUN);
        when(ledger.handle(any(RegisterJournalEntryCommand.class)))
                .thenReturn(Result.failure(LedgerCommandFailure.ACCOUNT_INACTIVE));
        assertThat(service.handle(month(3)).failure()).contains(FixedAssetCommandFailure.ACCOUNTS_NOT_CONFIGURED);
    }
}
