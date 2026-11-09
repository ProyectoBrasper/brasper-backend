package com.brasper.platform.accounting.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.projects.domain.events.ProjectCompletedEvent;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.staff.domain.events.FixedCostPaidEvent;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.treasury.domain.events.LoanDisbursedEvent;
import com.brasper.platform.treasury.domain.events.LoanInstallmentPaidEvent;
import com.brasper.platform.treasury.domain.events.PartnerContributionRegisteredEvent;
import com.brasper.platform.treasury.domain.events.PayablePaidEvent;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PostingRulesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 20);

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static InvoiceIssuedEvent issued(boolean advance, String igv) {
        return new InvoiceIssuedEvent(5L, 3L, DAY, pen("1000"), pen(igv), pen("1000").add(pen(igv)), advance);
    }

    private static JournalLineData line(RegisterJournalEntryCommand command, String account) {
        return command.lines().stream().filter(l -> l.accountCode().value().equals(account)).findFirst().orElseThrow();
    }

    @Test
    void anIssuedInvoiceDebitsReceivablesAndCreditsSalesAndIgv() {
        RegisterJournalEntryCommand command = PostingRules.forInvoiceIssued(issued(false, "180"));

        assertThat(command.source()).isEqualTo(EntrySource.INVOICE_ISSUED);
        assertThat(command.sourceId()).isEqualTo(5L);
        assertThat(line(command, "12").debit().amount()).isEqualByComparingTo("1180");
        assertThat(line(command, "70").credit().amount()).isEqualByComparingTo("1000");
        assertThat(line(command, "70").projectId()).isEqualTo(3L);
        assertThat(line(command, "40111").credit().amount()).isEqualByComparingTo("180");
    }

    @Test
    void anAssetTakesOverTheCostOfItsExpenseAndKeepsTheProject() {
        RegisterJournalEntryCommand command = PostingRules.forFixedAssetAcquisition(8L, DAY, pen("1200"),
                new com.brasper.platform.accounting.domain.model.valueobjects.AccountCode("60"), 3L);

        assertThat(command.source()).isEqualTo(EntrySource.FIXED_ASSET_ACQUISITION);
        assertThat(command.sourceId()).isEqualTo(8L);
        assertThat(line(command, "33").debit().amount()).isEqualByComparingTo("1200");
        assertThat(line(command, "60").credit().amount()).isEqualByComparingTo("1200");
        assertThat(line(command, "60").projectId()).isEqualTo(3L);
    }

    @Test
    void disposingAnAssetClearsItsCostAndWritesOffWhatWasNotDepreciated() {
        RegisterJournalEntryCommand command = PostingRules.forFixedAssetDisposal(8L, DAY, pen("1200"), pen("800"));

        assertThat(command.source()).isEqualTo(EntrySource.FIXED_ASSET_DISPOSAL);
        assertThat(line(command, "39").debit().amount()).isEqualByComparingTo("800");
        assertThat(line(command, "65").debit().amount()).isEqualByComparingTo("400");
        assertThat(line(command, "33").credit().amount()).isEqualByComparingTo("1200");
        assertThat(PostingRules.forFixedAssetDisposal(8L, DAY, pen("1200"), pen("1200")).lines())
                .extracting(l -> l.accountCode().value()).containsExactly("39", "33");
    }

    @Test
    void aStandalonePayableBooksTheCostAndTheDebtButOneFromAnExpenseOrATaxDoesNot() {
        var payable = new com.brasper.platform.treasury.domain.events.PayableRegisteredEvent(4L, null,
                com.brasper.platform.treasury.domain.model.valueobjects.PayableType.SUPPLIER, 3L, pen("700"), DAY);

        RegisterJournalEntryCommand command = PostingRules.forPayableRegistered(payable).orElseThrow();

        assertThat(command.source()).isEqualTo(EntrySource.PAYABLE_REGISTERED);
        assertThat(line(command, "60").debit().amount()).isEqualByComparingTo("700");
        assertThat(line(command, "60").projectId()).isEqualTo(3L);
        assertThat(line(command, "42").credit().amount()).isEqualByComparingTo("700");
        assertThat(PostingRules.forPayableRegistered(new com.brasper.platform.treasury.domain.events.PayableRegisteredEvent(
                4L, 9L, payable.type(), null, pen("700"), DAY))).isEmpty();
        assertThat(PostingRules.forPayableRegistered(new com.brasper.platform.treasury.domain.events.PayableRegisteredEvent(
                4L, null, com.brasper.platform.treasury.domain.model.valueobjects.PayableType.TAX, null, pen("700"), DAY)))
                .isEmpty();
    }

    @Test
    void anAdvanceGoesToCustomerAdvancesAndAnInvoiceWithoutIgvHasNoIgvLine() {
        RegisterJournalEntryCommand command = PostingRules.forInvoiceIssued(issued(true, "0"));

        assertThat(command.lines()).extracting(l -> l.accountCode().value()).containsExactly("12", "122");
    }

    @Test
    void aDepositDebitsCashAndAClearsReceivablesButARetentionCertificateIsLeftOut() {
        var deposit = new InvoicePaymentReceivedEvent(8L, 5L, DAY, pen("500"), InvoicePaymentType.NET_DEPOSIT, 2L);
        var certificate = new InvoicePaymentReceivedEvent(9L, 5L, DAY, pen("35"),
                InvoicePaymentType.RETENTION_CERTIFICATE, null);

        RegisterJournalEntryCommand command = PostingRules.forInvoicePayment(deposit).orElseThrow();

        assertThat(command.source()).isEqualTo(EntrySource.INVOICE_PAYMENT);
        assertThat(command.sourceId()).isEqualTo(8L);
        assertThat(line(command, "10").debit().amount()).isEqualByComparingTo("500");
        assertThat(line(command, "12").credit().amount()).isEqualByComparingTo("500");
        assertThat(PostingRules.forInvoicePayment(certificate)).isEmpty();
    }

    private static ExpenseRegisteredEvent expense(Long projectId, boolean paid, PaymentMethod method) {
        boolean partner = method == PaymentMethod.PARTNER_CONTRIBUTION;
        return new ExpenseRegisteredEvent(4L, DAY, projectId, pen("100"), pen("18"), pen("118"), paid, method,
                partner ? null : 2L, partner ? 6L : null);
    }

    @Test
    void anExpenseDebitsCostAndIgvAndCreditsCashPayablesOrCapital() {
        RegisterJournalEntryCommand paid = PostingRules.forExpense(expense(3L, true, PaymentMethod.BANK_ACCOUNT));
        RegisterJournalEntryCommand pending = PostingRules.forExpense(expense(null, false, PaymentMethod.BANK_ACCOUNT));
        RegisterJournalEntryCommand partner = PostingRules.forExpense(expense(3L, true, PaymentMethod.PARTNER_CONTRIBUTION));

        assertThat(paid.source()).isEqualTo(EntrySource.EXPENSE);
        assertThat(line(paid, "60").debit().amount()).isEqualByComparingTo("100");
        assertThat(line(paid, "60").projectId()).isEqualTo(3L);
        assertThat(line(paid, "40111").debit().amount()).isEqualByComparingTo("18");
        assertThat(line(paid, "10").credit().amount()).isEqualByComparingTo("118");
        assertThat(line(pending, "63").debit().amount()).isEqualByComparingTo("100");
        assertThat(line(pending, "42").credit().amount()).isEqualByComparingTo("118");
        assertThat(line(partner, "50").credit().amount()).isEqualByComparingTo("118");
    }

    @Test
    void aPayablePaymentDebitsSuppliersAndCreditsCash() {
        RegisterJournalEntryCommand command = PostingRules.forPayablePayment(new PayablePaidEvent(3L, 2L, DAY, pen("90"), 1L));

        assertThat(command.source()).isEqualTo(EntrySource.PAYABLE_PAYMENT);
        assertThat(line(command, "42").debit().amount()).isEqualByComparingTo("90");
        assertThat(line(command, "10").credit().amount()).isEqualByComparingTo("90");
    }

    @Test
    void loansPostOnTheLoanAccountOfTheirDirection() {
        RegisterJournalEntryCommand received = PostingRules.forLoanDisbursement(
                new LoanDisbursedEvent(1L, LoanType.RECEIVED, DAY, pen("5000"), 1L));
        RegisterJournalEntryCommand granted = PostingRules.forLoanDisbursement(
                new LoanDisbursedEvent(2L, LoanType.GRANTED, DAY, pen("5000"), 1L));

        assertThat(line(received, "10").debit().amount()).isEqualByComparingTo("5000");
        assertThat(line(received, "45").credit().amount()).isEqualByComparingTo("5000");
        assertThat(line(granted, "16").debit().amount()).isEqualByComparingTo("5000");
        assertThat(line(granted, "10").credit().amount()).isEqualByComparingTo("5000");
    }

    @Test
    void installmentsSplitPrincipalAndInterest() {
        RegisterJournalEntryCommand paid = PostingRules.forLoanInstallment(
                new LoanInstallmentPaidEvent(7L, 1L, LoanType.RECEIVED, DAY, pen("400"), pen("50"), 1L));
        RegisterJournalEntryCommand collected = PostingRules.forLoanInstallment(
                new LoanInstallmentPaidEvent(8L, 2L, LoanType.GRANTED, DAY, pen("400"), pen("50"), 1L));
        RegisterJournalEntryCommand noInterest = PostingRules.forLoanInstallment(
                new LoanInstallmentPaidEvent(9L, 1L, LoanType.RECEIVED, DAY, pen("400"), pen("0"), 1L));

        assertThat(line(paid, "45").debit().amount()).isEqualByComparingTo("400");
        assertThat(line(paid, "67").debit().amount()).isEqualByComparingTo("50");
        assertThat(line(paid, "10").credit().amount()).isEqualByComparingTo("450");
        assertThat(line(collected, "10").debit().amount()).isEqualByComparingTo("450");
        assertThat(line(collected, "16").credit().amount()).isEqualByComparingTo("400");
        assertThat(line(collected, "77").credit().amount()).isEqualByComparingTo("50");
        assertThat(noInterest.lines()).extracting(l -> l.accountCode().value()).containsExactly("45", "10");
    }

    @Test
    void aPartnerContributionIsPostedOnlyWhenMoneyReachedAnAccountAndNoExpenseCarriesIt() {
        var deposit = new PartnerContributionRegisteredEvent(5L, 6L, DAY, pen("300"), 1L, null, false);
        var forExpense = new PartnerContributionRegisteredEvent(6L, 6L, DAY, pen("80"), 1L, 4L, false);
        var noAccount = new PartnerContributionRegisteredEvent(7L, 6L, DAY, pen("80"), null, null, false);

        RegisterJournalEntryCommand command = PostingRules.forPartnerContribution(deposit).orElseThrow();

        assertThat(line(command, "10").debit().amount()).isEqualByComparingTo("300");
        assertThat(line(command, "50").credit().amount()).isEqualByComparingTo("300");
        RegisterJournalEntryCommand loan = PostingRules.forPartnerContribution(
                new PartnerContributionRegisteredEvent(5L, 6L, DAY, pen("300"), 1L, null, true)).orElseThrow();
        assertThat(loan.lines()).extracting(l -> l.accountCode().value()).containsExactly("10", "455");
        assertThat(PostingRules.forPartnerContribution(forExpense)).isEmpty();
        assertThat(PostingRules.forPartnerContribution(noAccount)).isEmpty();
    }

    @Test
    void aFixedCostPaymentDebitsTheAccountOfItsTypeAndCreditsCash() {
        var salary = PostingRules.forFixedCostPayment(new FixedCostPaidEvent(1L, 2L, FixedCostType.SALARY, DAY, pen("900"), 1L));
        var fees = PostingRules.forFixedCostPayment(new FixedCostPaidEvent(2L, 3L, FixedCostType.ITF, DAY, pen("5"), null));
        var rent = PostingRules.forFixedCostPayment(new FixedCostPaidEvent(3L, 4L, FixedCostType.OFFICE_RENT, DAY, pen("700"), 1L));

        assertThat(salary.source()).isEqualTo(EntrySource.FIXED_COST_PAYMENT);
        assertThat(line(salary, "62").debit().amount()).isEqualByComparingTo("900");
        assertThat(line(salary, "10").credit().amount()).isEqualByComparingTo("900");
        assertThat(line(fees, "67").debit().amount()).isEqualByComparingTo("5");
        assertThat(line(rent, "63").debit().amount()).isEqualByComparingTo("700");
    }

    @Test
    void completingAProjectTurnsItsAdvancesIntoSales() {
        var event = new ProjectCompletedEvent(3L, DAY);

        RegisterJournalEntryCommand command = PostingRules.forProjectCompleted(event, new BigDecimal("1000")).orElseThrow();

        assertThat(command.source()).isEqualTo(EntrySource.PROJECT_COMPLETED);
        assertThat(command.sourceId()).isEqualTo(3L);
        assertThat(line(command, "122").debit().amount()).isEqualByComparingTo("1000");
        assertThat(line(command, "70").credit().amount()).isEqualByComparingTo("1000");
        assertThat(line(command, "70").projectId()).isEqualTo(3L);
        assertThat(PostingRules.forProjectCompleted(event, BigDecimal.ZERO)).isEmpty();
    }
}
