package com.lec.spring.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.time.LocalDateTime;

public class CustomLoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    public CustomLoginSuccessHandler(String defaultTargetUrl) {
        // SavedRequestAwareAuthenticationSuccessHandler#setDefaultTargetUrl()
        // 로그인후 특별히 redirect 할 url 이 없는경우 기본적으로 redirect 할 url
        this.setDefaultTargetUrl(defaultTargetUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {

        System.out.println("### 로그인 성공: onAuthenticationSuccess() 호출 ###");
        System.out.println("접속 ip: " + getClientIp(request));
        PrincipalDetails userDetails = (PrincipalDetails) authentication.getPrincipal();
        System.out.println("username: " + userDetails.getUsername());
        System.out.println("password: " + userDetails.getPassword());
        System.out.println("authorities" + authentication.getAuthorities().stream().toList());

        // 로그인 시간을 세션에 저장
        LocalDateTime loginTime = LocalDateTime.now();
        System.out.println("로그인 시간: " + loginTime);
        request.getSession().setAttribute("loginTime", loginTime);

        // 로그인 직전의 url 로 redirect
        super.onAuthenticationSuccess(request, response, authentication);
    }

    // request 를 한 client ip 가져오기
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
