package com.example.lab.module.auth.presentation;

import com.example.lab.module.auth.usecase.AuthenticationResult;

public record AuthenticationResponse(long accountId) {

    public static AuthenticationResponse from(AuthenticationResult result) {
        long accountId = result.accountId();
        return new AuthenticationResponse(accountId);
    }
}
