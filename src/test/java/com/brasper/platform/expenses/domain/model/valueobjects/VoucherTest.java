package com.brasper.platform.expenses.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VoucherTest {

    @Test
    void onlyAnInvoiceAllowsTaxCredit() {
        assertThat(new Voucher(VoucherType.INVOICE, "F001-123").allowsTaxCredit()).isTrue();
        assertThat(new Voucher(VoucherType.SALES_RECEIPT, "B001-9").allowsTaxCredit()).isFalse();
        assertThat(new Voucher(VoucherType.FEE_RECEIPT, "E001-1").allowsTaxCredit()).isFalse();
        assertThat(new Voucher(VoucherType.NO_VOUCHER, null).allowsTaxCredit()).isFalse();
    }

    @Test
    void trimsTheNumberAndTreatsBlankAsAbsent() {
        assertThat(new Voucher(VoucherType.INVOICE, "  F001-123 ").number()).isEqualTo("F001-123");
        assertThat(new Voucher(VoucherType.NO_VOUCHER, "  ").number()).isNull();
    }

    @Test
    void aVoucherNeedsANumberAndNoVoucherCannotHaveOne() {
        assertThatThrownBy(() -> new Voucher(VoucherType.INVOICE, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Voucher(VoucherType.NO_VOUCHER, "X-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Voucher(VoucherType.INVOICE, "9".repeat(31)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Voucher(null, "F001-1")).isInstanceOf(NullPointerException.class);
    }
}
