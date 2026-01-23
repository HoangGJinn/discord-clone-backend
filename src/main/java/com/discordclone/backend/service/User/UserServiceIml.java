package com.discordclone.backend.service.User;

import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceIml implements UserService{

    private final UserRepository userRepository;

    @Override
    public Optional<User> findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }
}
