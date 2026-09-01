package com.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private Long dni;
    private String password;
}
