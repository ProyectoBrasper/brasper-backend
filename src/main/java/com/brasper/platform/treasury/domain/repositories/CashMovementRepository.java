package com.brasper.platform.treasury.domain.repositories;

import com.brasper.platform.treasury.domain.model.entities.CashMovement;
import java.time.LocalDate;
import java.util.List;

public interface CashMovementRepository {

    List<CashMovement> findAllByBankAccountIdAndOccurredOnBetween(Long bankAccountId, LocalDate start, LocalDate end);
}
