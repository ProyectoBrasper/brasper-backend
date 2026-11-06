package com.brasper.platform.staff.interfaces.rest;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.application.commandservices.EmployeeCommandFailure;
import com.brasper.platform.staff.application.commandservices.EmployeeCommandService;
import com.brasper.platform.staff.application.queryservices.EmployeeQueryService;
import com.brasper.platform.staff.domain.model.aggregates.Employee;
import com.brasper.platform.staff.domain.model.commands.HireEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.TerminateEmployeeCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateEmployeeCommand;
import com.brasper.platform.staff.domain.model.valueobjects.Dni;
import com.brasper.platform.staff.interfaces.rest.resources.HireEmployeeResource;
import com.brasper.platform.staff.interfaces.rest.resources.TerminateEmployeeResource;
import com.brasper.platform.staff.interfaces.rest.resources.UpdateEmployeeResource;
import com.brasper.platform.staff.interfaces.rest.transform.EmployeeFailureResponseAssembler;
import com.brasper.platform.staff.interfaces.rest.transform.EmployeeResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Salaries are sensitive: admin, accounting and management read; only admin and accounting write. */
@RestController
@RequestMapping("/api/v1/employees")
public class EmployeesController {

    private static final String CAN_WRITE = "hasAnyRole('ADMIN','ACCOUNTING')";
    private static final String CAN_READ = "hasAnyRole('ADMIN','ACCOUNTING','MANAGEMENT')";

    private final EmployeeCommandService commands;
    private final EmployeeQueryService queries;
    private final MessageSource messages;

    public EmployeesController(EmployeeCommandService commands, EmployeeQueryService queries,
            MessageSource messages) {
        this.commands = commands;
        this.queries = queries;
        this.messages = messages;
    }

    @PostMapping
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> hire(@Valid @RequestBody HireEmployeeResource resource) {
        var command = new HireEmployeeCommand(resource.fullName(), new Dni(resource.dni()), resource.position(),
                new Money(resource.monthlySalary(), resource.currency()), resource.hiredOn());
        return respond(commands.handle(command), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeResource resource) {
        var command = new UpdateEmployeeCommand(id, resource.position(),
                new Money(resource.monthlySalary(), resource.currency()));
        return respond(commands.handle(command), HttpStatus.OK);
    }

    @PostMapping("/{id}/termination")
    @PreAuthorize(CAN_WRITE)
    public ResponseEntity<?> terminate(@PathVariable Long id, @Valid @RequestBody TerminateEmployeeResource resource) {
        return respond(commands.handle(new TerminateEmployeeCommand(id, resource.terminatedOn())), HttpStatus.OK);
    }

    @GetMapping
    @PreAuthorize(CAN_READ)
    public ResponseEntity<?> list(@RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(queries.findAll(active).stream().map(EmployeeResourceAssembler::toResource).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize(CAN_READ)
    public ResponseEntity<?> get(@PathVariable Long id) {
        return queries.findById(id)
                .<ResponseEntity<?>>map(employee -> ResponseEntity.ok(EmployeeResourceAssembler.toResource(employee)))
                .orElseGet(() -> EmployeeFailureResponseAssembler.toResponse(
                        EmployeeCommandFailure.EMPLOYEE_NOT_FOUND, messages));
    }

    private ResponseEntity<?> respond(Result<Employee, EmployeeCommandFailure> result, HttpStatus success) {
        return result.<ResponseEntity<?>>fold(
                employee -> ResponseEntity.status(success).body(EmployeeResourceAssembler.toResource(employee)),
                failure -> EmployeeFailureResponseAssembler.toResponse(failure, messages));
    }
}
