package com.brasper.platform.treasury.interfaces.rest.resources;

import java.math.BigDecimal;

public record BankAccountResource(Long id, String bank, String accountNumber, String type, String currency,
                                  BigDecimal openingBalance, BigDecimal currentBalance) {
}
