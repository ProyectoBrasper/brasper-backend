package com.brasper.platform.staff.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DniTest {
    @Test
    void acceptsExactlyEightDigitsIncludingLeadingZeros() {
        assertThat(new Dni("01234567").value()).isEqualTo("01234567");
        assertThat(new Dni("01234567")).isEqualTo(new Dni("01234567"));
    }
    @Test
    void rejectsInvalidValues() {
        for (String value : new String[]{null, "", "1234567", "123456789", "1234567a", " 12345678", "12345678 "})
            assertThatThrownBy(() -> new Dni(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
