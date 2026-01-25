package com.discordclone.backend.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountNotVerifiedException extends AuthenticationException {
    public AccountNotVerifiedException(String message) {
        super(message);
    }
}
