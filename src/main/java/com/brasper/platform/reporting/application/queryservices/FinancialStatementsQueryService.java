package com.brasper.platform.reporting.application.queryservices;

import com.brasper.platform.reporting.application.outboundservices.StatementsSource;
import com.brasper.platform.reporting.domain.model.readmodels.BalanceSheet;
import com.brasper.platform.reporting.domain.model.readmodels.IncomeStatement;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

/**
 * shortcut: accounts are grouped by the PCGE element in their code (1 to 3 assets, 4 liabilities, 5 equity,
 * 60 direct costs, 67 financial, the rest of 6 operating, 7 revenue, 8 income tax). Accounts that do not follow
 * the PCGE numbering land in the wrong group; there is no period closing, so the whole profit sits in RESULT.
 */
@Service
public class FinancialStatementsQueryService {

    public static final String RESULT = "RESULT";

    private final StatementsSource source;

    public FinancialStatementsQueryService(StatementsSource source) {
        this.source = source;
    }

    public IncomeStatement incomeStatement(Period from, Period to) {
        if (from.year() * 12 + from.month() > to.year() * 12 + to.month()) {
            throw new IllegalArgumentException("The first month must not come after the last month");
        }
        Map<String, BigDecimal> movements = source.movementsBetween(from.startDate(), to.endDate());
        return new IncomeStatement(from, to,
                sum(movements, code -> code.startsWith("7")),
                sum(movements, code -> code.startsWith("60")),
                sum(movements, code -> code.startsWith("6") && !code.startsWith("60") && !code.startsWith("67")),
                sum(movements, code -> code.startsWith("67")),
                sum(movements, code -> code.startsWith("8")));
    }

    public BalanceSheet balanceSheet(Period period) {
        Map<String, BigDecimal> balances = source.balancesAt(period.endDate());
        Map<String, BigDecimal> equity = group(balances, code -> code.startsWith("5"));
        BigDecimal result = sum(balances, code -> code.startsWith("7"))
                .subtract(sum(balances, code -> code.startsWith("6") || code.startsWith("8")));
        equity.put(RESULT, result);
        return new BalanceSheet(period.endDate(),
                group(balances, code -> "123".contains(code.substring(0, 1))),
                group(balances, code -> code.startsWith("4")), equity);
    }

    private static BigDecimal sum(Map<String, BigDecimal> amounts, Predicate<String> accounts) {
        return amounts.entrySet().stream().filter(e -> accounts.test(e.getKey())).map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Map<String, BigDecimal> group(Map<String, BigDecimal> amounts, Predicate<String> accounts) {
        Map<String, BigDecimal> group = new TreeMap<>();
        amounts.forEach((code, amount) -> {
            if (accounts.test(code) && amount.signum() != 0) {
                group.put(code, amount);
            }
        });
        return group;
    }
}
