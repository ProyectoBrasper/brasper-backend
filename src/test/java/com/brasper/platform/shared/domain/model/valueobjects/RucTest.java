package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RucTest {

    @Test
    void acceptsElevenDigitsAndStripsSpaces() {
        assertThat(new Ruc(" 20123456789 ").value()).isEqualTo("20123456789");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2012345678", "201234567890", "2012345678a", "2012 3456789"})
    void rejectsAnythingElse(String value) {
        assertThatThrownBy(() -> new Ruc(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
