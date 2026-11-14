package com.tonga.satsia.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class DeviceKeyFilter extends OncePerRequestFilter {
  private final byte[] expectedKey;

  public DeviceKeyFilter(@Value("${tonga.device.key}") String expectedKey) {
    if (expectedKey.isBlank() || expectedKey.length() < 16) {
      throw new IllegalArgumentException("Device key must contain at least 16 characters");
    }
    this.expectedKey = expectedKey.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/rfid/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String key = request.getHeader("X-Device-Key");
    byte[] providedKey = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(expectedKey, providedKey)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Device access denied");
      return;
    }
    chain.doFilter(request, response);
  }
}
