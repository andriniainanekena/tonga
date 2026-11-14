package com.tonga.satsia.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  FilterRegistrationBean<DeviceKeyFilter> deviceFilterRegistration(DeviceKeyFilter filter) {
    FilterRegistrationBean<DeviceKeyFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(
      PasswordEncoder encoder,
      @Value("${tonga.admin.username}") String username,
      @Value("${tonga.admin.password}") String password) {
    return new InMemoryUserDetailsManager(
        User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build());
  }

  @Bean
  @Order(1)
  SecurityFilterChain deviceSecurity(HttpSecurity http, DeviceKeyFilter deviceKeyFilter)
      throws Exception {
    return http.securityMatcher("/api/rfid/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(HttpMethod.POST, "/api/rfid/scan")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(
            deviceKeyFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain webSecurity(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/",
                        "/login",
                        "/css/**",
                        "/js/**",
                        "/icons/**",
                        "/favicon.ico",
                        "/video/**",
                        "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form -> form.loginPage("/login").defaultSuccessUrl("/dashboard", true).permitAll())
        .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    policy ->
                        policy.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src"
                                + " 'self' data:; font-src 'self'; connect-src 'self'; object-src"
                                + " 'none'; base-uri 'self'; frame-ancestors 'none'; form-action"
                                + " 'self'")))
        .build();
  }
}
