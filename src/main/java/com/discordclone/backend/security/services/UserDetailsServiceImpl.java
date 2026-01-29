package com.discordclone.backend.security.services;

import com.discordclone.backend.entity.jpa.User;
import com.discordclone.backend.exception.AccountNotActiveException;
import com.discordclone.backend.exception.AccountNotVerifiedException;
import com.discordclone.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // Kiểm tra case-sensitive
        if (!user.getUserName().equals(username)) {
            throw new UsernameNotFoundException("User Not Found with username: " + username);
        }

        // Kiểm tra email đã được xác thực chưa
        if (user.getIsEmailVerified() == null || !user.getIsEmailVerified()) {
            throw new AccountNotVerifiedException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email để xác thực tài khoản.");
        }

        // Kiểm tra tài khoản có đang active không
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new AccountNotActiveException("Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        return UserDetailsImpl.build(user);
    }
}
