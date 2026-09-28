package com.proitbridge.lms.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF handling for the React single-page app (Spring Security 6.3).
 *
 * The server writes the token to the XSRF-TOKEN cookie on every response. The
 * client reads that cookie and echoes it in the X-XSRF-TOKEN header on every
 * state-changing request. A page on another site can make the browser send our
 * cookies, but it cannot read them, so it cannot produce the matching header.
 *
 * Header values are compared as-is (the SPA sends the raw cookie value); anything
 * rendered into a response body keeps Spring's BREACH-safe XOR masking.
 * Spring Security 6.4+ ships this as csrf.spa(); 6.3 needs this class.
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);
        // Load the deferred token so the XSRF-TOKEN cookie is always present,
        // including on the very first GET before anyone has signed in.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor)
                .resolveCsrfTokenValue(request, csrfToken);
    }
}
