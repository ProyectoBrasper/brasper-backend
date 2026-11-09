package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    Loan save(Loan loan);
    Optional<Loan> findById(Long id);
    List<Loan> findAll();
    /** Every loan with its installments loaded, for sums that read each one. */
    List<Loan> findAllWithInstallments();
    List<Loan> findAllByType(LoanType type);
}
