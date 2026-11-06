package com.brasper.platform.treasury.application.queryservices;

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

    public List<Payable> findAll() {
        return payables.findAll();
    }

    public Optional<Payable> findById(Long id) {
        return payables.findById(id);
    }
}
