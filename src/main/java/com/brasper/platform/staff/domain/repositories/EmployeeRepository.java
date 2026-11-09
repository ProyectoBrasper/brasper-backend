package com.brasper.platform.staff.domain.repositories;

import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(Long id);
    Optional<Employee> findByDni(Dni dni);
    boolean existsByDni(Dni dni);
    List<Employee> findAll();
    List<Employee> findAllByActive(boolean active);
}
