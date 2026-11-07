package com.brasper.platform.accounting.interfaces.rest.resources;
import com.brasper.platform.accounting.domain.model.valueobjects.LedgerAccountType;

public record LedgerAccountResource(Long id, String code, String name, LedgerAccountType type, boolean active) { }
