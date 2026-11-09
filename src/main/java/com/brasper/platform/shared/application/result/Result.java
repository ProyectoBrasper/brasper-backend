package com.brasper.platform.shared.application.result;

import java.util.Optional;
import java.util.function.Function;

/** Outcome of a command: a value or an expected failure. Unexpected errors stay as exceptions. */
public sealed interface Result<T, E> {

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    Optional<T> success();

    Optional<E> failure();

    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure);

    record Success<T, E>(T value) implements Result<T, E> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public Optional<T> success() {
            return Optional.ofNullable(value);
        }

        @Override
        public Optional<E> failure() {
            return Optional.empty();
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    record Failure<T, E>(E error) implements Result<T, E> {

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public Optional<T> success() {
            return Optional.empty();
        }

        @Override
        public Optional<E> failure() {
            return Optional.ofNullable(error);
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return new Failure<>(error);
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super E, ? extends R> onFailure) {
            return onFailure.apply(error);
        }
    }
}
