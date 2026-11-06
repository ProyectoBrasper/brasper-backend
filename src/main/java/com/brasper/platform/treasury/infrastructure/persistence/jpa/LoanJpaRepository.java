package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanJpaRepository extends JpaRepository<Loan, Long>, LoanRepository {
}
