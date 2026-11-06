package com.brasper.platform.treasury.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanResource(Long id, String type, String counterparty, BigDecimal principal, String currency,
                           BigDecimal interestRate, LocalDate disbursedOn, Long bankAccountId,
                           BigDecimal totalInterest, BigDecimal outstandingBalance, String status,
                           List<InstallmentResource> installments) {

    public record InstallmentResource(Long id, LocalDate dueOn, BigDecimal principal, BigDecimal interest,
                                      boolean paid, LocalDate paidOn) {
    }
}
