package com.proitbridge.lms.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/sso")
public class SsoController {
    private final boolean enabled;

    public SsoController(@Value("${lms.sso.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    /** Starts the generic OIDC login while keeping the browser device identity stable for the callback. */
    @GetMapping("/start")
    public void start(HttpServletResponse response) throws Exception {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SSO is not enabled.");
        }
        Cookie device = new Cookie("pib_sso_device", UUID.randomUUID().toString());
        device.setPath("/");
        device.setHttpOnly(true);
        device.setMaxAge(300);
        response.addCookie(device);
        response.sendRedirect("/oauth2/authorization/lms");
    }
}
