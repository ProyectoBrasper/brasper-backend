package com.brasper.platform.billing.application.queryservices;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.domain.model.valueobjects.PageSpec;
import com.brasper.platform.shared.domain.model.valueobjects.PagedList;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final InvoiceRepository invoices;

    public InvoiceQueryService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    /** All filters are optional; newest first. */
    public List<Invoice> findAll(InvoiceStatus status, Long projectId, Period issuedIn) {
        return invoices.filter(status, projectId, Period.startOrEarliest(issuedIn), Period.endOrLatest(issuedIn), 0, Integer.MAX_VALUE);
    }

    /** Same filters as {@link #findAll}, one page of them plus the total that match. */
    public PagedList<Invoice> findPage(InvoiceStatus status, Long projectId, Period issuedIn, PageSpec page) {
        return new PagedList<>(invoices.filter(status, projectId, Period.startOrEarliest(issuedIn), Period.endOrLatest(issuedIn), page.page(), page.size()),
                invoices.countFiltered(status, projectId, Period.startOrEarliest(issuedIn), Period.endOrLatest(issuedIn)));
    }

    /** Deposits received from {@code from} to {@code to}, both inclusive (retention certificates are no money). */
    public BigDecimal sumDepositsReceived(LocalDate from, LocalDate to) {
        return invoices.sumDepositsReceived(from, to);
    }

    /** IGV of the invoices issued from {@code from} to {@code to}, both inclusive, voided ones left out. */
    public BigDecimal sumIgvIssued(LocalDate from, LocalDate to) {
        return invoices.sumIgvIssued(from, to);
    }

    /** Empty when the invoice does not exist; oldest first. */
    public Optional<List<InvoicePayment>> findPayments(Long invoiceId) {
        return invoices.findById(invoiceId).map(invoice -> invoice.getPayments().stream()
                .sorted(Comparator.comparing(InvoicePayment::getReceivedOn).thenComparing(InvoicePayment::getId))
                .toList());
    }

    public Optional<Invoice> findById(Long id) {
        return invoices.findById(id);
    }
}
