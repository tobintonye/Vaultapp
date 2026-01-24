package Vaultproject.Vaultapp.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterDto {
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    public RegisterDto(String email, String firstName, String lastName, String password) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }
}
