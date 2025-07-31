package com.himusharier.auth.exception;

public class JwtUserAuthenticationException extends RuntimeException {
    public JwtUserAuthenticationException(String message) {
        super(message);
    }
}
