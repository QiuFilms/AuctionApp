package com.qiu.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@RestController
public class TokenBridgeController {

    @GetMapping("/get-token")
    public Map<String, String> getToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) { // nazwa z JwtAuthenticationFilter
                    return Collections.singletonMap("token", cookie.getValue());
                }
            }
        }
        return Collections.singletonMap("token", "");
    }
}