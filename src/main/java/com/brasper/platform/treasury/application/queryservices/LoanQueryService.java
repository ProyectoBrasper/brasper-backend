package com.brasper.platform.treasury.application.queryservices;

import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LoanQueryService {

    private final LoanRepository loans;

    public LoanQueryService(LoanRepository loans) {
        this.loans = loans;
    }

    public List<Loan> findAll(LoanType type) {
        return type == null ? loans.findAll() : loans.findAllByType(type);
    }

    public Optional<Loan> findById(Long id) {
        return loans.findById(id);
    }
}
