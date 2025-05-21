package com.example.demo.exceptions;

public class VerificationTokenExpiredException extends RuntimeException {
    public VerificationTokenExpiredException(String message){
        super(message);
    }
}
