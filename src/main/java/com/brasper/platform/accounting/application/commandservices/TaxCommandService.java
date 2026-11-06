package com.brasper.platform.accounting.application.commandservices;

import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.model.commands.SetTaxParametersCommand;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxCommandService {

    private final TaxParametersRepository parameters;

    public TaxCommandService(TaxParametersRepository parameters) {
        this.parameters = parameters;
    }

    /** Each change is a new row; a date can only be set once because the old row may already have been used. */
    @Transactional
    public Result<TaxParameters, TaxCommandFailure> handle(SetTaxParametersCommand command) {
        boolean taken = parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(command.effectiveFrom())
                .filter(existing -> existing.getEffectiveFrom().equals(command.effectiveFrom())).isPresent();
        if (taken) {
            return Result.failure(TaxCommandFailure.DUPLICATE_EFFECTIVE_DATE);
        }
        return Result.success(parameters.save(new TaxParameters(command)));
    }
}
