package Vaultproject.Vaultapp.Services;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.RefreshTokenRepository;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
     }
     
    // create and save refresh token in db
    public RefreshToken createRefreshToken(User user) {
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
        RefreshToken token = new RefreshToken();
        token.setToken(refreshToken);
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000));
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshToken> findByToken(String token) {
         return refreshTokenRepository.findByToken(token);
     }
     
     // Validate token from DB
     public boolean validateRefreshToken(String refreshToken) {
        Optional<RefreshToken> tokenOtp = refreshTokenRepository.findByToken(refreshToken);
        if(tokenOtp.isEmpty()) return false;
        if(tokenOtp.get().getExpiryDate().isBefore(Instant.now())) return false;
        
        return jwtService.validateRefreshToken(refreshToken);
     }

     public void deleteToken(User user) {
            refreshTokenRepository.deleteByUser(user);
    }

    public void deleteExpiredTokens() {
           refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
        }

}
