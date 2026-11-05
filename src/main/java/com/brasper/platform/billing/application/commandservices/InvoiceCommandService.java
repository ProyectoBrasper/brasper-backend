package com.brasper.platform.billing.application.commandservices;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.billing.application.outboundservices.TaxRatesProvider;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.VoidInvoiceCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceCommandService {

    private final InvoiceRepository invoices;
    private final ProjectLookup projects;
    private final TaxRatesProvider taxRates;

    public InvoiceCommandService(InvoiceRepository invoices, ProjectLookup projects, TaxRatesProvider taxRates) {
        this.invoices = invoices;
        this.projects = projects;
        this.taxRates = taxRates;
    }

    /** The invoice is for the project's client and uses the tax rates in force on the issue date. */
    @Transactional
    public Result<Invoice, InvoiceCommandFailure> handle(IssueInvoiceCommand command) {
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
        return Result.success(invoices.save(
                new Invoice(command, project.clientId(), taxRates.ratesAt(command.issuedOn()))));
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
        return Result.success(invoices.save(invoice));
    }
}
