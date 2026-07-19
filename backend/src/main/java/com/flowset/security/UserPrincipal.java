package com.flowset.security;

import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final RoleName role;
    private final UserStatus status;
    private final boolean emailVerified;

    public UserPrincipal(
            UUID id,
            String email,
            String passwordHash,
            RoleName role,
            UserStatus status,
            boolean emailVerified
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.emailVerified = emailVerified;
    }

    public UUID getId() {
        return id;
    }

    public RoleName getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != UserStatus.SUSPENDED;
    }
}
