package com.jlpp.modules.auth.model;

import com.jlpp.common.constants.RoleEnum;
import com.jlpp.modules.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OidcUser {

    private final OidcUser delegateOidc;
    private final OAuth2User delegateOAuth2;
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(OidcUser delegateOidc, User user) {
        this.delegateOidc = delegateOidc;
        this.delegateOAuth2 = delegateOidc;
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public CustomOAuth2User(OAuth2User delegateOAuth2, User user) {
        this.delegateOidc = (delegateOAuth2 instanceof OidcUser oidc) ? oidc : null;
        this.delegateOAuth2 = delegateOAuth2;
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegateOidc != null ? delegateOidc.getClaims() : Map.of();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegateOidc != null ? delegateOidc.getUserInfo() : null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegateOidc != null ? delegateOidc.getIdToken() : null;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegateOAuth2 != null ? delegateOAuth2.getAttributes() : Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        // Return email to align with principal.getName() throughout JEPS
        return user.getEmail();
    }

    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getFullName() {
        return user.getFullName() != null ? user.getFullName() : user.getEmail();
    }

    public RoleEnum getRole() {
        return user.getRole();
    }

    public boolean isProfileComplete() {
        return user.getPhone() != null && !user.getPhone().isBlank();
    }
}
