package com.brasper.platform.staff.application.queryservices;

import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmployeeQueryService {

    private final EmployeeRepository employees;

    public EmployeeQueryService(EmployeeRepository employees) {
        this.employees = employees;
    }

    /** {@code active == null} lists everybody. */
    public List<Employee> findAll(Boolean active) {
        return active == null ? employees.findAll() : employees.findAllByActive(active);
    }

    public Optional<Employee> findById(Long id) {
        return employees.findById(id);
    }
}
