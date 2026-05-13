package com.example.authService.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private OAuth2SuccessHandler successHandler;

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/auth/password/forgot/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/auth/users").hasAnyAuthority("ADMIN", "STAFF")
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                )

                // VERY IMPORTANT
                .formLogin(form -> form.disable())

                    // Return JSON instead of HTML
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((req, res, e) -> {
                                res.setContentType("application/json");
                                res.setStatus(401);
                                res.getWriter().write("{\"error\": \"Unauthorized\"}");
                            })
                );

        //default authentication
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}