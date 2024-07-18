package com.github.travelbuddy.users.jwt;

import com.github.travelbuddy.users.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtill jwtUtill;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        System.out.println(email + " " + password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

        //authenticationManager를 사용하여 인증을 시도 -> UserDetailsService(사용자 정보 로드)
        return authenticationManager.authenticate(authToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication)  {

        CustomUserDetails customUserDetails =(CustomUserDetails) authentication.getPrincipal();

        Integer userId = customUserDetails.getUserId();

        String token = jwtUtill.createJwt(userId,5*60*60*1000L);

        response.addHeader("Authorization", "Bearer " + token);

        System.out.println("successful authentication");
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed)  {

        System.out.println("unsuccessful authentication");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }


}
