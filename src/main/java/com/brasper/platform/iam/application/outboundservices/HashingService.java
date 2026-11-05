package com.brasper.platform.iam.application.outboundservices;

public interface HashingService {

    String encode(CharSequence rawPassword);

    boolean matches(CharSequence rawPassword, String hash);
}
