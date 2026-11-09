package com.brasper.platform.accounting.domain.services;

import com.brasper.platform.accounting.domain.model.commands.JournalLineData;
import com.brasper.platform.accounting.domain.model.commands.RegisterJournalEntryCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.AccountCode;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.billing.domain.events.InvoiceIssuedEvent;
import com.brasper.platform.billing.domain.events.InvoicePaymentReceivedEvent;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.projects.domain.events.ProjectCompletedEvent;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.staff.domain.events.FixedCostPaidEvent;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.treasury.domain.events.LoanDisbursedEvent;
import com.brasper.platform.treasury.domain.events.LoanInstallmentPaidEvent;
import com.brasper.platform.treasury.domain.events.PartnerContributionRegisteredEvent;
import com.brasper.platform.treasury.domain.events.PayablePaidEvent;
import com.brasper.platform.treasury.domain.events.PayableRegisteredEvent;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The journal entry each business event produces, in PCGE accounts. Pure functions, nothing is persisted here. */
public final class PostingRules {

    static final AccountCode CASH = new AccountCode("10");
    static final AccountCode RECEIVABLES = new AccountCode("12");
    static final AccountCode IGV = new AccountCode("40111");
    public static final AccountCode CUSTOMER_ADVANCES = new AccountCode("122");
    static final AccountCode SALES = new AccountCode("70");
    static final AccountCode PAYABLES = new AccountCode("42");
    static final AccountCode CAPITAL = new AccountCode("50");
    static final AccountCode DIRECT_COSTS = new AccountCode("60");
    static final AccountCode SERVICES = new AccountCode("63");
    static final AccountCode PERSONNEL = new AccountCode("62");
    static final AccountCode LOANS_GRANTED = new AccountCode("16");
    static final AccountCode LOANS_RECEIVED = new AccountCode("45");
    static final AccountCode FINANCIAL_EXPENSES = new AccountCode("67");
    static final AccountCode FINANCIAL_INCOME = new AccountCode("77");
    static final AccountCode FIXED_ASSETS = new AccountCode("33");
    static final AccountCode ACCUMULATED_DEPRECIATION = new AccountCode("39");
    static final AccountCode OTHER_MANAGEMENT_EXPENSES = new AccountCode("65");
    static final AccountCode RETAINED_EARNINGS = new AccountCode("59");

    private PostingRules() { }

    /** D 12 total / H 70 taxable base (H 122 for an advance) + H 40111 IGV. */
    public static RegisterJournalEntryCommand forInvoiceIssued(InvoiceIssuedEvent event) {
        List<JournalLineData> lines = new ArrayList<>();
        lines.add(debit(RECEIVABLES, event.total(), null));
        lines.add(credit(event.advance() ? CUSTOMER_ADVANCES : SALES, event.taxableBase(), event.projectId()));
        if (event.igv().isPositive()) {
            lines.add(credit(IGV, event.igv(), null));
        }
        return new RegisterJournalEntryCommand(event.issuedOn(), "Invoice issued", EntrySource.INVOICE_ISSUED,
                event.invoiceId(), lines);
    }

    /**
     * D 10 / H 12 for money that reached the bank. A retention certificate moves no money and is left out.
     * shortcut: the detraction deposit also goes to 10 instead of 107, and retained amounts stay in 12.
     */
    public static Optional<RegisterJournalEntryCommand> forInvoicePayment(InvoicePaymentReceivedEvent event) {
        if (event.type() == InvoicePaymentType.RETENTION_CERTIFICATE) {
            return Optional.empty();
        }
        return Optional.of(new RegisterJournalEntryCommand(event.receivedOn(), "Invoice payment received",
                EntrySource.INVOICE_PAYMENT, event.paymentId(),
                List.of(debit(CASH, event.amount(), null), credit(RECEIVABLES, event.amount(), null))));
    }

    /**
     * D 60 (63 for a general expense) taxable base + D 40111 IGV / H 10 when paid, H 42 while pending, H 50 for a
     * partner contribution. shortcut: a contribution goes to capital (50) instead of a partner loan account (455).
     */
    public static RegisterJournalEntryCommand forExpense(ExpenseRegisteredEvent event) {
        AccountCode settlement = event.paymentMethod() == PaymentMethod.PARTNER_CONTRIBUTION ? CAPITAL
                : event.paid() ? CASH : PAYABLES;
        List<JournalLineData> lines = new ArrayList<>();
        lines.add(debit(event.projectId() != null ? DIRECT_COSTS : SERVICES, event.taxableBase(), event.projectId()));
        if (event.igv().isPositive()) {
            lines.add(debit(IGV, event.igv(), null));
        }
        lines.add(credit(settlement, event.total(), null));
        return new RegisterJournalEntryCommand(event.spentOn(), "Expense", EntrySource.EXPENSE, event.expenseId(), lines);
    }

    /**
     * D 60 (63 without a project) / H 42 for a payable that is not an expense yet. One created from an expense is
     * already in the expense's entry, and a tax or self-detraction payable is not a cost.
     */
    public static Optional<RegisterJournalEntryCommand> forPayableRegistered(PayableRegisteredEvent event) {
        if (event.expenseId() != null || event.type() == PayableType.TAX || event.type() == PayableType.SELF_DETRACTION) {
            return Optional.empty();
        }
        return Optional.of(new RegisterJournalEntryCommand(event.registeredOn(), "Payable registered",
                EntrySource.PAYABLE_REGISTERED, event.payableId(), List.of(
                        debit(event.projectId() != null ? DIRECT_COSTS : SERVICES, event.total(), event.projectId()),
                        credit(PAYABLES, event.total(), null))));
    }

    /** D 42 / H 10. shortcut: every payable goes through 42 (suppliers), none through 46 (other accounts payable). */
    public static RegisterJournalEntryCommand forPayablePayment(PayablePaidEvent event) {
        return new RegisterJournalEntryCommand(event.paidOn(), "Payable payment", EntrySource.PAYABLE_PAYMENT,
                event.paymentId(), List.of(debit(PAYABLES, event.amount(), null), credit(CASH, event.amount(), null)));
    }

    /**
     * D 33 cost / H the account that already carried the cost as an expense (60 or 63, same project), or H 59 for an
     * asset the company already owned. IGV stays where the expense put it.
     */
    public static RegisterJournalEntryCommand forFixedAssetAcquisition(Long assetId, LocalDate acquiredOn, Money cost,
            AccountCode counterpart, Long projectId) {
        return new RegisterJournalEntryCommand(acquiredOn, "Fixed asset acquisition", EntrySource.FIXED_ASSET_ACQUISITION,
                assetId, List.of(debit(FIXED_ASSETS, cost, null), credit(counterpart, cost, projectId)));
    }

    /** D 39 accumulated depreciation + D 65 what was left to depreciate (the loss) / H 33 cost. */
    public static RegisterJournalEntryCommand forFixedAssetDisposal(Long assetId, LocalDate disposedOn, Money cost,
            Money accumulated) {
        List<JournalLineData> lines = new ArrayList<>();
        if (accumulated.isPositive()) {
            lines.add(debit(ACCUMULATED_DEPRECIATION, accumulated, null));
        }
        Money loss = cost.subtract(accumulated);
        if (loss.isPositive()) {
            lines.add(debit(OTHER_MANAGEMENT_EXPENSES, loss, null));
        }
        lines.add(credit(FIXED_ASSETS, cost, null));
        return new RegisterJournalEntryCommand(disposedOn, "Fixed asset disposal", EntrySource.FIXED_ASSET_DISPOSAL,
                assetId, lines);
    }

    /** Received loan: D 10 / H 45. Granted loan: D 16 / H 10. */
    public static RegisterJournalEntryCommand forLoanDisbursement(LoanDisbursedEvent event) {
        boolean received = event.type() == LoanType.RECEIVED;
        return new RegisterJournalEntryCommand(event.disbursedOn(), "Loan disbursement", EntrySource.LOAN_DISBURSEMENT,
                event.loanId(), List.of(debit(received ? CASH : LOANS_GRANTED, event.principal(), null),
                        credit(received ? LOANS_RECEIVED : CASH, event.principal(), null)));
    }

    /**
     * Received loan: D 45 principal + D 67 interest / H 10. Granted loan: D 10 / H 16 principal + H 77 interest.
     */
    public static RegisterJournalEntryCommand forLoanInstallment(LoanInstallmentPaidEvent event) {
        boolean received = event.type() == LoanType.RECEIVED;
        Money total = event.principal().add(event.interest());
        List<JournalLineData> lines = new ArrayList<>();
        if (received) {
            lines.add(debit(LOANS_RECEIVED, event.principal(), null));
            if (event.interest().isPositive()) {
                lines.add(debit(FINANCIAL_EXPENSES, event.interest(), null));
            }
            lines.add(credit(CASH, total, null));
        } else {
            lines.add(debit(CASH, total, null));
            lines.add(credit(LOANS_GRANTED, event.principal(), null));
            if (event.interest().isPositive()) {
                lines.add(credit(FINANCIAL_INCOME, event.interest(), null));
            }
        }
        return new RegisterJournalEntryCommand(event.paidOn(), "Loan installment", EntrySource.LOAN_INSTALLMENT,
                event.installmentId(), lines);
    }

    /**
     * D 10 / H 50 for money a partner put into a bank account. A contribution that is an expense paid out of pocket
     * is already in the expense's entry, and one without an account moved no money, so neither is posted here.
     */
    public static Optional<RegisterJournalEntryCommand> forPartnerContribution(PartnerContributionRegisteredEvent event) {
        if (event.expenseId() != null || event.bankAccountId() == null) {
            return Optional.empty();
        }
        return Optional.of(new RegisterJournalEntryCommand(event.contributedOn(), "Partner contribution",
                EntrySource.PARTNER_CONTRIBUTION, event.contributionId(),
                List.of(debit(CASH, event.amount(), null), credit(CAPITAL, event.amount(), null))));
    }

    /**
     * D 62 (salaries and overtime), 67 (bank fees and ITF) or 63 (everything else) / H 10.
     * shortcut: a payment with no bank account is still credited to 10, as the UML says.
     */
    public static RegisterJournalEntryCommand forFixedCostPayment(FixedCostPaidEvent event) {
        AccountCode expense = switch (event.type()) {
            case SALARY, OVERTIME -> PERSONNEL;
            case BANK_FEES, ITF -> FINANCIAL_EXPENSES;
            default -> SERVICES;
        };
        return new RegisterJournalEntryCommand(event.paidOn(), "Fixed cost payment", EntrySource.FIXED_COST_PAYMENT,
                event.paymentId(), List.of(debit(expense, event.amount(), null), credit(CASH, event.amount(), null)));
    }

    /**
     * D 122 / H 70 for the advances the project's invoices took. Nothing when there are none.
     * shortcut: the amount is read as PEN; advances in another currency are not recognized yet.
     */
    public static Optional<RegisterJournalEntryCommand> forProjectCompleted(ProjectCompletedEvent event, BigDecimal advances) {
        if (advances.signum() <= 0) {
            return Optional.empty();
        }
        Money amount = new Money(advances, com.brasper.platform.shared.domain.model.valueobjects.Currency.PEN);
        return Optional.of(new RegisterJournalEntryCommand(event.completedOn(), "Project completed",
                EntrySource.PROJECT_COMPLETED, event.projectId(),
                List.of(debit(CUSTOMER_ADVANCES, amount, event.projectId()), credit(SALES, amount, event.projectId()))));
    }

    private static JournalLineData debit(AccountCode account, Money amount, Long projectId) {
        return new JournalLineData(account, amount, Money.zero(amount.currency()), projectId);
    }

    private static JournalLineData credit(AccountCode account, Money amount, Long projectId) {
        return new JournalLineData(account, Money.zero(amount.currency()), amount, projectId);
    }
}
