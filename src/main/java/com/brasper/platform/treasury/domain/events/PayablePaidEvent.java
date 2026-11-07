package com.brasper.platform.treasury.domain.events;

import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.time.LocalDate;

/** Cash already left the account: Treasury moved it when it registered the payment. */
public record PayablePaidEvent(Long paymentId, Long payableId, LocalDate paidOn, Money amount, Long bankAccountId) { }
