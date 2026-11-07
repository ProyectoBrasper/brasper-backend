package com.brasper.platform.accounting.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PostingRulesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 20);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static InvoiceIssuedEvent issued(boolean advance, String igv) {
        return new InvoiceIssuedEvent(5L, 3L, DAY, pen("1000"), pen(igv), pen("1000").add(pen(igv)), advance);
    }

    private static JournalLineData line(RegisterJournalEntryCommand command, String account) {
        return command.lines().stream().filter(l -> l.accountCode().value().equals(account)).findFirst().orElseThrow();
    }

    @Test
    void anIssuedInvoiceDebitsReceivablesAndCreditsSalesAndIgv() {
        RegisterJournalEntryCommand command = PostingRules.forInvoiceIssued(issued(false, "180"));

        assertThat(command.source()).isEqualTo(EntrySource.INVOICE_ISSUED);
        assertThat(command.sourceId()).isEqualTo(5L);
        assertThat(line(command, "12").debit().amount()).isEqualByComparingTo("1180");
        assertThat(line(command, "70").credit().amount()).isEqualByComparingTo("1000");
        assertThat(line(command, "70").projectId()).isEqualTo(3L);
        assertThat(line(command, "40111").credit().amount()).isEqualByComparingTo("180");
    }

    @Test
    void anAdvanceGoesToCustomerAdvancesAndAnInvoiceWithoutIgvHasNoIgvLine() {
        RegisterJournalEntryCommand command = PostingRules.forInvoiceIssued(issued(true, "0"));

        assertThat(command.lines()).extracting(l -> l.accountCode().value()).containsExactly("12", "122");
    }

    @Test
    void aDepositDebitsCashAndAClearsReceivablesButARetentionCertificateIsLeftOut() {
        var deposit = new InvoicePaymentReceivedEvent(8L, 5L, DAY, pen("500"), InvoicePaymentType.NET_DEPOSIT, 2L);
        var certificate = new InvoicePaymentReceivedEvent(9L, 5L, DAY, pen("35"),
                InvoicePaymentType.RETENTION_CERTIFICATE, null);

        RegisterJournalEntryCommand command = PostingRules.forInvoicePayment(deposit).orElseThrow();

        assertThat(command.source()).isEqualTo(EntrySource.INVOICE_PAYMENT);
        assertThat(command.sourceId()).isEqualTo(8L);
        assertThat(line(command, "10").debit().amount()).isEqualByComparingTo("500");
        assertThat(line(command, "12").credit().amount()).isEqualByComparingTo("500");
        assertThat(PostingRules.forInvoicePayment(certificate)).isEmpty();
    }

    private static ExpenseRegisteredEvent expense(Long projectId, boolean paid, PaymentMethod method) {
        boolean partner = method == PaymentMethod.PARTNER_CONTRIBUTION;
        return new ExpenseRegisteredEvent(4L, DAY, projectId, pen("100"), pen("18"), pen("118"), paid, method,
                partner ? null : 2L, partner ? 6L : null);
    }

    @Test
    void anExpenseDebitsCostAndIgvAndCreditsCashPayablesOrCapital() {
        RegisterJournalEntryCommand paid = PostingRules.forExpense(expense(3L, true, PaymentMethod.BANK_ACCOUNT));
        RegisterJournalEntryCommand pending = PostingRules.forExpense(expense(null, false, PaymentMethod.BANK_ACCOUNT));
        RegisterJournalEntryCommand partner = PostingRules.forExpense(expense(3L, true, PaymentMethod.PARTNER_CONTRIBUTION));

        assertThat(paid.source()).isEqualTo(EntrySource.EXPENSE);
        assertThat(line(paid, "60").debit().amount()).isEqualByComparingTo("100");
        assertThat(line(paid, "60").projectId()).isEqualTo(3L);
        assertThat(line(paid, "40111").debit().amount()).isEqualByComparingTo("18");
        assertThat(line(paid, "10").credit().amount()).isEqualByComparingTo("118");
        assertThat(line(pending, "63").debit().amount()).isEqualByComparingTo("100");
        assertThat(line(pending, "42").credit().amount()).isEqualByComparingTo("118");
        assertThat(line(partner, "50").credit().amount()).isEqualByComparingTo("118");
    }
}
