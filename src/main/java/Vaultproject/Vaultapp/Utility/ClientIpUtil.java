package Vaultproject.Vaultapp.Utility;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

// getting client ip
@Component
public class ClientIpUtil {

    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}

