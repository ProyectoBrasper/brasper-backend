package com.brasper.platform.billing.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceTest {

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static Invoice invoice(String base, boolean retention, boolean detraction, boolean self) {
        return new Invoice(new IssueInvoiceCommand(new InvoiceNumber("F001", 7), LocalDate.of(2026, 8, 20), "  ",
                3L, 4L, pen(base), retention, detraction, self, false, null), TaxRates.standard());
    }

    @Test
    void issuesAPendingInvoiceWithIgvAndNoDeductions() {
        Invoice invoice = invoice("1000", false, false, false);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getNumber()).hasToString("F001-00000007");
        assertThat(invoice.getAmounts().igv()).isEqualTo(pen("180"));
        assertThat(invoice.getTotal()).isEqualTo(pen("1180"));
        assertThat(invoice.getRetention()).isEqualTo(pen("0"));
        assertThat(invoice.getDetraction()).isEqualTo(pen("0"));
        assertThat(invoice.netAmount()).isEqualTo(pen("1180"));
        assertThat(invoice.getPurchaseOrder()).isNull();
    }

    @Test
    void retentionIsThreePercentOfTheTotal() {
        Invoice invoice = invoice("1000", true, false, false);

        assertThat(invoice.getRetention()).isEqualTo(pen("35.40"));
        assertThat(invoice.netAmount()).isEqualTo(pen("1144.60"));
    }

    @Test
    void detractionIsTwelvePercentOfTheTotalRoundedToWholeUnits() {
        Invoice invoice = invoice("1000", false, true, true);

        assertThat(invoice.getDetraction()).isEqualTo(pen("142"));
        assertThat(invoice.netAmount()).isEqualTo(pen("1038"));
        assertThat(invoice.isSelfDetraction()).isTrue();
        // 1180.50 x 12% = 141.66 -> 142
        assertThat(invoice("1000.42", false, true, false).getDetraction()).isEqualTo(pen("142"));
    }

    private static RegisterInvoicePaymentCommand payment(Money amount) {
        return new RegisterInvoicePaymentCommand(1L, LocalDate.of(2026, 9, 1), amount,
                InvoicePaymentType.NET_DEPOSIT, 9L, " OP-123 ");
    }

    @Test
    void paymentsMoveTheStatusFromPendingToPartiallyToCollected() {
        Invoice invoice = invoice("1000", false, false, false);

        var first = invoice.registerPayment(payment(pen("400")));
        assertThat(first.getReference()).isEqualTo("OP-123");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_COLLECTED);
        assertThat(invoice.collectedAmount()).isEqualTo(pen("400"));
        assertThat(invoice.outstandingBalance()).isEqualTo(pen("780"));

        invoice.registerPayment(payment(pen("780")));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.COLLECTED);
        assertThat(invoice.outstandingBalance()).isEqualTo(pen("0"));
        assertThat(invoice.getPayments()).hasSize(2);
    }

    @Test
    void rejectsPaymentsAboveTheBalanceInAnotherCurrencyOrNotPositive() {
        Invoice invoice = invoice("1000", false, false, false);

        assertThatThrownBy(() -> invoice.registerPayment(payment(pen("1180.01"))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> invoice.registerPayment(payment(new Money(BigDecimal.TEN, Currency.USD))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> invoice.registerPayment(payment(pen("0"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(invoice.getPayments()).isEmpty();
    }

    @Test
    void voidsOnlyAnInvoiceWithoutPayments() {
        Invoice unpaid = invoice("1000", false, false, false);
        unpaid.voidInvoice();
        assertThat(unpaid.getStatus()).isEqualTo(InvoiceStatus.VOIDED);
        assertThatThrownBy(unpaid::voidInvoice).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> unpaid.registerPayment(payment(pen("1")))).isInstanceOf(IllegalStateException.class);

        Invoice paid = invoice("1000", false, false, false);
        paid.registerPayment(payment(pen("1")));
        assertThatThrownBy(paid::voidInvoice).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsInvalidData() {
        assertThatThrownBy(() -> invoice("0", false, false, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> invoice("100", false, false, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
