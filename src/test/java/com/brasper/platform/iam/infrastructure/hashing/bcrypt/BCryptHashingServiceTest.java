package com.brasper.platform.iam.infrastructure.hashing.bcrypt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptHashingServiceTest {

    private final BCryptHashingService hashing = new BCryptHashingService();

    @Test
    void hashIsNotThePasswordAndIsSaltedPerCall() {
        String first = hashing.encode("s3cret-pass");
        String second = hashing.encode("s3cret-pass");

        assertThat(first).isNotEqualTo("s3cret-pass").isNotEqualTo(second);
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String hash = hashing.encode("s3cret-pass");

        assertThat(hashing.matches("s3cret-pass", hash)).isTrue();
        assertThat(hashing.matches("other-pass", hash)).isFalse();
    }
}
