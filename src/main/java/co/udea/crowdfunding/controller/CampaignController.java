package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.CampaignResponse;
import co.udea.crowdfunding.dto.ContributionConfirmationRequest;
import co.udea.crowdfunding.dto.ContributionConfirmationResponse;
import co.udea.crowdfunding.dto.ContributionRequest;
import co.udea.crowdfunding.dto.CreateCampaignRequest;
import co.udea.crowdfunding.dto.UpdateCampaignRequest;
import co.udea.crowdfunding.dto.UpdateCampaignStatusRequest;
import co.udea.crowdfunding.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@Tag(name = "Campañas", description = "Publicación, edición y consulta de campañas")
@SecurityRequirement(name = "bearerAuth")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    @Operation(summary = "HU-06: Publicar una campaña (queda activa y visible en el listado público)")
    public ResponseEntity<CampaignResponse> publish(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateCampaignRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(campaignService.create(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "HU-07: Editar una campaña activa (solo su creador)")
    public ResponseEntity<CampaignResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest request) {

        return ResponseEntity.ok(campaignService.update(principal.getId(), id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver el detalle de una campaña")
    public ResponseEntity<CampaignResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "Listar campañas administradas por el usuario autenticado")
    public ResponseEntity<List<CampaignResponse>> listManaged(
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean admin = "admin".equalsIgnoreCase(principal.getRole());
        return ResponseEntity.ok(campaignService.listManaged(principal.getId(), admin));
    }

    @GetMapping
    @Operation(summary = "Listado público de campañas activas (con filtros)")
    public ResponseEntity<List<CampaignResponse>> listActive(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) UUID categoria,
            @RequestParam(required = false) String orden) {
        
        List<CampaignResponse> campaigns = campaignService.exploreActiveCampaigns(nombre, categoria, orden);
        return ResponseEntity.ok(campaigns);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CampaignResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateCampaignStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        CampaignResponse response = campaignService.updateCampaignStatus(id, request.getStatus(), principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/contributions")
    @Operation(summary = "HU-10: Realizar un aporte a una campaña activa (recompensa opcional, respuesta de texto)")
    public ResponseEntity<String> makeContribution(
            @PathVariable UUID id,
            @RequestBody ContributionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        campaignService.makeContributionWithConfirmation(
                id, request.getAmount(), request.getRewardId(), principal.getId());
        return ResponseEntity.ok("Aporte realizado exitosamente.");
    }

    @PostMapping("/{id}/contributions/confirm")
    @Operation(summary = "HU-11/HU-12: Aportar con recompensa opcional y recibir el comprobante de la contribución")
    public ResponseEntity<ContributionConfirmationResponse> confirmContribution(
            @PathVariable UUID id,
            @Valid @RequestBody ContributionConfirmationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                campaignService.makeContributionWithConfirmation(
                        id, request.amount(), request.rewardId(), principal.getId()));
    }

}
