package com.brasper.platform.billing.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.billing.application.outboundservices.ProjectLookup.BillableProject;
import com.brasper.platform.billing.application.outboundservices.TaxRatesProvider;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.events.InvoiceVoidedEvent;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.commands.VoidInvoiceCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceStatus;
import com.brasper.platform.billing.domain.repositories.InvoiceRepository;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.TaxRates;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class InvoiceCommandServiceTest {

    private static final InvoiceNumber NUMBER = new InvoiceNumber("F001", 7);

    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final ProjectLookup projects = mock(ProjectLookup.class);
    private final TaxRatesProvider taxRates = mock(TaxRatesProvider.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final InvoiceCommandService service = new InvoiceCommandService(invoices, projects, taxRates, events);

    private static IssueInvoiceCommand issue() {
        return new IssueInvoiceCommand(NUMBER, LocalDate.of(2026, 8, 20), null, 3L,
                new Money(new BigDecimal("1000"), Currency.PEN), true, false, false, false, null);
    }

    private void projectWithStatus(String status) {
        when(projects.find(3L)).thenReturn(Optional.of(new BillableProject(3L, 4L, status)));
        when(taxRates.ratesAt(any())).thenReturn(TaxRates.standard());
        when(invoices.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
    }

    private Invoice storedInvoice() {
        Invoice invoice = new Invoice(issue(), 4L, TaxRates.standard());
        when(invoices.findById(5L)).thenReturn(Optional.of(invoice));
        when(invoices.save(invoice)).thenReturn(invoice);
        return invoice;
    }

    @Test
    void issuesAnInvoiceForTheProjectsClientWithTheRatesOfTheIssueDate() {
        projectWithStatus("IN_PROGRESS");

        Invoice invoice = service.handle(issue()).success().orElseThrow();

        assertThat(invoice.getClientId()).isEqualTo(4L);
        assertThat(invoice.getTotal().amount()).isEqualByComparingTo("1180");
        assertThat(invoice.getRetention().amount()).isEqualByComparingTo("35.40");
        verify(taxRates).ratesAt(LocalDate.of(2026, 8, 20));
    }

    @Test
    void completedProjectsCanBeInvoicedButQuotedAndCancelledOnesCannot() {
        projectWithStatus("COMPLETED");
        assertThat(service.handle(issue()).isSuccess()).isTrue();

        for (String status : new String[] {"QUOTED", "CANCELLED"}) {
            projectWithStatus(status);
            assertThat(service.handle(issue()).failure()).contains(InvoiceCommandFailure.PROJECT_NOT_BILLABLE);
        }
    }

    @Test
    void issuingFailsForAnUnknownProjectOrADuplicateNumberAndStoresNothing() {
        when(projects.find(3L)).thenReturn(Optional.empty());
        assertThat(service.handle(issue()).failure()).contains(InvoiceCommandFailure.PROJECT_NOT_FOUND);

        projectWithStatus("IN_PROGRESS");
        when(invoices.existsByNumber(NUMBER)).thenReturn(true);
        assertThat(service.handle(issue()).failure()).contains(InvoiceCommandFailure.DUPLICATE_NUMBER);
        verify(invoices, never()).save(any());
    }

    @Test
    void rejectsAnInvoiceInUsdBeforeLookingAnythingUp() {
        var usd = new IssueInvoiceCommand(NUMBER, LocalDate.of(2026, 8, 20), null, 3L,
                new Money(new BigDecimal("1000"), Currency.USD), true, false, false, false, null);

        assertThat(service.handle(usd).failure()).contains(InvoiceCommandFailure.UNSUPPORTED_CURRENCY);
        verify(projects, never()).find(any());
        verify(invoices, never()).save(any());
    }

    @Test
    void voidsAnInvoiceOnlyOnceAndOnlyWithoutPayments() {
        Invoice invoice = storedInvoice();

        assertThat(service.handle(new VoidInvoiceCommand(5L)).isSuccess()).isTrue();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VOIDED);
        assertThat(service.handle(new VoidInvoiceCommand(5L)).failure()).contains(InvoiceCommandFailure.ALREADY_VOIDED);

        Invoice paid = new Invoice(issue(), 4L, TaxRates.standard());
        paid.registerPayment(new RegisterInvoicePaymentCommand(null, LocalDate.of(2026, 9, 1),
                new Money(BigDecimal.TEN, Currency.PEN), InvoicePaymentType.NET_DEPOSIT, 1L, null));
        when(invoices.findById(6L)).thenReturn(Optional.of(paid));
        assertThat(service.handle(new VoidInvoiceCommand(6L)).failure()).contains(InvoiceCommandFailure.HAS_PAYMENTS);
        assertThat(paid.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_COLLECTED);
    }

    private static RegisterInvoicePaymentCommand payment(String amount, Currency currency) {
        return new RegisterInvoicePaymentCommand(5L, LocalDate.of(2026, 9, 1),
                new Money(new BigDecimal(amount), currency), InvoicePaymentType.NET_DEPOSIT, 1L, "OP-1");
    }

    @Test
    void registersPaymentsUntilTheBalanceIsCleared() {
        Invoice invoice = storedInvoice();

        var first = service.handle(payment("1000", Currency.PEN)).success().orElseThrow();
        assertThat(first.getAmount().amount()).isEqualByComparingTo("1000");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PARTIALLY_COLLECTED);

        service.handle(payment("180", Currency.PEN));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.COLLECTED);
    }

    @Test
    void paymentFailuresSayWhichRuleBrokeAndStoreNothing() {
        Invoice invoice = storedInvoice();

        assertThat(service.handle(payment("1180.01", Currency.PEN)).failure())
                .contains(InvoiceCommandFailure.AMOUNT_EXCEEDS_BALANCE);
        assertThat(service.handle(payment("10", Currency.USD)).failure())
                .contains(InvoiceCommandFailure.PAYMENT_CURRENCY_MISMATCH);
        assertThat(invoice.getPayments()).isEmpty();

        invoice.voidInvoice();
        assertThat(service.handle(payment("10", Currency.PEN)).failure())
                .contains(InvoiceCommandFailure.ALREADY_VOIDED);

        when(invoices.findById(9L)).thenReturn(Optional.empty());
        assertThat(service.handle(new RegisterInvoicePaymentCommand(9L, LocalDate.of(2026, 9, 1),
                new Money(BigDecimal.TEN, Currency.PEN), InvoicePaymentType.NET_DEPOSIT, 1L, null)).failure())
                .contains(InvoiceCommandFailure.INVOICE_NOT_FOUND);
    }

    @Test
    void voidingAnUnknownInvoiceFails() {
        when(invoices.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.handle(new VoidInvoiceCommand(9L)).failure()).contains(InvoiceCommandFailure.INVOICE_NOT_FOUND);
    }

    @Test
    void publishesAnEventForEachIssuedPaidAndVoidedInvoice() {
        projectWithStatus("IN_PROGRESS");
        service.handle(issue());
        verify(events).publishEvent(any(InvoiceIssuedEvent.class));

        storedInvoice();
        service.handle(payment("100", Currency.PEN));
        verify(events).publishEvent(any(InvoicePaymentReceivedEvent.class));

        Invoice unpaid = new Invoice(issue(), 4L, TaxRates.standard());
        when(invoices.findById(7L)).thenReturn(Optional.of(unpaid));
        when(invoices.save(unpaid)).thenReturn(unpaid);
        service.handle(new VoidInvoiceCommand(7L));
        verify(events).publishEvent(any(InvoiceVoidedEvent.class));
    }
}
