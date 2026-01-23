package com.discordclone.backend.service.user;

import com.discordclone.backend.entity.jpa.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    void updatePassword(User user, String newPassword);
}
