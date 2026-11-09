package com.brasper.platform.treasury.infrastructure.persistence.jpa;

import com.brasper.platform.treasury.domain.model.aggregates.BankAccount;
import com.brasper.platform.treasury.domain.repositories.BankAccountRepository;
import com.brasper.platform.treasury.domain.model.valueobjects.MovementTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankAccountJpaRepository extends JpaRepository<BankAccount, Long>, BankAccountRepository {

    @Override
    @Query("""
            select coalesce(sum(a.openingBalance), 0)
                 + coalesce((select sum(case when m.direction = com.brasper.platform.treasury.domain.model.valueobjects.MovementDirection.INFLOW
                                        then m.amount else -m.amount end)
                             from CashMovement m where m.bankAccount.openedOn <= :date and m.occurredOn <= :date), 0)
            from BankAccount a where a.openedOn <= :date
            """)
    BigDecimal sumBalancesAt(@Param("date") LocalDate date);

    @Override
    @Query("""
            select new com.brasper.platform.treasury.domain.model.valueobjects.MovementTotal(m.direction, m.source, sum(m.amount))
            from CashMovement m
            where m.occurredOn between :from and :to
              and m.source <> com.brasper.platform.treasury.domain.model.valueobjects.MovementSource.TRANSFER
            group by m.direction, m.source
            """)
    List<MovementTotal> sumMovementsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
