package com.brasper.platform.billing.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InvoiceNumberTest {

    @Test
    void normalizesTheSeriesAndPrintsTheCorrelativeWithEightDigits() {
        InvoiceNumber number = new InvoiceNumber(" f001 ", 123);

        assertThat(number.series()).isEqualTo("F001");
        assertThat(number).hasToString("F001-00000123");
    }

    @Test
    void rejectsABadSeriesOrCorrelative() {
        assertThatThrownBy(() -> new InvoiceNumber("F01", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InvoiceNumber("F-01", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InvoiceNumber("F001", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new InvoiceNumber("F001", 100_000_000)).isInstanceOf(IllegalArgumentException.class);
    }
}
