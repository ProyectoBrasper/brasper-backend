package com.brasper.platform.shared.domain.model.valueobjects;

/** A zero-based page of a list. The REST lists take it as optional {@code page} and {@code size} query parameters. */
public record PageSpec(int page, int size) {

    public static final int MAX_SIZE = 200;

    public PageSpec {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0: " + page);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE + ": " + size);
        }
    }

    /** Null when neither is given (the whole list); both must come together. */
    public static PageSpec of(Integer page, Integer size) {
        if (page == null && size == null) {
            return null;
        }
        if (page == null || size == null) {
            throw new IllegalArgumentException("page and size must be given together");
        }
        return new PageSpec(page, size);
    }
}
