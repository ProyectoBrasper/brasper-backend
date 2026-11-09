package com.brasper.platform.billing.application.queryservices;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.entities.InvoicePayment;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.domain.model.valueobjects.PageSpec;
import com.brasper.platform.shared.domain.model.valueobjects.PagedList;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
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
        return invoices.filter(status, projectId, from(issuedIn), to(issuedIn), 0, Integer.MAX_VALUE);
    }

    /** Same filters as {@link #findAll}, one page of them plus the total that match. */
    public PagedList<Invoice> findPage(InvoiceStatus status, Long projectId, Period issuedIn, PageSpec page) {
        return new PagedList<>(invoices.filter(status, projectId, from(issuedIn), to(issuedIn), page.page(), page.size()),
                invoices.countFiltered(status, projectId, from(issuedIn), to(issuedIn)));
    }

    // Postgres cannot type a null date parameter, so "no period" is the widest range instead.
    private static LocalDate from(Period period) {
        return period == null ? LocalDate.of(1900, 1, 1) : period.startDate();
    }

    private static LocalDate to(Period period) {
        return period == null ? LocalDate.of(9999, 12, 31) : period.endDate();
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
