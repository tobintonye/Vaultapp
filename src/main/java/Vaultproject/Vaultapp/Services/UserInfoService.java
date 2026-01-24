package Vaultproject.Vaultapp.Services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import Vaultproject.Vaultapp.Config.UserInfoDetails;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Model.VerificationToken;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Repository.VerificationTokenRepository;

@Service
public class UserInfoService implements UserDetailsService {
    private final UserInfoRepository userInfoRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder encoder;
    private final EmailService emailService;

    public UserInfoService(UserInfoRepository userInfoRepository, VerificationTokenRepository verificationTokenRepository, 
        @Lazy PasswordEncoder encoder, EmailService emailService) {
        this.userInfoRepository = userInfoRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.encoder = encoder;
        this.emailService = emailService;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch user from the database by email (username)
        Optional<User> userInfo = userInfoRepository.findByEmail(username);
        if(userInfo.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        } 

         // Convert User to UserDetails (UserInfoDetails)
         User user = userInfo.get();

         // chech if email is activated
         if(!user.isEnabled()) {
            throw new UsernameNotFoundException("Email not verified. Please check your email.");
         }
         return new UserInfoDetails(user);  
    }

    @Transactional
    public String saveUser(User user) {
        // check if user already exists
        if(userInfoRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        user.setPassword(encoder.encode(user.getPassword()));
        // Set account as not enabled until email is verified
        user.setEnabled(false);
        user.setEmailVerified(false);
        User newUser = userInfoRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(newUser, token,  LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(newUser.getEmail(), token);
        
        return "Registration successful! Please check your email to verify your account.";

    }

        @Transactional
        public String verifyEmail(String token) {
            Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);
        
            if(verificationTokenOpt.isEmpty()) {
                return "Invalid verification token.";
            }

            VerificationToken verificationToken = verificationTokenOpt.get();
            if(verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                return "Verification token has expired.";
            }
            
            // check if token is already used
            if (verificationToken.isUsed()) {
                 return "This verification link has already been used";
            }
            
            // Activate user account
            User user = verificationToken.getUser();
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setVerifiedAt(LocalDateTime.now());
            userInfoRepository.save(user);

            verificationToken.setUsed(true);
            verificationTokenRepository.save(verificationToken);

            return "Email verified successfully! You can now log in.";
        }

        @Transactional
        public String resendVerificationEmail(String email) {
            Optional<User> userOpt = userInfoRepository.findByEmail(email);
            if(userOpt.isEmpty()) {
                return "No account found with the provided email.";
            }

            User user = userOpt.get();
            if(user.isEnabled()) {
                return "Account is already verified.";
            }
            
             verificationTokenRepository.findByUser(user).ifPresent(existingToken -> {
                verificationTokenRepository.delete(existingToken);
            });
                
            // Generate new token
            String token = UUID.randomUUID().toString();
            VerificationToken verificationToken = new VerificationToken(user, token, LocalDateTime.now().plusHours(24));
            verificationTokenRepository.save(verificationToken);

            // Send verification email
            emailService.sendVerificationEmail(user.getEmail(), token);

            return "A new verification email has been sent. Please check your inbox.";
        }
}