package com.brasper.platform.iam.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsernameTest {

    @Test
    void isStrippedAndLowercasedSoItIsUniqueIgnoringCase() {
        assertThat(new Username("  Jhermozaq.Admin ")).isEqualTo(new Username("jhermozaq.admin"));
        assertThat(new Username("Ana_01").value()).isEqualTo("ana_01");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "has space", "ñandú", "semi;colon"})
    void rejectsInvalidNames(String value) {
        assertThatThrownBy(() -> new Username(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesLengthBounds() {
        assertThat(new Username("a".repeat(50)).value()).hasSize(50);
        assertThatThrownBy(() -> new Username("a".repeat(51))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Username(null)).isInstanceOf(NullPointerException.class);
    }
}
