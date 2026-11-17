package com.autotesthub.system;

import com.autotesthub.common.ApiResponse;
import com.autotesthub.system.dto.LoginRequest;
import com.autotesthub.system.dto.LoginResponse;
import com.autotesthub.system.entity.SysUser;
import com.autotesthub.system.security.JwtService;
import com.autotesthub.system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserService userService;
    private final JwtService jwtService;

    public LoginController(
            UserService userService,
            JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        SysUser user = userService.login(request);

        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername()
        );

        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                token
        );

        return ApiResponse.ok(response);
    }
}