package Vaultproject.Vaultapp.Model;
import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "users", uniqueConstraints={
    @UniqueConstraint(columnNames={"provider", "providerId"}),
    @UniqueConstraint(columnNames={"email"})
})
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstname;

    @Column(name = "last_name", nullable = false)
    private String lastname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false)
    private boolean emailVerified = false;

     @Column(nullable = false)
     private boolean enabled = false; // Account activation status

     private LocalDateTime verifiedAt;

     @Column(nullable = false)
     private int failedLoginAttempts = 0;

     @Column
     private Instant lockUntil;

     // OAuth2
     @Enumerated(EnumType.STRING)
     @Column(nullable = false )
     private AuthProvider provider = AuthProvider.LOCAL;

    @Column
    private String providerId; 

    @Column
    private String imageUrl;
}