package Vaultproject.Vaultapp.Services;

import org.springframework.stereotype.Service;
import Vaultproject.Vaultapp.Model.User;


@Service
public class LoginRiskService {
      private static final int CAPTCHA_THRESHOLD = 3;

    public boolean isCaptchaRequired(User user) {
        return user.getFailedLoginAttempts() >= CAPTCHA_THRESHOLD;
    }
}
