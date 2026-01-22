package com.discordclone.backend.service.User;

import com.discordclone.backend.entity.jpa.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUserName(String userName);
}
