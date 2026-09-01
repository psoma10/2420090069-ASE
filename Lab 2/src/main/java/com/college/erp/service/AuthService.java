package com.college.erp.service;

import com.college.erp.dto.LoginRequest;
import com.college.erp.dto.LoginResponse;
import com.college.erp.entity.User;
import com.college.erp.exception.NotFoundException;
import com.college.erp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Iteration 1 - session based authentication (FR-01, US-01).
 *
 * Form login is disabled in {@code SecurityConfig} because the frontend is a
 * JavaScript client, so the credentials are authenticated here explicitly and the
 * resulting security context is saved into the HTTP session.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    /**
     * Authenticates the credentials and starts a session.
     *
     * @throws org.springframework.security.core.AuthenticationException when the credentials are wrong
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest,
                               HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, httpRequest, httpResponse);

        return describe(authentication.getName());
    }

    /** Ends the session and clears the security context. */
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /** Returns the profile of the currently authenticated account. */
    public LoginResponse describe(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("No account found for " + username));
        return new LoginResponse(user.getUsername(), user.getFullName(), user.getRole().name());
    }
}
