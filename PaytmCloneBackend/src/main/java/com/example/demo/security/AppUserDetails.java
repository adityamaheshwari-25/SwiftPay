package com.example.demo.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.demo.entity.AppUser;

import lombok.Getter;
/**
 * Because Spring Security doesn’t understand your AppUser entity directly, that's why we have made this
 * custom AppUserDetails that implements UserDetails. UserDetails is from the Spring boot, so we wrap it.
 * Spring requires roles to be prefixed with:ROLE_
 * Like: ADMIN → ROLE_ADMIN, USER → ROLE_USER
 * 
 * I created a custom UserDetails implementation to adapt my AppUser entity to Spring Security’s internal 
 * authentication model. It provides username, password, authorities, and account status.
 * 
 * Authorities are the permissions/roles granted to the user in Spring Security.
 * Spring Security doesn’t directly use your enum Role.ADMIN.
 * It uses GrantedAuthority objects like:"ROLE_ADMIN", "ROLE_USER"
 */
@Getter
public class AppUserDetails implements UserDetails{
	private final AppUser user;
	
	public AppUserDetails(AppUser user) {
		this.user = user;
	}

	@Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() { return user.getPassword(); }

    @Override
    public String getUsername() { return user.getEmail(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.isActive(); } // here, that active is used.
}
