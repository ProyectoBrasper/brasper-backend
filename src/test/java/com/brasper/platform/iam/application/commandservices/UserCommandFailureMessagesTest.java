package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.billing.application.commandservices.InvoiceCommandFailure;
import com.brasper.platform.billing.application.commandservices.ScheduledCollectionCommandFailure;
import com.brasper.platform.expenses.application.commandservices.ExpenseCommandFailure;
import com.brasper.platform.expenses.application.commandservices.SupplierCommandFailure;
import com.brasper.platform.treasury.application.commandservices.BankAccountCommandFailure;
import com.brasper.platform.treasury.application.commandservices.LoanCommandFailure;
import com.brasper.platform.treasury.application.commandservices.PayableCommandFailure;
import com.brasper.platform.treasury.application.commandservices.PartnerCommandFailure;
import com.brasper.platform.projects.application.commandservices.ClientCommandFailure;
import com.brasper.platform.projects.application.commandservices.ProjectCommandFailure;
import com.brasper.platform.shared.application.result.CommandFailure;
import com.brasper.platform.staff.application.commandservices.EmployeeCommandFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class UserCommandFailureMessagesTest {

    @Test
    void everyFailureHasAMessageInEnglishAndSpanish() {
        for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.of("es")}) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            List<CommandFailure> failures = new ArrayList<>(List.of(UserCommandFailure.values()));
            failures.addAll(List.of(ClientCommandFailure.values()));
            failures.addAll(List.of(ProjectCommandFailure.values()));
            failures.addAll(List.of(InvoiceCommandFailure.values()));
            failures.addAll(List.of(ScheduledCollectionCommandFailure.values()));
            failures.addAll(List.of(SupplierCommandFailure.values()));
            failures.addAll(List.of(ExpenseCommandFailure.values()));
            failures.addAll(List.of(BankAccountCommandFailure.values()));
            failures.addAll(List.of(PartnerCommandFailure.values()));
            failures.addAll(List.of(LoanCommandFailure.values()));
            failures.addAll(List.of(PayableCommandFailure.values()));
            failures.addAll(List.of(EmployeeCommandFailure.values()));
            for (CommandFailure failure : failures) {
                assertThat(bundle.containsKey(failure.messageKey()))
                        .as("%s in %s", failure.messageKey(), locale)
                        .isTrue();
            }
        }
    }
}
