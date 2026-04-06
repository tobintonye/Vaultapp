package Vaultproject.Vaultapp.Config;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import Vaultproject.Vaultapp.Model.AuthProvider;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserInfoRepository userRepo;

    public CustomOAuth2UserService(UserInfoRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = new DefaultOAuth2UserService().loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        String providerId = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        Boolean emailVerified = oauthUser.getAttribute("email_verified");

        if ( email == null || providerId == null) {
            throw new OAuth2AuthenticationException("Invalid OAuth response");
        }

        if ( emailVerified != null && !emailVerified) {
            throw new OAuth2AuthenticationException("Email not verified");
        }
        
        User user; 
        
        // find by providerId 
        Optional<User> byProvider = userRepo.findByProviderAndProviderId(provider, providerId);

        if(byProvider.isPresent()) {
            user = byProvider.get();
        } else {
            // email conflict
            Optional<User> userEmail = userRepo.findByEmail(email);

            if(userEmail.isPresent()) {
                User isExisting = userEmail.get();

                if(isExisting.getProvider() != provider) {
                    throw new OAuth2AuthenticationException("Account registered using " + isExisting.getProvider());
                }
                // Link providerId
                isExisting.setProviderId(providerId);
                user = userRepo.save(isExisting);
            } else {
                // new auth user
                user = new User();
                user.setEmail(email);
                user.setFirstname(oauthUser.getAttribute("given_name"));
                user.setLastname(oauthUser.getAttribute("family_name"));
                user.setImageUrl(oauthUser.getAttribute("picture"));
                user.setProvider(provider);
                user.setProviderId(providerId);
                user.setEnabled(true);
                user.setEmailVerified(true);
                
                user = userRepo.save(user);
            }
        }

        return new CustomOAuth2User(oauthUser, user);
    }
}