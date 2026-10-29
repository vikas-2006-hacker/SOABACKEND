package com.klef.soa.sdp.security;

public class LoginResponse {

    private String token;
    private String tokenType;
    private String email;
    private String role;

    public LoginResponse(String token, String tokenType, String email, String role) {
        this.token = token;
        this.tokenType = tokenType;
        this.email = email;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
