package com.stockflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String OPERATIONS = "OPERATIONS_MANAGER";
    private static final String STORE = "STORE_SUPERVISOR";
    private static final String FINANCE = "FINANCE_EXECUTIVE";
    private static final String CUSTOMER_SERVICE = "CUSTOMER_SERVICE_COORDINATOR";
    private static final String IT = "IT_SYSTEMS_OFFICER";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserAccountRepository userAccountRepository) {
        return username -> {
            UserAccount account = userAccountRepository.findByUsernameIgnoreCase(username)
                    .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                            "Invalid username or password"));

            return User.withUsername(account.getUsername())
                    .password(account.getPasswordHash())
                    .roles(account.getRole().name())
                    .disabled(!account.isActive())
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/login", "/error",
                                "/css/**", "/js/**", "/images/**", "/favicon.ico")
                        .permitAll()
                        .requestMatchers("/user-accounts/**").hasRole(IT)
                        .requestMatchers("/reference-data/**")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers("/dashboard", "/access-denied").authenticated()

                        .requestMatchers(HttpMethod.GET, "/products/new", "/products/*/edit")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers(HttpMethod.GET, "/products")
                        .hasAnyRole(OPERATIONS, STORE, FINANCE, CUSTOMER_SERVICE)
                        .requestMatchers(HttpMethod.GET,
                                "/suppliers/new", "/suppliers/*/edit",
                                "/purchase-orders/new", "/purchase-orders/*/edit")
                        .hasAnyRole(OPERATIONS, FINANCE)
                        .requestMatchers(HttpMethod.GET, "/suppliers/**", "/purchase-orders/**")
                        .hasAnyRole(OPERATIONS, STORE, FINANCE)
                        .requestMatchers(HttpMethod.GET,
                                "/goods-receipts/new", "/goods-receipts/*/edit")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers(HttpMethod.GET, "/goods-receipts/**")
                        .hasAnyRole(OPERATIONS, STORE, FINANCE)
                        .requestMatchers(HttpMethod.GET,
                                "/inventory-control/stock", "/inventory-control/movements")
                        .hasAnyRole(OPERATIONS, STORE, FINANCE, CUSTOMER_SERVICE)
                        .requestMatchers(HttpMethod.GET,
                                "/recipients/new", "/recipients/*/edit",
                                "/stock-issues/new", "/stock-issues/*/edit",
                                "/stock-returns/new", "/stock-returns/*/edit")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers(HttpMethod.GET,
                                "/recipients/**", "/stock-issues/**", "/stock-returns/**")
                        .hasAnyRole(OPERATIONS, STORE, CUSTOMER_SERVICE)

                        .requestMatchers(HttpMethod.POST, "/stock-transfers/*/approve")
                        .hasRole(OPERATIONS)
                        .requestMatchers("/products/**").hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers("/suppliers/**", "/purchase-orders/**")
                        .hasAnyRole(OPERATIONS, FINANCE)
                        .requestMatchers("/goods-receipts/**")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers("/warehouses/**", "/stock-transfers/**")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers("/recipients/**", "/stock-issues/**", "/stock-returns/**")
                        .hasAnyRole(OPERATIONS, STORE)
                        .requestMatchers(
                                "/inventory-control/**", "/reorder-rules/**",
                                "/stock-counts/**", "/stock-adjustments/**")
                        .hasRole(OPERATIONS)
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied"));

        return http.build();
    }
}
