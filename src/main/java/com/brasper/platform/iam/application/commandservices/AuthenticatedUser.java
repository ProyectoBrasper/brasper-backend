package com.brasper.platform.iam.application.commandservices;

import com.brasper.platform.iam.domain.model.aggregates.User;

public record AuthenticatedUser(User user, String token) {
}
