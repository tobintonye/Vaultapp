package Vaultproject.Vaultapp.Services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
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
import Vaultproject.Vaultapp.Model.PasswordResetToken;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Model.VerificationToken;
import Vaultproject.Vaultapp.Repository.PasswordResetRepository;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Repository.VerificationTokenRepository;
import Vaultproject.Vaultapp.exception.ExpiredVerificationTokenException;
import Vaultproject.Vaultapp.exception.InvalidVerificationTokenException;
import Vaultproject.Vaultapp.exception.UsedVerificationTokenException;

@Service
public class UserInfoService implements UserDetailsService {
    private final UserInfoRepository userInfoRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserInfoService(UserInfoRepository userInfoRepository, VerificationTokenRepository verificationTokenRepository, 
        PasswordResetRepository passwordResetRepository,
        @Lazy PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userInfoRepository = userInfoRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetRepository = passwordResetRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Fetch user from the database by email (username)
        Optional<User> userInfo = userInfoRepository.findByEmail(username);
        if(userInfo.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        } 

         // Convert User to UserDetails
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
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Set account as not enabled until email is verified
        user.setEnabled(false);
        user.setEmailVerified(false);
        User newUser = userInfoRepository.save(user);

        String rawToken =  UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);

        VerificationToken token = new VerificationToken();
        token.setTokenHash(hashedToken);
        token.setExpiryDate(LocalDateTime.now().plusHours(24));
        token.setUser(newUser);
        token.setUsed(false);
        verificationTokenRepository.save(token);

        // Send verification email
        emailService.sendVerificationEmail(newUser.getEmail(), rawToken);
        
        return "Registration successful! Please check your email to verify your account.";
    }

        @Transactional
        public void verifyEmail(String rawToken) {
             // Get all valid tokens for comparison
            List<VerificationToken> tokens = verificationTokenRepository
                .findAllByExpiryDateAfter(LocalDateTime.now());
            // Optional<VerificationToken> verificationTokenOpt = verificationTokenRepository.findByToken(token);

             VerificationToken verificationToken = null;
            for (VerificationToken token : tokens) {
                if (passwordEncoder.matches(rawToken, token.getTokenHash())) {
                    verificationToken = token;
                    break;
                }
            }
            

            if (verificationToken == null) {
                throw new InvalidVerificationTokenException();
            }
                    
            if (verificationToken.isUsed()) {
                throw new UsedVerificationTokenException();
            }

            if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new ExpiredVerificationTokenException();
            }

            // Activate user account
            User user = verificationToken.getUser();
            user.setEnabled(true);
            user.setEmailVerified(true);
            user.setVerifiedAt(LocalDateTime.now());
            userInfoRepository.save(user);

            verificationToken.setUsed(true);
            verificationTokenRepository.save(verificationToken);
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
              String rawToken = UUID.randomUUID().toString();
              String hashedToken = passwordEncoder.encode(rawToken);
            
            VerificationToken verificationToken = new VerificationToken();
            verificationToken.setUser(user);
            verificationToken.setTokenHash(hashedToken);
            verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
            verificationToken.setUsed(false);
            verificationTokenRepository.save(verificationToken);

            // Send verification email
            emailService.sendVerificationEmail(user.getEmail(), rawToken);

            return "A new verification email has been sent. Please check your inbox.";
        }
        
        public void checkAccountNotLocked(User user) {
            if(user.getLockUntil() != null && user.getLockUntil().isAfter(Instant.now())) {
                throw new RuntimeException("Account locked. Try again later.");
            }
        } 

        @Transactional
        public void handleFailedLogin(User user) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if(attempts >= 5) {
               user.setLockUntil(Instant.now().plusSeconds(15 * 60));
            }
            userInfoRepository.save(user);
        }

        @Transactional
        public void handleSuccessfulLogin(User user) {
            user.setFailedLoginAttempts(0);
            user.setLockUntil(null);
            userInfoRepository.save(user);
        }
        
    // request password reset email
    @Transactional
    public String requestPasswordReset(String email) {
        Optional<User> userOpt = userInfoRepository.findByEmail(email);
        
        String successMessage = "If an account with that email exists, a password reset link has been sent.";
        
        if (userOpt.isEmpty()) {
            return successMessage;
        }
        
        User user = userOpt.get();
        
        // Delete any existing reset tokens for this user
        passwordResetRepository.findByUser(user).ifPresent(existingToken -> {
            passwordResetRepository.delete(existingToken);
        });
        
        // Generate new token
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);
        Instant expiryDate = Instant.now().plusSeconds(15 * 60);
        
        PasswordResetToken passwordResetToken = new PasswordResetToken();

        passwordResetToken.setTokenHash(hashedToken);
        passwordResetToken.setExpiryDate(expiryDate);
        passwordResetToken.setUser(user);
        passwordResetToken.setUsed(false);
        passwordResetRepository.save(passwordResetToken);
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        
        return successMessage;
    }

    public PasswordResetToken validatePasswordResetToken(String rawToken) {

    List<PasswordResetToken> tokens = passwordResetRepository.findAllValidTokens(
        Instant.now()
    );

    for (PasswordResetToken token : tokens) {

        if (passwordEncoder.matches(rawToken, token.getTokenHash())) {

            if (token.isUsed()) {
                throw new RuntimeException("Token already used");
            }

            return token; // ✅ valid token
        }
    }

    throw new RuntimeException("Invalid or expired token");
}

    @Transactional
    public String resetPassword(String rawToken, String newPassword) {

        PasswordResetToken token = validatePasswordResetToken(rawToken);
        User user = token.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));
        userInfoRepository.save(user);

        token.setUsed(true);
        passwordResetRepository.save(token);

        // invalidate any other tokens
        passwordResetRepository.deleteByUser(user);

        return "Password reset successful";
    }

    @Transactional     
    public String resendPasswordToken(String email) {
        Optional<User> userOpt = userInfoRepository.findByEmail(email);
        
        String successMessage = "If an account with that email exists, a password reset link has been sent.";

        if(userOpt.isEmpty()) {
            return successMessage;
        }

        User user = userOpt.get();
         
        passwordResetRepository.findByUser(user).ifPresent(existingToken -> {
            passwordResetRepository.delete(existingToken);
        });
        // Generate new token
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);
        Instant expiryDate = Instant.now().plusSeconds(15 * 60);
        
        PasswordResetToken passwordResetToken = new PasswordResetToken();

        passwordResetToken.setTokenHash(hashedToken);
        passwordResetToken.setExpiryDate(expiryDate);
        passwordResetToken.setUser(user);
        passwordResetToken.setUsed(false);
        passwordResetRepository.save(passwordResetToken);
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        
        return successMessage;
    }    
}