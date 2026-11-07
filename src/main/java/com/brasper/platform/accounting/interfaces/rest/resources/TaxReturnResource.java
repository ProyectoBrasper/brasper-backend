package com.brasper.platform.accounting.interfaces.rest.resources;
import java.time.LocalDate;

public record TaxReturnResource(Long id, int year, int month, MoneyResource outputIgv, MoneyResource inputIgv,
        MoneyResource carriedCredit, MoneyResource igvPayable, MoneyResource incomeTaxPrepayment,
        MoneyResource totalPayable, String status, LocalDate paidOn, Long bankAccountId) { }
