package com.rag.knowledgehub.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String role;
    private boolean enabled;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String normalized = RoleConstants.normalize(role);
        if (RoleConstants.SUPER_ADMIN.equals(normalized)) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_" + RoleConstants.SUPER_ADMIN),
                    new SimpleGrantedAuthority("ROLE_" + RoleConstants.DOC_ADMIN),
                    new SimpleGrantedAuthority("ROLE_" + RoleConstants.USER)
            );
        }
        if (RoleConstants.DOC_ADMIN.equals(normalized)) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_" + RoleConstants.DOC_ADMIN),
                    new SimpleGrantedAuthority("ROLE_" + RoleConstants.USER)
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + RoleConstants.USER));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
