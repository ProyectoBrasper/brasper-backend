package com.brasper.platform.billing.interfaces.rest.resources;

import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code taxableBase} is the amount before IGV. The client is the project's, so it is not sent. IGV, retention and
 * detraction are computed. {@code selfDetraction} needs {@code hasDetraction}.
 */
public record IssueInvoiceResource(
        @NotBlank @Size(max = 4) String series,
        @Min(1) @Max(99_999_999) int correlative,
        @NotNull LocalDate issuedOn,
        @Size(max = 50) String purchaseOrder,
        @NotNull Long projectId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal taxableBase,
        @NotNull Currency currency,
        Boolean hasRetention,
        Boolean hasDetraction,
        Boolean selfDetraction,
        Boolean advance,
        @Size(max = 1000) String notes) {

    /** A flag that is not sent counts as false (Jackson rejects a missing primitive). */
    public IssueInvoiceResource {
        hasRetention = Boolean.TRUE.equals(hasRetention);
        hasDetraction = Boolean.TRUE.equals(hasDetraction);
        selfDetraction = Boolean.TRUE.equals(selfDetraction);
        advance = Boolean.TRUE.equals(advance);
    }
}
