
package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.config.UserPrincipal;
import co.udea.crowdfunding.dto.ActiveCampaignReportDTO;
import co.udea.crowdfunding.dto.PeriodFundsReportDTO;
import co.udea.crowdfunding.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes", description = "Módulo de reportes y analítica de campañas (Sprint 3)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/active-campaigns")
    @Operation(summary = "HU-18: Visualizar reporte de campañas activas (creador o consolidado admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<List<ActiveCampaignReportDTO>> getActiveCampaignsReport(
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        List<ActiveCampaignReportDTO> report = reportService.getActiveCampaignsReport(principal.getId(), isAdmin);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/funds-by-period")
    @Operation(summary = "HU-19: Generar reporte de fondos recaudados en un periodo (creador o admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte de fondos obtenido exitosamente"),
            @ApiResponse(responseCode = "400", description = "La fecha final es anterior a la fecha inicial"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<PeriodFundsReportDTO> getFundsByPeriod(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Fecha inicial en formato ISO UTC (ej: 2026-11-01T00:00:00Z)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Fecha final en formato ISO UTC (ej: 2026-11-30T23:59:59Z)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        boolean isAdmin = "admin".equalsIgnoreCase(principal.getRole());
        PeriodFundsReportDTO report = reportService.getFundsByPeriod(startDate, endDate, principal.getId(), isAdmin);
        return ResponseEntity.ok(report);
    }
}
