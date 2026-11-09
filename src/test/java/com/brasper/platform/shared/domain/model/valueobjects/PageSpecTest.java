package com.brasper.platform.shared.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageSpecTest {

    @Test
    void noPageAndNoSizeMeansTheWholeList() {
        assertThat(PageSpec.of(null, null)).isNull();
        assertThat(PageSpec.of(2, 50)).isEqualTo(new PageSpec(2, 50));
    }

    @Test
    void rejectsOneWithoutTheOtherAndOutOfRangeValues() {
        assertThatThrownBy(() -> PageSpec.of(1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PageSpec.of(null, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageSpec(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageSpec(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageSpec(0, PageSpec.MAX_SIZE + 1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new PageSpec(0, PageSpec.MAX_SIZE).size()).isEqualTo(200);
    }
}
