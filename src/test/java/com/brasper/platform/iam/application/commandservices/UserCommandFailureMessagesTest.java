package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import com.brasper.platform.projects.application.commandservices.ClientCommandFailure;
import com.brasper.platform.shared.application.result.CommandFailure;
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
            for (CommandFailure failure : failures) {
                assertThat(bundle.containsKey(failure.messageKey()))
                        .as("%s in %s", failure.messageKey(), locale)
                        .isTrue();
            }
        }
    }
}
