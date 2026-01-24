package Vaultproject.Vaultapp.Controller;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vault-api-v1")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/welcome")
    public String vault() {
        return "Welcome to vault authentication and authorization";
    }

    
}
