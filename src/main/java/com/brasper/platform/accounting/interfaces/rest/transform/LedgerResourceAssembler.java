package com.brasper.platform.accounting.interfaces.rest.transform;
import com.brasper.platform.accounting.domain.model.aggregates.*;
import com.brasper.platform.accounting.domain.model.valueobjects.TrialBalance;
import com.brasper.platform.accounting.interfaces.rest.resources.*;
import com.brasper.platform.shared.domain.model.valueobjects.Money;

public final class LedgerResourceAssembler {
    private LedgerResourceAssembler() { }
    public static MoneyResource toResource(Money money) { return new MoneyResource(money.amount(), money.currency()); }
    public static LedgerAccountResource toResource(LedgerAccount account) {
        return new LedgerAccountResource(account.getId(), account.getCode().value(), account.getName(), account.getType(), account.isActive());
    }
    public static JournalEntryResource toResource(JournalEntry entry) {
        return new JournalEntryResource(entry.getId(), entry.getEntryDate(), entry.getPeriod().year(), entry.getPeriod().month(),
                entry.getDescription(), entry.getSource(), entry.getSourceId(), entry.isReversed(), entry.getReversalOfId(),
                toResource(entry.totalDebit()), toResource(entry.totalCredit()), entry.getLines().stream().map(line ->
                        new JournalLineResource(line.getAccountCode().value(), toResource(line.getDebit()), toResource(line.getCredit()), line.getProjectId())).toList());
    }
    public static TrialBalanceResource toResource(TrialBalance balance) {
        return new TrialBalanceResource(balance.period().year(), balance.period().month(), balance.lines().stream().map(line ->
                new TrialBalanceResource.Line(line.accountCode().value(), line.accountName(), toResource(line.debit()), toResource(line.credit()))).toList());
    }
}
