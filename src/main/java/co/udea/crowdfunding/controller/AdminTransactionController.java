
package co.udea.crowdfunding.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.udea.crowdfunding.dto.TransactionDTO;
import co.udea.crowdfunding.service.TransactionQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Transacciones", description = "Auditoría general de transacciones financieras (HU-17)")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransactionController {

    private final TransactionQueryService transactionQueryService;

    public AdminTransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    @GetMapping
    @Operation(summary = "HU-17: Consultar el registro general de transacciones con filtros")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @Parameter(description = "Fecha inicial en formato ISO UTC (ej: 2026-11-01T00:00:00Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,

            @Parameter(description = "Fecha final en formato ISO UTC (ej: 2026-11-30T23:59:59Z)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,

            @Parameter(description = "Tipo de transacción (ej: recharge, contribution)")
            @RequestParam(required = false) String type,

            @Parameter(description = "ID del usuario (UUID) para filtrar")
            @RequestParam(required = false) UUID userId
    ) {
        List<TransactionDTO> response = transactionQueryService.getTransactions(startDate, endDate, type, userId);
        return ResponseEntity.ok(response);
    }
}
