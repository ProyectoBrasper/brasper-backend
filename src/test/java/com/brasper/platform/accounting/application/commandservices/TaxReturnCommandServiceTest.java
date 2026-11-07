package com.brasper.platform.accounting.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.brasper.platform.accounting.application.outboundservices.AccountingTreasuryLookup;
import com.brasper.platform.accounting.domain.model.aggregates.*;
import com.brasper.platform.accounting.domain.model.commands.*;
import com.brasper.platform.accounting.domain.model.valueobjects.*;
import com.brasper.platform.accounting.domain.repositories.*;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.SimpleTransactionStatus;

class TaxReturnCommandServiceTest {
    private static final Period PERIOD = new Period(2026, 11);
    private static final LocalDate DAY = PERIOD.endDate();
    private final TaxParametersRepository parameters = mock(TaxParametersRepository.class);
    private final TaxReturnRepository returns = mock(TaxReturnRepository.class);
    private final JournalEntryRepository entries = mock(JournalEntryRepository.class);
    private final LedgerCommandService ledger = mock(LedgerCommandService.class);
    private final AccountingTreasuryLookup treasury = mock(AccountingTreasuryLookup.class);
    private final TaxCommandService service = new TaxCommandService(parameters, returns, entries, ledger, treasury,
            mock(ApplicationEventPublisher.class));
    private static Money pen(String amount) { return new Money(new BigDecimal(amount), Currency.PEN); }
    private static JournalLineData line(String account, String debit, String credit) {
        return new JournalLineData(new AccountCode(account), pen(debit), pen(credit), null);
    }
    private static TaxReturn taxReturn(String output, String input, String prepayment) {
        var value = new TaxReturn(new PrepareTaxReturnCommand(PERIOD), pen(output), pen(input), pen("0"), pen(prepayment));
        ReflectionTestUtils.setField(value, "id", 1L);
        return value;
    }
    private static JournalEntry entry(JournalLineData... lines) {
        return new JournalEntry(new RegisterJournalEntryCommand(DAY, "Test", EntrySource.MANUAL, null, List.of(lines)));
    }
    private void payment(TaxReturn value) {
        when(returns.findById(1L)).thenReturn(Optional.of(value));
        when(treasury.bankAccountExists(2L)).thenReturn(true);
        when(returns.save(any())).thenAnswer(call -> call.getArgument(0));
    }
    private PayTaxReturnCommand pay() { return new PayTaxReturnCommand(1L, DAY, 2L); }

    @Test void preparesFromPenEntriesAndCarriesPreviousCredit() {
        when(entries.findAllByPeriod(PERIOD)).thenReturn(List.of(
                entry(line("10", "1180", "0"), line("70", "0", "1000"), line("40111", "0", "180")),
                entry(line("40111", "50", "0"), line("10", "0", "50")),
                entry(line("70", "100", "0"), line("10", "0", "100")),
                new JournalEntry(new RegisterJournalEntryCommand(DAY, "USD", EntrySource.MANUAL, null, List.of(
                        new JournalLineData(new AccountCode("10"), new Money(BigDecimal.TEN, Currency.USD), Money.zero(Currency.USD), null),
                        new JournalLineData(new AccountCode("40111"), Money.zero(Currency.USD), new Money(BigDecimal.TEN, Currency.USD), null))))));
        when(returns.findByPeriod(PERIOD.previous())).thenReturn(Optional.of(taxReturn("0", "20", "0")));
        when(returns.save(any())).thenAnswer(call -> call.getArgument(0));
        var value = service.handle(new PrepareTaxReturnCommand(PERIOD)).success().orElseThrow();
        assertThat(value.getOutputIgv()).isEqualTo(pen("180"));
        assertThat(value.getInputIgv()).isEqualTo(pen("50"));
        assertThat(value.getCarriedCredit()).isEqualTo(pen("20"));
        assertThat(value.getIncomeTaxPrepayment()).isEqualTo(pen("13.50"));
        assertThat(value.getStatus()).isEqualTo(TaxReturnStatus.DRAFT);
        when(returns.existsByPeriod(PERIOD)).thenReturn(true);
        assertThat(service.handle(new PrepareTaxReturnCommand(PERIOD)).failure()).contains(TaxCommandFailure.DUPLICATE_RETURN);
    }

    @Test void usesEffectiveRatesRoundsAndClampsNegativeSales() {
        var standard = TaxRates.standard();
        var rates = new TaxRates(standard.igv(), standard.detraction(), standard.retention(), new Percentage(new BigDecimal("0.025")));
        when(parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(DAY))
                .thenReturn(Optional.of(new TaxParameters(new SetTaxParametersCommand(rates, DAY))));
        when(returns.save(any())).thenAnswer(call -> call.getArgument(0));
        when(entries.findAllByPeriod(PERIOD)).thenReturn(List.of(entry(line("10", "1", "0"), line("70", "0", "1"))));
        assertThat(service.handle(new PrepareTaxReturnCommand(PERIOD)).success().orElseThrow().getIncomeTaxPrepayment()).isEqualTo(pen("0.03"));
        when(entries.findAllByPeriod(PERIOD)).thenReturn(List.of(entry(line("70", "1", "0"), line("10", "0", "1"))));
        assertThat(service.handle(new PrepareTaxReturnCommand(PERIOD)).success().orElseThrow().getIncomeTaxPrepayment()).isEqualTo(pen("0"));
    }
    @Test void paysWithBalancedTaxAndCashLines() {
        var value = taxReturn("180", "50", "15");
        payment(value);
        when(ledger.handle(any(RegisterJournalEntryCommand.class))).thenAnswer(call -> {
            assertThat(value.getStatus()).isEqualTo(TaxReturnStatus.PAID);
            return Result.success(new JournalEntry(call.getArgument(0)));
        });
        assertThat(service.handle(pay()).success()).contains(value);
        var captured = ArgumentCaptor.forClass(RegisterJournalEntryCommand.class);
        verify(ledger).handle(captured.capture());
        var command = captured.getValue();
        assertThat(command.lines()).containsExactly(line("40111", "130", "0"), line("4017", "15", "0"), line("10", "0", "145"));
        assertThat(command.source()).isEqualTo(EntrySource.TAX_RETURN);
        assertThat(command.sourceId()).isEqualTo(1L);
        assertThat(command.entryDate()).isEqualTo(DAY);
        assertThat(command.description()).isEqualTo("IGV and income tax prepayment 2026-11");
        assertThat(value.getBankAccountId()).isEqualTo(2L);
        assertThat(value.getPaidOn()).isEqualTo(DAY);
        assertThat(service.handle(pay()).failure()).contains(TaxCommandFailure.ALREADY_PAID);
    }

    @Test void rejectsUnknownReturnAndBankWithoutSaving() {
        assertThat(service.handle(pay()).failure()).contains(TaxCommandFailure.TAX_RETURN_NOT_FOUND);
        when(returns.findById(1L)).thenReturn(Optional.of(taxReturn("1", "0", "0")));
        assertThat(service.handle(pay()).failure()).contains(TaxCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        verify(returns, never()).save(any());
        verifyNoInteractions(ledger);
    }

    @Test void paysZeroWithoutAnEntryAndOmitsZeroDebitLines() {
        payment(taxReturn("0", "50", "0"));
        assertThat(service.handle(pay()).isSuccess()).isTrue();
        verifyNoInteractions(ledger);
        payment(taxReturn("0", "50", "15"));
        when(ledger.handle(any(RegisterJournalEntryCommand.class)))
                .thenAnswer(call -> Result.success(new JournalEntry(call.getArgument(0))));
        assertThat(service.handle(pay()).isSuccess()).isTrue();
        var captured = ArgumentCaptor.forClass(RegisterJournalEntryCommand.class);
        verify(ledger).handle(captured.capture());
        assertThat(captured.getValue().lines()).containsExactly(line("4017", "15", "0"), line("10", "0", "15"));
    }

    @Test void ledgerFailuresMarkRollbackAndDoNotSave() {
        for (var failure : List.of(LedgerCommandFailure.ACCOUNT_NOT_FOUND, LedgerCommandFailure.DUPLICATE_SOURCE)) {
            payment(taxReturn("180", "0", "0"));
            when(ledger.handle(any(RegisterJournalEntryCommand.class))).thenReturn(Result.failure(failure));
            var status = new SimpleTransactionStatus();
            try (var transactions = mockStatic(TransactionAspectSupport.class)) {
                transactions.when(TransactionAspectSupport::currentTransactionStatus).thenReturn(status);
                assertThat(service.handle(pay()).failure()).contains(failure == LedgerCommandFailure.DUPLICATE_SOURCE
                        ? TaxCommandFailure.ALREADY_PAID : TaxCommandFailure.ACCOUNTS_NOT_CONFIGURED);
                assertThat(status.isRollbackOnly()).isTrue();
            }
        }
        verify(returns, never()).save(any());
    }
}
