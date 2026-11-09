package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.Loan;
import com.brasper.platform.treasury.domain.repositories.LoanRepository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoanJpaRepository extends JpaRepository<Loan, Long>, LoanRepository {

    @Override
    @Query("select distinct l from Loan l left join fetch l.installments")
    List<Loan> findAllWithInstallments();
}
