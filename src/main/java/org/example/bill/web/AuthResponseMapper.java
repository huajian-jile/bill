package org.example.bill.web;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.security.JwtService;
import org.example.bill.service.UserPhoneService;
import org.example.bill.web.dto.LoginResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthResponseMapper {

    private final JwtService jwtService;
    private final UserPhoneService userPhoneService;

    public LoginResponse toLoginResponse(AppUser u) {
        String token = jwtService.createToken(u);
        List<String> authorities = new ArrayList<>();
        u.getRoles()
                .forEach(
                        r -> {
                            authorities.add("ROLE_" + r.getCode());
                            r.getPermissions()
                                    .forEach(p -> authorities.add("PERM_" + p.getCode()));
                        });
        List<String> phones = userPhoneService.listMobiles(u.getId());
        return new LoginResponse(token, u.getUsername(), authorities, phones);
    }
}
