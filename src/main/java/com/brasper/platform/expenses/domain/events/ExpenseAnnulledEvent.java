package com.brasper.platform.expenses.domain.events;

import java.time.LocalDate;

public record ExpenseAnnulledEvent(Long expenseId, LocalDate annulledOn) { }
