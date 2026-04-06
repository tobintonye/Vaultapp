package Vaultproject.Vaultapp.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleRecaptchaService implements CaptchaService {

    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void verify(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new RuntimeException("CAPTCHA token is required");
        }

        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", captchaToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, request, Map.class);
            
            if (response.getBody() == null) {
                throw new RuntimeException("CAPTCHA verification failed: no response");
            }
            
            Boolean success = (Boolean) response.getBody().get("success");
            
            if (success == null || !success) {
                // Get error codes if available
                Object errorCodes = response.getBody().get("error-codes");
                throw new RuntimeException("CAPTCHA verification failed: " + 
                    (errorCodes != null ? errorCodes.toString() : "unknown error"));
            }
            
        } catch (Exception e) {
            throw new RuntimeException("CAPTCHA verification error: " + e.getMessage());
        }
    }
}