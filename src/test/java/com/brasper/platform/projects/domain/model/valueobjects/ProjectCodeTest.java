package com.brasper.platform.projects.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectCodeTest {

    @Test
    void parsesSequenceMonthAndYear() {
        ProjectCode code = new ProjectCode("862BR-AGO-2026");

        assertThat(code.sequence()).isEqualTo(862);
        assertThat(code.month()).isEqualTo(8);
        assertThat(code.year()).isEqualTo(2026);
    }

    @Test
    void normalizesCaseAndSpaces() {
        assertThat(new ProjectCode(" 001br-ene-2027 ").value()).isEqualTo("001BR-ENE-2027");
        assertThat(new ProjectCode("001BR-ENE-2027").month()).isEqualTo(1);
        assertThat(new ProjectCode("001BR-DIC-2027").month()).isEqualTo(12);
        assertThat(new ProjectCode("001BR-ABR-2027").month()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"8789R-OCT-2026", "853BRJUN-2026", "862BR-AUG-2026", "62BR-AGO-2026",
            "862BR-AGO-26", "", "862BR-AGO-2026-X"})
    void rejectsTyposFromTheExcel(String value) {
        assertThatThrownBy(() -> new ProjectCode(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
