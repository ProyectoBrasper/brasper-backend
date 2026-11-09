package com.brasper.platform.iam.infrastructure.hashing.bcrypt;

import com.brasper.platform.iam.application.outboundservices.HashingService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BCryptHashingService implements HashingService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
