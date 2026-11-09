package com.brasper.platform.accounting.application.eventhandlers;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Percentage;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandService;
import com.brasper.platform.treasury.application.commandservices.LoanCommandService;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandService;
import com.brasper.platform.treasury.application.queryservices.BankAccountQueryService;
import com.brasper.platform.treasury.domain.model.commands.OpenBankAccountCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterLoanCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerCommand;
import com.brasper.platform.treasury.domain.model.commands.RegisterPartnerContributionCommand;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountNumber;
import com.brasper.platform.treasury.domain.model.valueobjects.AccountType;
import com.brasper.platform.treasury.domain.model.valueobjects.Bank;
import com.brasper.platform.treasury.domain.model.valueobjects.LoanType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Goes through the real Treasury services, which commit on their own, so the handlers run for real. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TreasuryPostingEventHandlerTest {

    private static final LocalDate DAY = LocalDate.of(2036, 8, 10);

    @Autowired private BankAccountCommandService bankAccounts;
    @Autowired private BankAccountQueryService bankQueries;
    @Autowired private LoanCommandService loans;
    @Autowired private PartnerCommandService partners;
    @Autowired private LedgerQueryService ledger;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private Long bankAccount(String number) {
        return bankAccounts.handle(new OpenBankAccountCommand(Bank.BCP, new AccountNumber(number), AccountType.CHECKING,
                Currency.PEN, pen("0"), DAY)).success().orElseThrow().getId();
    }

    private List<JournalEntry> entries(EntrySource source) {
        return ledger.findEntriesByPeriod(new Period(2036, 8)).stream().filter(e -> e.getSource() == source).toList();
    }

    @Test
    void aReceivedLoanIsPostedToTheLedgerAndMovesCashOnce() {
        Long bank = bankAccount("994-1");

        var loan = loans.handle(new RegisterLoanCommand(LoanType.RECEIVED, "Bank", pen("5000"),
                new Percentage(new BigDecimal("0.10")), DAY, bank)).success().orElseThrow();

        assertThat(entries(EntrySource.LOAN_DISBURSEMENT)).extracting(JournalEntry::getSourceId).contains(loan.getId());
        assertThat(bankQueries.findById(bank).orElseThrow().currentBalance().amount()).isEqualByComparingTo("5000");
    }

    @Test
    void aPartnerContributionIntoAnAccountIsPostedAndDepositedButOneForAnExpenseIsNot() {
        Long bank = bankAccount("994-2");
        Long partner = partners.handle(new RegisterPartnerCommand("Ana Quispe", "Engineer")).success().orElseThrow().getId();

        var saved = partners.handle(new RegisterPartnerContributionCommand(partner, DAY, pen("300"), false, bank,
                null, "Capital")).success().orElseThrow();
        partners.handle(new RegisterPartnerContributionCommand(partner, DAY, pen("80"), true, bank, 4242L, "Paid expense"));

        Long contribution = saved.getContributions().getFirst().getId();
        assertThat(entries(EntrySource.PARTNER_CONTRIBUTION)).extracting(JournalEntry::getSourceId)
                .containsExactly(contribution);
        assertThat(bankQueries.findById(bank).orElseThrow().currentBalance().amount()).isEqualByComparingTo("300");
    }
}
