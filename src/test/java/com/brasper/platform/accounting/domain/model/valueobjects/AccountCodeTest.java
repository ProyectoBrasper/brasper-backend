package com.brasper.platform.accounting.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountCodeTest {
    @Test
    void acceptsTwoToTenDigitsAndExtractsElement() {
        for (int element = 1; element <= 9; element++) {
            for (int length = 2; length <= 10; length++) {
                String value = element + "0".repeat(length - 1);
                assertThat(new AccountCode(value).value()).isEqualTo(value);
                assertThat(new AccountCode(value).element()).isEqualTo(element);
                assertThat(new AccountCode(value)).isEqualTo(new AccountCode(value));
            }
        }
    }
    @Test
    void rejectsInvalidCodes() {
        for (String value : new String[]{null, "", "1", "12345678901", "01", "00", "1a", "-10", " 10", "10 ", "10\n", "١٠"})
            assertThatThrownBy(() -> new AccountCode(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
