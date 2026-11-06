package com.brasper.platform.staff.application.commandservices;

import com.brasper.platform.shared.application.result.Result;
import com.brasper.platform.staff.application.outboundservices.StaffTreasuryLookup;
import com.brasper.platform.staff.domain.model.aggregates.FixedCost;
import com.brasper.platform.staff.domain.model.commands.CreateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.DeactivateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.RegisterFixedCostPaymentCommand;
import com.brasper.platform.staff.domain.model.commands.UpdateFixedCostCommand;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.staff.domain.repositories.EmployeeRepository;
import com.brasper.platform.staff.domain.repositories.FixedCostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixedCostCommandService {

    private final FixedCostRepository costs;
    private final EmployeeRepository employees;
    private final StaffTreasuryLookup treasury;

    public FixedCostCommandService(FixedCostRepository costs, EmployeeRepository employees,
            StaffTreasuryLookup treasury) {
        this.costs = costs;
        this.employees = employees;
        this.treasury = treasury;
    }

    @Transactional
    public Result<FixedCost, FixedCostCommandFailure> handle(CreateFixedCostCommand command) {
        if (costs.existsByCode(command.code())) {
            return Result.failure(FixedCostCommandFailure.DUPLICATE_CODE);
        }
        if (command.type() == FixedCostType.SALARY && command.employeeId() == null) {
            return Result.failure(FixedCostCommandFailure.EMPLOYEE_REQUIRED);
        }
        if (command.employeeId() != null && employees.findById(command.employeeId()).isEmpty()) {
            return Result.failure(FixedCostCommandFailure.EMPLOYEE_NOT_FOUND);
        }
        return Result.success(costs.save(new FixedCost(command)));
    }

    @Transactional
    public Result<FixedCost, FixedCostCommandFailure> handle(UpdateFixedCostCommand command) {
        FixedCost cost = costs.findById(command.fixedCostId()).orElse(null);
        if (cost == null) {
            return Result.failure(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        }
        if (!cost.isActive()) {
            return Result.failure(FixedCostCommandFailure.FIXED_COST_INACTIVE);
        }
        cost.update(command);
        return Result.success(costs.save(cost));
    }

    @Transactional
    public Result<FixedCost, FixedCostCommandFailure> handle(DeactivateFixedCostCommand command) {
        FixedCost cost = costs.findById(command.fixedCostId()).orElse(null);
        if (cost == null) {
            return Result.failure(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        }
        cost.deactivate();
        return Result.success(costs.save(cost));
    }

    @Transactional
    public Result<FixedCost, FixedCostCommandFailure> handle(RegisterFixedCostPaymentCommand command) {
        FixedCost cost = costs.findById(command.fixedCostId()).orElse(null);
        if (cost == null) {
            return Result.failure(FixedCostCommandFailure.FIXED_COST_NOT_FOUND);
        }
        if (!cost.isActive()) {
            return Result.failure(FixedCostCommandFailure.FIXED_COST_INACTIVE);
        }
        if (command.bankAccountId() != null && !treasury.bankAccountExists(command.bankAccountId())) {
            return Result.failure(FixedCostCommandFailure.BANK_ACCOUNT_NOT_FOUND);
        }
        cost.registerPayment(command);
        return Result.success(costs.save(cost));
    }
}
