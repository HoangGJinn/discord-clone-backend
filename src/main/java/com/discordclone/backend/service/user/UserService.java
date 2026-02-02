package com.discordclone.backend.service.user;

import com.discordclone.backend.dto.request.RegisterRequest;
import com.discordclone.backend.dto.request.UpdateProfileRequest;
import com.discordclone.backend.entity.jpa.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUserName(String userName);

    Optional<User> findByEmail(String email);

    void updatePassword(User user, String newPassword);

    void registerUser(RegisterRequest request);

    void verifyAccount(String email, String otp);

    User updateProfile(String userName, UpdateProfileRequest request);
}
