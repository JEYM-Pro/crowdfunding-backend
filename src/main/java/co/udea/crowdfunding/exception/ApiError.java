package co.udea.crowdfunding.exception;

import java.util.Map;

public record ApiError(String message, Map<String, String> details) {
}