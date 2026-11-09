package com.brasper.platform.staff.infrastructure.persistence.jpa;

import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixedCostJpaRepository extends JpaRepository<FixedCost, Long>, FixedCostRepository {

    @Query("select c from FixedCost c where (:active is null or c.active = :active) order by c.id")
    List<FixedCost> filterPage(@Param("active") Boolean active, Pageable pageable);

    @Override
    default List<FixedCost> filter(Boolean active, int page, int size) {
        return filterPage(active, PageRequest.of(page, size));
    }

    @Override
    @Query("select count(c) from FixedCost c where (:active is null or c.active = :active)")
    long countFiltered(@Param("active") Boolean active);
}
