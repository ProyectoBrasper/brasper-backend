package com.brasper.platform.iam.infrastructure.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditLogFilterTest {

    private final Logger audit = (Logger) LoggerFactory.getLogger("audit");
    private final ListAppender<ILoggingEvent> lines = new ListAppender<>();

    @BeforeEach
    void listen() {
        lines.start();
        audit.addAppender(lines);
    }

    @AfterEach
    void stopListening() {
        audit.detachAppender(lines);
        SecurityContextHolder.clearContext();
    }

    private void run(String method, String uri, int status) throws Exception {
        var response = new MockHttpServletResponse();
        response.setStatus(status);
        new AuditLogFilter().doFilter(new MockHttpServletRequest(method, uri), response, new MockFilterChain());
    }

    private List<String> logged() {
        return lines.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void logsWhoChangedWhatWithTheStatus() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("ana", null, List.of()));

        run("POST", "/api/v1/invoices/7/void", 200);

        assertThat(logged()).containsExactly("ana POST /api/v1/invoices/7/void -> 200");
    }

    @Test
    void anonymousChangesAreLoggedAsAnonymousAndReadsAreNotLogged() throws Exception {
        run("POST", "/api/v1/auth/sign-in", 401);
        run("GET", "/api/v1/invoices", 200);
        run("HEAD", "/api/v1/invoices", 200);

        assertThat(logged()).containsExactly("anonymous POST /api/v1/auth/sign-in -> 401");
    }
}
