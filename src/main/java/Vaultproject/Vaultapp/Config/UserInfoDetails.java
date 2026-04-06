package Vaultproject.Vaultapp.Config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import Vaultproject.Vaultapp.Model.User;

public class UserInfoDetails implements UserDetails, CredentialsContainer {
    private final User user;
    private final String username;
    private String password;
    private final List<GrantedAuthority> authorities;

    public UserInfoDetails(User user) {
        this.user = user;
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.authorities = List.of();
    }    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getUsername() {
        return username;
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
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled(); // Check if email is verified
    }

    public User getUser() {
        return user;
    }
    @Override
    public void eraseCredentials() {
        this.password = null; // Securely dereference the password field
    }
}


