package com.brasper.platform.staff.infrastructure.persistence.jpa;

import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeJpaRepository extends JpaRepository<Employee, Long>, EmployeeRepository { }
