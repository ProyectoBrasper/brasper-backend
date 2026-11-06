package com.brasper.platform.accounting.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.time.LocalDate;

public record SetTaxParametersCommand(TaxRates rates, LocalDate effectiveFrom) { }
