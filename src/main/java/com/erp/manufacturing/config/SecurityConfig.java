package com.erp.manufacturing.config;

import com.erp.manufacturing.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final Environment env;

    public SecurityConfig(CustomUserDetailsService userDetailsService, Environment env) {
        this.userDetailsService = userDetailsService;
        this.env = env;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            String redirectUrl = "/";
            if (role.equals("ROLE_GENERAL_MANAGER")) {
                redirectUrl = "/gm";
            } else if (role.equals("ROLE_DESIGNER")) {
                redirectUrl = "/designer";
            } else if (role.equals("ROLE_PURCHASE_MANAGER")) {
                redirectUrl = "/purchase";
            } else if (role.equals("ROLE_PRODUCTION_MANAGER")) {
                redirectUrl = "/production";
            } else if (role.equals("ROLE_ACCOUNTS")) {
                redirectUrl = "/accounts";
            }
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isDev = Arrays.asList(env.getActiveProfiles()).contains("dev");

        http
            .sessionManagement(session -> session
                .sessionFixation().migrateSession()
            )
            .authorizeHttpRequests(authz -> {
                authz.requestMatchers("/css/**", "/js/**", "/images/**").permitAll();
                
                if (isDev) {
                    authz.requestMatchers(new AntPathRequestMatcher("/h2-console/**")).permitAll();
                }
                
                authz.requestMatchers("/gm/**").hasRole("GENERAL_MANAGER")
                     .requestMatchers("/designer/**").hasRole("DESIGNER")
                     .requestMatchers("/purchase/**").hasRole("PURCHASE_MANAGER")
                     .requestMatchers("/production/**").hasRole("PRODUCTION_MANAGER")
                     .requestMatchers("/accounts/**").hasRole("ACCOUNTS")
                     .anyRequest().authenticated();
            })
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        if (isDev) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }
}
