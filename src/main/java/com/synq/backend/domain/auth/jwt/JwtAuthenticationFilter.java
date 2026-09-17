package com.synq.backend.domain.auth.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtProvider jwtProvider;
	private final ActiveUserChecker activeUserChecker;

	public JwtAuthenticationFilter(JwtProvider jwtProvider, ActiveUserChecker activeUserChecker) {
		this.jwtProvider = jwtProvider;
		this.activeUserChecker = activeUserChecker;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		BearerTokenExtractor.extract(request.getHeader("Authorization")).ifPresent(accessToken -> {
			try {
				Long userId = jwtProvider.parseUserId(accessToken);
				if (!activeUserChecker.isActive(userId)) {
					throw new JwtException("활성 사용자가 아닙니다.");
				}
				UserAuthDto principal = new UserAuthDto(userId);
				Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
				((UsernamePasswordAuthenticationToken) authentication)
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (JwtException | IllegalArgumentException e) {
				log.debug("access token 인증 실패: {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
				SecurityContextHolder.clearContext();
			}
		});
		filterChain.doFilter(request, response);
	}
}
