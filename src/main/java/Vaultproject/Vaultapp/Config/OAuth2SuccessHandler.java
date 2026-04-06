package Vaultproject.Vaultapp.Config;


import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import Vaultproject.Vaultapp.Model.RefreshToken;
import Vaultproject.Vaultapp.Model.User;
import Vaultproject.Vaultapp.Repository.UserInfoRepository;
import Vaultproject.Vaultapp.Services.JwtService;
import Vaultproject.Vaultapp.Services.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserInfoRepository userRepo;
    public OAuth2SuccessHandler(
            JwtService jwtService,
            RefreshTokenService refreshTokenService,UserInfoRepository userRepo) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepo = userRepo;
    }

    @Override
public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication) throws IOException {

    OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
    String email = oauthUser.getAttribute("email");

    User user = userRepo.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found"));

    String accessToken = jwtService.generateAccessToken(user.getEmail());
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    String redirectUrl =
            "http://localhost:3000/oauth-success"
            + "?accessToken=" + accessToken
            + "&refreshToken=" + refreshToken.getToken();

    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
}
}