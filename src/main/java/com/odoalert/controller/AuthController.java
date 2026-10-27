package com.odoalert.controller;
import org.springframework.security.core.AuthenticationException;
import com.odoalert.dto.LoginRequest;
import com.odoalert.dto.SignupRequest;
import com.odoalert.model.AppUser;
import com.odoalert.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepository;

    public AuthController(UserRepository users, PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          SecurityContextRepository contextRepository) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.contextRepository = contextRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "An account with this email already exists."));
        }
        AppUser user = users.save(new AppUser(
            request.name().trim(), email, encoder.encode(request.password())
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "message", "Account created. Please log in.",
            "name", user.getName(), "email", user.getEmail()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email().trim().toLowerCase(), request.password()
                )
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // Persist authentication using Spring Security's configured session repository.
            contextRepository.saveContext(context, httpRequest, httpResponse);
            var user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
            return ResponseEntity.ok(Map.of(
                "message", "Login successful", "name", user.getName(), "email", user.getEmail()
            ));
        } catch (org.springframework.security.core.AuthenticationException ex)  {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid email or password."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not logged in"));
        }
        var user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of("name", user.getName(), "email", user.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
