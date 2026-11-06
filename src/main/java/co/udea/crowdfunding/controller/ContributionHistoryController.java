package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.ContributionHistorySummaryResponse;
import co.udea.crowdfunding.service.ContributionHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contributions")
@Tag(name = "Historial de aportes",
        description = "Consulta del historial de aportes del usuario")
@SecurityRequirement(name = "bearerAuth")
public class ContributionHistoryController {

    private final ContributionHistoryService historyService;

    public ContributionHistoryController(
            ContributionHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/history")
    @Operation(summary = "HU-16: Consultar mi historial de aportes")
    public ResponseEntity<ContributionHistorySummaryResponse> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                historyService.getHistory(principal.getId()));
    }
}
