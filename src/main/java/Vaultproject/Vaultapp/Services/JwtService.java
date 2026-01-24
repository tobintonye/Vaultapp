package Vaultproject.Vaultapp.Services;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

@Component
public class JwtService {
    @Value("${jwt.access.token}")
    private String jwtAccessTokenSecret;

    @Value("${jwt.refresh.token}")
    private String jwtRefreshTokenSecret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, accessExpiration, getAccessKey());
    }

    public String generateRefreshToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, refreshExpiration, getRefreshKey());
    }

    public long getAccessExpiration () {
        return accessExpiration;
    }

    public long getRefreshExpiration () {
        return refreshExpiration;
    }

    private String createToken(Map<String, Object> claims, String email, long expiration, Key key) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getAccessKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtAccessTokenSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getRefreshKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtRefreshTokenSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

     public String extractUsername(String token, boolean isRefreshToken) {
        return extractClaim(token, Claims::getSubject, isRefreshToken);
    }

    public Date extractExpiration(String token, boolean isRefreshToken) {
        return extractClaim(token, Claims::getExpiration, isRefreshToken );
    }
    
     public <T> T extractClaim(String token, Function<Claims, T> claimsResolver, boolean isRefreshToken) {
        final Claims claims = extractAllClaims(token, isRefreshToken);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, boolean isRefreshToken) {
   return Jwts.parserBuilder()
                .setSigningKey(isRefreshToken ? getRefreshKey() : getAccessKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token,  boolean isRefreshToken) {
        return extractExpiration(token, isRefreshToken).before(new Date());
    }

    public Boolean validateAccessToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token, false);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token, false));
    }

    public boolean validateRefreshToken(String token) {
        return !isTokenExpired(token, true);
    }
}