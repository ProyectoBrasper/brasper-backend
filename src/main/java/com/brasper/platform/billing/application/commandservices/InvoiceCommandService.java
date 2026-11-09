package com.brasper.platform.billing.application.commandservices;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.billing.application.outboundservices.TaxRatesProvider;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.events.InvoiceVoidedEvent;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.commands.VoidInvoiceCommand;
import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceCommandService {

    private final InvoiceRepository invoices;
    private final ProjectLookup projects;
    private final TaxRatesProvider taxRates;
    private final ApplicationEventPublisher events;

    public InvoiceCommandService(InvoiceRepository invoices, ProjectLookup projects, TaxRatesProvider taxRates,
            ApplicationEventPublisher events) {
        this.invoices = invoices;
        this.projects = projects;
        this.taxRates = taxRates;
        this.events = events;
    }

    /** The invoice is for the project's client and uses the tax rates in force on the issue date. */
    @Transactional
    public Result<Invoice, InvoiceCommandFailure> handle(IssueInvoiceCommand command) {
        // shortcut: reports, IGV and balances sum PEN only; accept USD once an exchange rate is stored with the entry.
        if (command.taxableBase().currency() != Currency.PEN) {
            return Result.failure(InvoiceCommandFailure.UNSUPPORTED_CURRENCY);
        }
        var project = projects.find(command.projectId()).orElse(null);
        if (project == null) {
            return Result.failure(InvoiceCommandFailure.PROJECT_NOT_FOUND);
        }
        if (!project.canBeInvoiced()) {
            return Result.failure(InvoiceCommandFailure.PROJECT_NOT_BILLABLE);
        }
        if (invoices.existsByNumber(command.number())) {
            return Result.failure(InvoiceCommandFailure.DUPLICATE_NUMBER);
        }
        // shortcut: two simultaneous requests with one number hit the unique constraint, which the global handler maps to 409.
        Invoice invoice = invoices.save(new Invoice(command, project.clientId(), taxRates.ratesAt(command.issuedOn())));
        events.publishEvent(new InvoiceIssuedEvent(invoice.getId(), invoice.getProjectId(), invoice.getIssuedOn(),
                invoice.getAmounts().taxableBase(), invoice.getAmounts().igv(), invoice.getTotal(), invoice.isAdvance()));
        return Result.success(invoice);
    }

    /** The aggregate enforces these rules too; checking first tells the caller which one failed. */
    @Transactional
    public Result<InvoicePayment, InvoiceCommandFailure> handle(RegisterInvoicePaymentCommand command) {
        Invoice invoice = invoices.findById(command.invoiceId()).orElse(null);
        if (invoice == null) {
            return Result.failure(InvoiceCommandFailure.INVOICE_NOT_FOUND);
        }
        if (invoice.getStatus() == InvoiceStatus.VOIDED) {
            return Result.failure(InvoiceCommandFailure.ALREADY_VOIDED);
        }
        if (command.amount().currency() != invoice.getTotal().currency()) {
            return Result.failure(InvoiceCommandFailure.PAYMENT_CURRENCY_MISMATCH);
        }
        if (command.amount().amount().compareTo(invoice.outstandingBalance().amount()) > 0) {
            return Result.failure(InvoiceCommandFailure.AMOUNT_EXCEEDS_BALANCE);
        }
        invoice.registerPayment(command);
        List<InvoicePayment> saved = invoices.save(invoice).getPayments();
        // Read the payment back from the saved aggregate: that instance is the persisted one, with its id.
        InvoicePayment payment = saved.get(saved.size() - 1);
        events.publishEvent(new InvoicePaymentReceivedEvent(payment.getId(), invoice.getId(), payment.getReceivedOn(),
                payment.getAmount(), payment.getType(), payment.getBankAccountId()));
        return Result.success(payment);
    }

    /** The aggregate also refuses, but checking first tells the caller which rule failed. */
    @Transactional
    public Result<Invoice, InvoiceCommandFailure> handle(VoidInvoiceCommand command) {
        Invoice invoice = invoices.findById(command.invoiceId()).orElse(null);
        if (invoice == null) {
            return Result.failure(InvoiceCommandFailure.INVOICE_NOT_FOUND);
        }
        if (invoice.getStatus() == InvoiceStatus.VOIDED) {
            return Result.failure(InvoiceCommandFailure.ALREADY_VOIDED);
        }
        if (!invoice.getPayments().isEmpty()) {
            return Result.failure(InvoiceCommandFailure.HAS_PAYMENTS);
        }
        invoice.voidInvoice();
        Invoice saved = invoices.save(invoice);
        events.publishEvent(new InvoiceVoidedEvent(saved.getId(), LocalDate.now()));
        return Result.success(saved);
    }
}
