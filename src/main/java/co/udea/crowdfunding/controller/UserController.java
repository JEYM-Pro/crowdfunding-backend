package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.RechargeRequest;
import co.udea.crowdfunding.dto.RechargeResponse;
import co.udea.crowdfunding.dto.UpdateProfileRequest;
import co.udea.crowdfunding.dto.UserResponse;
import co.udea.crowdfunding.entity.User;
import co.udea.crowdfunding.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        User updated = userService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(toResponse(updated));
    }

    @PostMapping("/recharge")
    public ResponseEntity<RechargeResponse> recharge(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RechargeRequest request) {

        User updated = userService.recharge(principal.getId(), request.amount());
        return ResponseEntity.ok(new RechargeResponse(
                updated.getId(),
                updated.getBalance(),
                "Recarga realizada exitosamente"
        ));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getHistoric()
        );
    }
}
