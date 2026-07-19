package com.flowset.security.jwt;

import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.security.UserPrincipal;
import com.flowset.users.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseAccessToken(token);
            if (!"access".equals(claims.get("type", String.class))) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                userRepository.findActiveById(userId).ifPresent(user -> {
                    if (user.getStatus() == UserStatus.SUSPENDED) {
                        return;
                    }
                    UserPrincipal principal = new UserPrincipal(
                            user.getId(),
                            user.getEmail(),
                            user.getPasswordHash(),
                            user.getRole(),
                            user.getStatus(),
                            user.isEmailVerified());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
