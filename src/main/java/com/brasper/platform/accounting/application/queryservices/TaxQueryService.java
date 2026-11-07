package com.brasper.platform.accounting.application.queryservices;
import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
import com.brasper.platform.accounting.domain.repositories.TaxReturnRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaxQueryService {
    private final TaxParametersRepository parameters;
    private final TaxReturnRepository returns;
    public TaxQueryService(TaxParametersRepository parameters, TaxReturnRepository returns) {
        this.parameters = parameters;
        this.returns = returns;
    }
    public Optional<TaxReturn> findReturnByPeriod(Period period) { return returns.findByPeriod(period); }
    public List<TaxReturn> findAllReturns() {
        return returns.findAll().stream().sorted(Comparator.comparing(TaxReturn::getPeriod,
                Comparator.comparingInt(Period::year).thenComparingInt(Period::month)).reversed()).toList();
    }
    public Optional<TaxParameters> findParametersAt(LocalDate date) {
        return parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(date);
    }
    public List<TaxParameters> findAllParameters() {
        return parameters.findAll().stream().sorted(Comparator.comparing(TaxParameters::getEffectiveFrom).reversed()).toList();
    }
}
