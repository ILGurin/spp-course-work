package by.example.cloud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody AuthenticationRequest authenticationRequest) {
        final String jwtAccessToken = authenticationRequest.username();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Authorization", "Bearer " + jwtAccessToken)
                .build();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody AuthenticationRequest authenticationRequest) {
        return null;
    }
}
