package com.autotesthub.system;

import com.autotesthub.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthTestController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            HttpServletRequest request) {

        Object userId = request.getAttribute("userId");
        Object username = request.getAttribute("username");

        if (userId == null || username == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(
                            401,
                            "请先登录",
                            null
                    ));
        }

        Map<String, Object> userInfo = Map.of(
                "userId", userId,
                "username", username
        );

        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }
}