package com.brasper.platform.accounting.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.TestcontainersConfiguration;
import com.brasper.platform.accounting.application.queryservices.LedgerQueryService;
import com.brasper.platform.accounting.domain.model.aggregates.FixedAsset;
import com.brasper.platform.accounting.domain.model.aggregates.JournalEntry;
import com.brasper.platform.accounting.domain.model.commands.DisposeFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.commands.RegisterFixedAssetCommand;
import com.brasper.platform.accounting.domain.model.valueobjects.EntrySource;
import com.brasper.platform.expenses.domain.events.ExpenseRegisteredEvent;
import com.brasper.platform.expenses.domain.model.valueobjects.PaymentMethod;
import com.brasper.platform.shared.domain.model.valueobjects.Currency;
import com.brasper.platform.shared.domain.model.valueobjects.Money;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/** Not @Transactional on purpose: the expense entry is posted when the publishing transaction commits. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FixedAssetLedgerTest {

    private static final LocalDate DAY = LocalDate.of(2037, 3, 10);

    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private TransactionTemplate transaction;
    @Autowired private LedgerQueryService ledger;
    @Autowired private FixedAssetCommandService fixedAssets;

    private static Money pen(String amount) {
        return new Money(new BigDecimal(amount), Currency.PEN);
    }

    private List<JournalEntry> entries(EntrySource source, Long sourceId) {
        return ledger.findEntriesByPeriod(new Period(2037, 3)).stream()
                .filter(e -> e.getSource() == source && sourceId.equals(e.getSourceId())).toList();
    }

    /** Reads the lines inside a transaction: they load lazily. */
    private List<String> lines(JournalEntry entry) {
        return transaction.execute(status -> ledger.findEntryById(entry.getId()).orElseThrow().getLines().stream()
                .map(l -> l.getAccountCode().value() + (l.getDebit().isPositive() ? " D " + l.getDebit().amount().intValue()
                        : " H " + l.getCredit().amount().intValue()) + " p" + l.getProjectId()).toList());
    }

    @Test
    void anAssetPaidWithAnExpenseMovesItsCostOutOfTheProjectCostIntoFixedAssets() {
        transaction.executeWithoutResult(status -> publisher.publishEvent(new ExpenseRegisteredEvent(9701L, DAY, 9801L,
                pen("1200"), pen("216"), pen("1416"), false, PaymentMethod.PETTY_CASH, null, null)));

        FixedAsset asset = fixedAssets.handle(new RegisterFixedAssetCommand("Laptop", DAY, pen("1200"), 12, 9701L))
                .success().orElseThrow();

        List<JournalEntry> acquisition = entries(EntrySource.FIXED_ASSET_ACQUISITION, asset.getId());
        assertThat(acquisition).hasSize(1);
        assertThat(lines(acquisition.getFirst())).containsExactly("33 D 1200 pnull", "60 H 1200 p9801");
    }

    @Test
    void anAssetWithoutAnExpenseIsAnOpeningBalanceAndDisposingWritesOffWhatIsLeft() {
        FixedAsset asset = fixedAssets.handle(new RegisterFixedAssetCommand("Desk", DAY, pen("1200"), 12, null))
                .success().orElseThrow();
        assertThat(lines(entries(EntrySource.FIXED_ASSET_ACQUISITION, asset.getId()).getFirst()))
                .containsExactly("33 D 1200 pnull", "59 H 1200 pnull");

        fixedAssets.handle(new DisposeFixedAssetCommand(asset.getId(), DAY.plusMonths(3))).success().orElseThrow();

        List<JournalEntry> disposal = ledger.findEntriesByPeriod(new Period(2037, 6)).stream()
                .filter(e -> e.getSource() == EntrySource.FIXED_ASSET_DISPOSAL && asset.getId().equals(e.getSourceId())).toList();
        assertThat(disposal).hasSize(1);
        assertThat(lines(disposal.getFirst())).containsExactly("39 D 300 pnull", "65 D 900 pnull", "33 H 1200 pnull");
    }
}
