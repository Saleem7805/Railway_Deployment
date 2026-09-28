package com.proitbridge.lms.service;

import com.proitbridge.lms.domain.User;
import com.proitbridge.lms.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/** Bridges an external OIDC identity to an existing LMS account and keeps the LMS JWT/session rules. */
@Service
public class SsoAuthService {
    private final UserRepository users;
    private final SessionService sessions;
    private final AuthService auth;
    private final ActivityService activity;

    public SsoAuthService(UserRepository users, SessionService sessions, AuthService auth,
                          ActivityService activity) {
        this.users = users;
        this.sessions = sessions;
        this.auth = auth;
        this.activity = activity;
    }

    public Map<String, Object> signIn(String email, String deviceId, String deviceLabel,
                                      String ip, String userAgent) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Your SSO provider did not return an email address.");
        }
        User user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your SSO account is not provisioned in the LMS. Ask an administrator to create the account first."));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is switched off.");
        }
        if (user.isSuspended()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is suspended. Contact your administrator.");
        }

        user.setLastLoginAt(Instant.now());
        users.save(user);
        var session = sessions.open(user, deviceId, deviceLabel, ip, userAgent);
        activity.log(user.getId(), user.getEmail(), "SSO_LOGIN", "user", deviceLabel);
        return auth.sessionForSso(user, session.getId());
    }
}
