package com.brasper.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.accounting.application.commandservices.FixedAssetCommandService;
import com.brasper.platform.accounting.application.commandservices.TaxCommandService;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.TaxReturn;
import com.brasper.platform.accounting.domain.model.commands.PayTaxReturnCommand;
import com.brasper.platform.accounting.domain.model.commands.PrepareTaxReturnCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.RunMonthlyDepreciationCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalance;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalanceLine;
import com.brasper.platform.billing.application.commandservices.InvoiceCommandService;
import com.brasper.platform.billing.domain.model.aggregates.Invoice;
import com.brasper.platform.billing.domain.model.commands.IssueInvoiceCommand;
import com.brasper.platform.billing.domain.model.commands.RegisterInvoicePaymentCommand;
import com.brasper.platform.billing.domain.model.commands.VoidInvoiceCommand;
import com.brasper.platform.billing.domain.model.valueobjects.InvoiceNumber;
import com.brasper.platform.billing.domain.model.valueobjects.InvoicePaymentType;
import com.brasper.platform.expenses.application.commandservices.ExpenseCommandService;
import com.brasper.platform.expenses.domain.model.commands.RegisterExpenseCommand;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.expenses.domain.model.valueobjects.Voucher;
import com.brasper.platform.expenses.domain.model.valueobjects.VoucherType;
import com.brasper.platform.projects.application.commandservices.ClientCommandService;
import com.brasper.platform.projects.application.commandservices.ProjectCommandService;
import com.brasper.platform.projects.domain.model.commands.CreateClientCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.RegisterProjectProgressCommand;
import com.brasper.platform.projects.domain.model.commands.StartProjectCommand;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.reporting.application.queryservices.CashFlowQueryService;
import com.brasper.platform.reporting.application.queryservices.DashboardQueryService;
import com.brasper.platform.reporting.application.queryservices.FinancialStatementsQueryService;
import com.brasper.platform.reporting.application.queryservices.ProjectProfitabilityQueryService;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowLine;
import com.brasper.platform.reporting.domain.model.readmodels.CashFlowReport;
import com.brasper.platform.reporting.domain.model.readmodels.IncomeStatement;
import com.brasper.platform.reporting.domain.model.readmodels.ProjectProfitability;
import com.brasper.platform.reporting.domain.model.valueobjects.CashFlowConcept;
import com.brasper.platform.shared.domain.model.valueobjects.CashFlowWeek;
import com.brasper.platform.shared.domain.model.valueobjects.CostCategory;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.shared.domain.model.valueobjects.Ruc;
import com.brasper.platform.staff.application.commandservices.FixedCostCommandService;
import com.brasper.platform.staff.domain.model.commands.CreateFixedCostCommand;
import com.brasper.platform.staff.domain.model.commands.RegisterFixedCostPaymentCommand;
import com.brasper.platform.staff.domain.model.valueobjects.CostCode;
import com.brasper.platform.staff.domain.model.valueobjects.FixedCostType;
import com.brasper.platform.staff.domain.model.valueobjects.PaymentSchedule;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandService;
import com.brasper.platform.treasury.application.commandservices.PayableCommandService;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.domain.model.commands.CreatePayableCommand;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPayablePaymentCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.PayableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * A month of a small company with made-up figures, driven through the real services (each one commits, so the
 * events fire). It checks that the contexts agree with each other: the bank balance, the ledger, the statements,
 * the cash flow and the profitability report must all tell the same story. April 2025 is in the past and no other
 * test uses it, so every figure for it is exact.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EndToEndFlowTest {

    private static final Period APRIL = new Period(2025, 4);
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired private ClientCommandService clients;
    @Autowired private ProjectCommandService projects;
    @Autowired private InvoiceCommandService invoices;
    @Autowired private ExpenseCommandService expenses;
    @Autowired private FixedCostCommandService fixedCosts;
    @Autowired private FixedAssetCommandService fixedAssets;
    @Autowired private TaxCommandService tax;
    @Autowired private BankAccountCommandService bankAccounts;
    @Autowired private BankAccountQueryService bankQueries;
    @Autowired private PartnerCommandService partners;
    @Autowired private PayableCommandService payables;
    @Autowired private LedgerQueryService ledger;
    @Autowired private FinancialStatementsQueryService statements;
    @Autowired private CashFlowQueryService cashFlow;
    @Autowired private ProjectProfitabilityQueryService profitability;
    @Autowired private DashboardQueryService dashboard;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private static LocalDate april(int day) {
        return LocalDate.of(2025, 4, day);
    }

    private Long startedProject(String ruc, String code, String budget) {
        Long clientId = clients.handle(new CreateClientCommand("E2E client " + ruc, new Ruc(ruc), null, null, null))
                .success().orElseThrow().getId();
        Long projectId = projects.handle(new CreateProjectCommand(new ProjectCode(code), "E2E plant", null, clientId,
                pen(budget), LocalDate.of(2025, 3, 1), new Percentage(BigDecimal.ONE),
                new Percentage(new BigDecimal("0.03")))).success().orElseThrow().getId();
        projects.handle(new StartProjectCommand(projectId)).success().orElseThrow();
        return projectId;
    }

    private BigDecimal debit(TrialBalance balance, String code) {
        return balance.lines().stream().filter(l -> l.accountCode().value().equals(code))
                .map(l -> l.debit().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal credit(TrialBalance balance, String code) {
        return balance.lines().stream().filter(l -> l.accountCode().value().equals(code))
                .map(l -> l.credit().amount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal flow(CashFlowReport report, boolean inflow, CashFlowConcept concept) {
        return report.lines().stream().map(line -> (inflow ? line.inflows() : line.outflows()).get(concept))
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void aWholeMonthLeavesTheBankTheLedgerTheStatementsAndTheReportsInAgreement() {
        BigDecimal differenceBefore = statements.balanceSheet(APRIL).difference();
        Long projectId = startedProject("20999000001", "901BR-ABR-2025", "10000");
        Long bank = bankAccounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber("996-1"),
                AccountType.CHECKING, Currency.PEN, pen("10000"), LocalDate.of(2025, 1, 1))).success().orElseThrow().getId();

        // 5,000 put in by a partner
        Long partner = partners.handle(new RegisterPartnerCommand("E2E Partner", "Engineer")).success().orElseThrow().getId();
        partners.handle(new RegisterPartnerContributionCommand(partner, april(2), pen("5000"), false, bank, null, "Capital"))
                .success().orElseThrow();

        // an invoice of 10,000 + 18% IGV, paid in full
        Invoice invoice = invoices.handle(new IssueInvoiceCommand(new InvoiceNumber("E001", 1), april(5), null, projectId,
                pen("10000"), false, false, false, false, null)).success().orElseThrow();
        assertThat(invoice.getTotal().amount()).isEqualByComparingTo("11800");
        invoices.handle(new RegisterInvoicePaymentCommand(invoice.getId(), april(15), pen("11800"),
                InvoicePaymentType.NET_DEPOSIT, bank, "OP-1")).success().orElseThrow();

        // materials for the project: 2,000 + 360 IGV, paid from the account
        expenses.handle(new RegisterExpenseCommand(april(8), projectId, null, CostCategory.MATERIALS, "Steel",
                new Voucher(VoucherType.INVOICE, "F001-100"), pen("2000"), false, PaymentMethod.BANK_ACCOUNT, bank, null,
                true)).success().orElseThrow();

        // office rent of 1,000
        Long rent = fixedCosts.handle(new CreateFixedCostCommand(new CostCode("FC-E2E"), "Office rent",
                FixedCostType.OFFICE_RENT, pen("1000"), null, new PaymentSchedule(List.of(1)))).success().orElseThrow().getId();
        fixedCosts.handle(new RegisterFixedCostPaymentCommand(rent, april(10), APRIL, pen("1000"), bank, null))
                .success().orElseThrow();

        // a supplier payable of 500, scheduled and then paid
        Long payable = payables.handle(new CreatePayableCommand("Supplier invoice", PayableType.SUPPLIER, pen("500"), null,
                null, null, CashFlowWeek.from(april(20)))).success().orElseThrow().getId();
        payables.handle(new RegisterPayablePaymentCommand(payable, april(22), pen("500"), bank)).success().orElseThrow();

        // a laptop of 1,200 over 12 months, depreciated for April (acquired in January: 100 a month)
        fixedAssets.handle(new RegisterFixedAssetCommand("Laptop", LocalDate.of(2025, 1, 10), pen("1200"), 12, null))
                .success().orElseThrow();
        assertThat(fixedAssets.handle(new RunMonthlyDepreciationCommand(APRIL)).success().orElseThrow()).hasSize(1);

        // the IGV return: 1,800 output - 360 input = 1,440, plus 1.5% of 10,000 sales = 150
        TaxReturn taxReturn = tax.handle(new PrepareTaxReturnCommand(APRIL)).success().orElseThrow();
        assertThat(taxReturn.getOutputIgv().amount()).isEqualByComparingTo("1800");
        assertThat(taxReturn.getInputIgv().amount()).isEqualByComparingTo("360");
        assertThat(taxReturn.totalPayable().amount()).isEqualByComparingTo("1590");
        tax.handle(new PayTaxReturnCommand(taxReturn.getId(), april(28), bank)).success().orElseThrow();

        // the project reaches 100%
        projects.handle(new RegisterProjectProgressCommand(projectId, APRIL, new Percentage(BigDecimal.ONE)))
                .success().orElseThrow();

        // --- the bank: 10,000 + 5,000 + 11,800 - 2,360 - 1,000 - 500 - 1,590
        BigDecimal balance = bankQueries.findById(bank).orElseThrow().currentBalance().amount();
        assertThat(balance).isEqualByComparingTo("21350");

        // --- the ledger of April: every account as expected, and it balances
        TrialBalance trial = ledger.trialBalance(APRIL);
        assertThat(debit(trial, "10")).isEqualByComparingTo("16800");
        assertThat(credit(trial, "10")).isEqualByComparingTo("5450");
        assertThat(debit(trial, "12")).isEqualByComparingTo("11800");
        assertThat(credit(trial, "12")).isEqualByComparingTo("11800");
        assertThat(credit(trial, "70")).isEqualByComparingTo("10000");
        assertThat(credit(trial, "40111")).isEqualByComparingTo("1800");
        assertThat(debit(trial, "40111")).isEqualByComparingTo("1800");
        assertThat(debit(trial, "60")).isEqualByComparingTo("2000");
        assertThat(debit(trial, "63")).isEqualByComparingTo("1000");
        assertThat(debit(trial, "42")).isEqualByComparingTo("500");
        assertThat(debit(trial, "4017")).isEqualByComparingTo("150");
        assertThat(credit(trial, "50")).isEqualByComparingTo("5000");
        assertThat(debit(trial, "68")).isEqualByComparingTo("100");
        assertThat(credit(trial, "39")).isEqualByComparingTo("100");
        BigDecimal debits = trial.lines().stream().map(TrialBalanceLine::debit).map(Money::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credits = trial.lines().stream().map(TrialBalanceLine::credit).map(Money::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(debits).isEqualByComparingTo(credits);

        // --- the bank and the ledger agree on the cash that moved in April
        assertThat(debit(trial, "10").subtract(credit(trial, "10"))).isEqualByComparingTo(balance.subtract(new BigDecimal("10000")));

        // --- income statement and balance sheet
        IncomeStatement result = statements.incomeStatement(APRIL, APRIL);
        assertThat(result.revenue()).isEqualByComparingTo("10000");
        assertThat(result.directCosts()).isEqualByComparingTo("2000");
        assertThat(result.grossProfit()).isEqualByComparingTo("8000");
        assertThat(result.operatingExpenses()).isEqualByComparingTo("1100");
        assertThat(result.netProfit()).isEqualByComparingTo("6900");
        assertThat(statements.balanceSheet(APRIL).difference()).isEqualByComparingTo(differenceBefore);

        // --- the cash flow of April is real (the month is past) and says the same as the bank
        CashFlowReport report = cashFlow.weekly(APRIL, TODAY);
        assertThat(report.lines()).noneMatch(CashFlowLine::projected);
        assertThat(flow(report, true, CashFlowConcept.INVOICE_COLLECTIONS)).isEqualByComparingTo("11800");
        assertThat(flow(report, true, CashFlowConcept.PARTNER_CONTRIBUTIONS)).isEqualByComparingTo("5000");
        assertThat(flow(report, false, CashFlowConcept.SUPPLIERS)).isEqualByComparingTo("2360");
        assertThat(flow(report, false, CashFlowConcept.OFFICE_RENT)).isEqualByComparingTo("1000");
        assertThat(flow(report, false, CashFlowConcept.PENDING_PAYABLES)).isEqualByComparingTo("500");
        assertThat(flow(report, false, CashFlowConcept.TAXES)).isEqualByComparingTo("1590");
        assertThat(report.lines().stream().map(CashFlowLine::netFlow).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("11350");

        // --- the project: cost 2,000 against no estimate, 11,800 invoiced and collected
        ProjectProfitability project = profitability.findByProject(projectId, APRIL).orElseThrow();
        assertThat(project.actualCost()).isEqualByComparingTo("2000");
        assertThat(project.invoiced()).isEqualByComparingTo("11800");
        assertThat(project.collected()).isEqualByComparingTo("11800");
        assertThat(project.salesCommission()).isEqualByComparingTo("354.00");
        assertThat(project.grossProfit()).isEqualByComparingTo("7646.00");
        assertThat(project.status()).isEqualTo("COMPLETED");
        assertThat(dashboard.dashboard(APRIL, TODAY).alerts()).contains("COST_OVERRUN:901BR-ABR-2025");
    }

    @Test
    void voidingAnInvoiceCancelsItsEffectOnTheLedger() {
        Long projectId = startedProject("20999000002", "902BR-MAY-2025", "1000");
        LocalDate day = LocalDate.of(2025, 5, 6);
        Invoice invoice = invoices.handle(new IssueInvoiceCommand(new InvoiceNumber("E001", 2), day, null, projectId,
                pen("1000"), false, false, false, false, null)).success().orElseThrow();
        TrialBalance issued = ledger.trialBalance(new Period(2025, 5));
        assertThat(credit(issued, "70")).isEqualByComparingTo("1000");

        invoices.handle(new VoidInvoiceCommand(invoice.getId())).success().orElseThrow();

        // the reversal is dated today, so the cancelled sale shows in the month it was undone
        TrialBalance voided = ledger.trialBalance(Period.from(TODAY));
        assertThat(debit(voided, "70")).isGreaterThanOrEqualTo(new BigDecimal("1000"));
        assertThat(credit(voided, "12")).isGreaterThanOrEqualTo(new BigDecimal("1180"));
    }
}
