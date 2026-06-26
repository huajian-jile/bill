package org.example.bill.security;

import lombok.RequiredArgsConstructor;
import org.example.bill.repo.AppUserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var u =
                appUserRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException(username));
        return User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .disabled(!u.isEnabled())
                .build();
    }
}
