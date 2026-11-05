package com.brasper.platform.billing.application.queryservices;

import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
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

    /** All filters are optional; newest first. shortcut: filters in memory, move them into the query if invoices reach thousands. */
    public List<Invoice> findAll(InvoiceStatus status, Long projectId, Period issuedIn) {
        return invoices.findAll().stream()
                .filter(invoice -> status == null || invoice.getStatus() == status)
                .filter(invoice -> projectId == null || invoice.getProjectId().equals(projectId))
                .filter(invoice -> issuedIn == null || Period.from(invoice.getIssuedOn()).equals(issuedIn))
                .sorted(Comparator.comparing(Invoice::getIssuedOn).thenComparing(Invoice::getId).reversed())
                .toList();
    }

    public Optional<Invoice> findById(Long id) {
        return invoices.findById(id);
    }
}
