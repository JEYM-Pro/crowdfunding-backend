package co.udea.crowdfunding.dto;

import java.util.UUID;

public record RegisterResponse(UUID id, String name, String email, String message) {
}