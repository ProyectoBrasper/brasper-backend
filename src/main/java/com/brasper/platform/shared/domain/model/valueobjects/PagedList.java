package com.brasper.platform.shared.domain.model.valueobjects;

import java.util.List;

/** One page of items plus how many items match in all. */
public record PagedList<T>(List<T> items, long total) {
}
