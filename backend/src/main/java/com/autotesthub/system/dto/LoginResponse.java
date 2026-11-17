package com.autotesthub.system.dto;

public class LoginResponse {

    private Long id;
    private String username;
    private String nickname;
    private String token;
    private String tokenType;

    public LoginResponse(
            Long id,
            String username,
            String nickname,
            String token) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.token = token;
        this.tokenType = "Bearer";
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }
}