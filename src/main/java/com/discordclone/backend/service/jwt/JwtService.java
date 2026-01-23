package com.discordclone.backend.service.jwt;

import com.discordclone.backend.entity.jpa.User;

public interface JwtService {
    String generateToken(User user);
    String getUserNameFromToken(String token);
    boolean validateToken(String token);
}
