package com.brasper.platform.accounting.interfaces.rest.resources;
import java.util.List;

public record TrialBalanceResource(int year, int month, List<Line> lines) {
    public record Line(String accountCode, String accountName, MoneyResource debit, MoneyResource credit) { }
}
