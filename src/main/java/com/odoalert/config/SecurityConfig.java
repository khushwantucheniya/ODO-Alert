package com.odoalert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        // CSRF is disabled for this local, same-origin teaching demo. Re-enable it before deployment.
        http.csrf(csrf -> csrf.disable());
        // Use the same HTTP-session security context repository as the login controller.
        http.securityContext(context -> context.securityContextRepository(securityContextRepository));
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/index.html", "/style.css", "/app.js", "/favicon.ico",
                "/api/auth/signup", "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
        );
        http.logout(logout -> logout.disable());
        return http.build();
    }

    @Bean PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
