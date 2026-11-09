package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.shared.domain.model.valueobjects.PageSpec;
import com.brasper.platform.shared.domain.model.valueobjects.PagedList;
import com.brasper.platform.shared.domain.model.valueobjects.PaymentStatus;
import com.brasper.platform.treasury.domain.model.aggregates.Payable;
import com.brasper.platform.treasury.domain.repositories.PayableRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PayableQueryService {

    private final PayableRepository payables;

    public PayableQueryService(PayableRepository payables) {
        this.payables = payables;
    }

    /** The status filter is optional; oldest first. */
    public List<Payable> findAll(PaymentStatus status) {
        return payables.filter(status, 0, Integer.MAX_VALUE);
    }

    /** Same filter as {@link #findAll}, one page of it plus the total that match. */
    public PagedList<Payable> findPage(PaymentStatus status, PageSpec page) {
        return new PagedList<>(payables.filter(status, page.page(), page.size()), payables.countFiltered(status));
    }

    public Optional<Payable> findById(Long id) {
        return payables.findById(id);
    }
}
