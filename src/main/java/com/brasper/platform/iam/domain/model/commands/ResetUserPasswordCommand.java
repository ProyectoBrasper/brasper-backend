package com.brasper.platform.iam.domain.model.commands;

public record ResetUserPasswordCommand(Long userId, String newPassword) {
}
