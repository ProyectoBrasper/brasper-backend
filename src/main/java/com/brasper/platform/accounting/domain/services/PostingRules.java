package com.brasper.platform.accounting.domain.services;

import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The journal entry each business event produces, in PCGE accounts. Pure functions, nothing is persisted here. */
public final class PostingRules {

    static final AccountCode CASH = new AccountCode("10");
    static final AccountCode RECEIVABLES = new AccountCode("12");
    static final AccountCode IGV = new AccountCode("40111");
    static final AccountCode CUSTOMER_ADVANCES = new AccountCode("122");
    static final AccountCode SALES = new AccountCode("70");

    private PostingRules() { }

    /** D 12 total / H 70 taxable base (H 122 for an advance) + H 40111 IGV. */
    public static RegisterJournalEntryCommand forInvoiceIssued(InvoiceIssuedEvent event) {
        List<JournalLineData> lines = new ArrayList<>();
        lines.add(debit(RECEIVABLES, event.total(), null));
        lines.add(credit(event.advance() ? CUSTOMER_ADVANCES : SALES, event.taxableBase(), event.projectId()));
        if (event.igv().isPositive()) {
            lines.add(credit(IGV, event.igv(), null));
        }
        return new RegisterJournalEntryCommand(event.issuedOn(), "Invoice issued", EntrySource.INVOICE_ISSUED,
                event.invoiceId(), lines);
    }

    /**
     * D 10 / H 12 for money that reached the bank. A retention certificate moves no money and is left out.
     * shortcut: the detraction deposit also goes to 10 instead of 107, and retained amounts stay in 12.
     */
    public static Optional<RegisterJournalEntryCommand> forInvoicePayment(InvoicePaymentReceivedEvent event) {
        if (event.type() == InvoicePaymentType.RETENTION_CERTIFICATE) {
            return Optional.empty();
        }
        return Optional.of(new RegisterJournalEntryCommand(event.receivedOn(), "Invoice payment received",
                EntrySource.INVOICE_PAYMENT, event.paymentId(),
                List.of(debit(CASH, event.amount(), null), credit(RECEIVABLES, event.amount(), null))));
    }

    private static JournalLineData debit(AccountCode account, Money amount, Long projectId) {
        return new JournalLineData(account, amount, Money.zero(amount.currency()), projectId);
    }

    private static JournalLineData credit(AccountCode account, Money amount, Long projectId) {
        return new JournalLineData(account, Money.zero(amount.currency()), amount, projectId);
    }
}
