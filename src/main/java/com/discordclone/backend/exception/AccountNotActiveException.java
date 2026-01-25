package com.discordclone.backend.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountNotActiveException extends AuthenticationException {
    public AccountNotActiveException(String message) {
        super(message);
    }
}
