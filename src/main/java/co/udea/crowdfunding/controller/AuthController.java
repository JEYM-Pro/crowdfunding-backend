package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.dto.LoginRequest;
import co.udea.crowdfunding.dto.LoginResponse;
import co.udea.crowdfunding.dto.RegisterRequest;
import co.udea.crowdfunding.dto.RegisterResponse;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.service.AuthService;
import co.udea.crowdfunding.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        RegisterResponse body = new RegisterResponse(
                user.getId(), user.getName(), user.getEmail(), "Cuenta creada correctamente");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
