package com.brasper.platform.iam.application.outboundservices;

import com.brasper.platform.iam.domain.model.aggregates.User;
import java.util.Optional;

public interface TokenService {

    String generateToken(User user);

    /** The id of the user the token was issued to, or empty if it is malformed, forged or expired. */
    Optional<Long> validateToken(String token);
}
