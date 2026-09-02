package com.college.erp.controller;

import com.college.erp.dto.ApiResponse;
import com.college.erp.dto.LoginRequest;
import com.college.erp.dto.LoginResponse;
import com.college.erp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * Iteration 1 - login, logout and current-user endpoints (FR-01, US-01).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        try {
            LoginResponse body = authService.login(request, httpRequest, httpResponse);
            return ResponseEntity.ok(ApiResponse.ok(body));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failed("Invalid username or password"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failed("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.describe(principal.getName())));
    }
}
