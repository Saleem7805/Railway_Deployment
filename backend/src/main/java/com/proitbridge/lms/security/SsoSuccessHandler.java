package com.proitbridge.lms.security;

import com.proitbridge.lms.service.SsoAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class SsoSuccessHandler implements AuthenticationSuccessHandler {
    private final SsoAuthService sso;
    private final String frontend;

    public SsoSuccessHandler(SsoAuthService sso,
                             @Value("${lms.app.base-url}") String frontend) {
        this.sso = sso;
        this.frontend = frontend;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = firstNonBlank(principal.getAttribute("email"), principal.getAttribute("preferred_username"),
                principal.getAttribute("upn"));
        String deviceId = cookie(request, "pib_sso_device");
        if (deviceId == null || deviceId.isBlank()) deviceId = UUID.randomUUID().toString();

        String label = request.getHeader("User-Agent");
        String ip = request.getRemoteAddr();
        Map<String, Object> session = sso.signIn(email, deviceId, label == null ? "SSO browser" : label,
                ip, label);
        String token = String.valueOf(session.get("token"));

        Cookie clear = new Cookie("pib_sso_device", "");
        clear.setPath("/");
        clear.setMaxAge(0);
        response.addCookie(clear);

        /*
         * The OIDC login leaves an authenticated session behind, tied to the JSESSIONID
         * cookie. The LMS never uses it: from here on the person is identified by the
         * JWT. Left in place it would be a second, cookie-based way in, which is
         * exactly what a CSRF attack rides on. It is thrown away now that the JWT exists.
         */
        SecurityContextHolder.clearContext();
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) httpSession.invalidate();

        String target = frontend.replaceAll("/$", "") + "/sso/callback#token="
                + URLEncoder.encode(token, StandardCharsets.UTF_8);
        response.sendRedirect(target);
    }

    private static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }
}
