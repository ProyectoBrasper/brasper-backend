package com.brasper.platform.projects.domain.model.commands;

import com.brasper.platform.shared.domain.model.valueobjects.Ruc;

public record CreateClientCommand(String businessName, Ruc ruc, String contactName, String phone, String email) {
}
