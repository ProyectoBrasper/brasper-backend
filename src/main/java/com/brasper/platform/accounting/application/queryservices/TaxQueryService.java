package com.brasper.platform.accounting.application.queryservices;
import com.brasper.platform.accounting.domain.model.aggregates.TaxParameters;
import com.brasper.platform.accounting.domain.repositories.TaxParametersRepository;
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
    public TaxQueryService(TaxParametersRepository parameters) { this.parameters = parameters; }
    public Optional<TaxParameters> findParametersAt(LocalDate date) {
        return parameters.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(date);
    }
    public List<TaxParameters> findAllParameters() {
        return parameters.findAll().stream().sorted(Comparator.comparing(TaxParameters::getEffectiveFrom).reversed()).toList();
    }
}
