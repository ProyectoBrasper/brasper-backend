package com.brasper.platform.shared.application.result;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void successCarriesTheValue() {
        Result<Integer, String> result = Result.success(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.success()).contains(5);
        assertThat(result.failure()).isEmpty();
    }

    @Test
    void failureCarriesTheError() {
        Result<Integer, String> result = Result.failure("boom");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).contains("boom");
        assertThat(result.success()).isEmpty();
    }

    @Test
    void mapTransformsOnlySuccess() {
        assertThat(Result.<Integer, String>success(2).map(n -> n * 10)).isEqualTo(Result.success(20));
        assertThat(Result.<Integer, String>failure("boom").map(n -> n * 10)).isEqualTo(Result.failure("boom"));
    }

    @Test
    void foldPicksTheMatchingBranch() {
        Result<Integer, String> ok = Result.success(7);
        Result<Integer, String> ko = Result.failure("boom");

        String fromOk = ok.fold(n -> "ok " + n, e -> "ko " + e);
        String fromKo = ko.fold(n -> "ok " + n, e -> "ko " + e);

        assertThat(fromOk).isEqualTo("ok 7");
        assertThat(fromKo).isEqualTo("ko boom");
    }
}
