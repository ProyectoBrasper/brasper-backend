package com.brasper.platform.iam.application.commandservices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class UserCommandFailureMessagesTest {

    @Test
    void everyFailureHasAMessageInEnglishAndSpanish() {
        for (Locale locale : new Locale[] {Locale.ENGLISH, Locale.of("es")}) {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
            for (UserCommandFailure failure : UserCommandFailure.values()) {
                assertThat(bundle.containsKey(failure.messageKey()))
                        .as("%s in %s", failure.messageKey(), locale)
                        .isTrue();
            }
        }
    }
}
