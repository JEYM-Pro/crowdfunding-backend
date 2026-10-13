package co.udea.crowdfunding.controller;

import co.udea.crowdfunding.dto.CategoryResponse;
import co.udea.crowdfunding.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categorías", description = "Categorías disponibles para clasificar campañas")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "Listar categorías (para el selector del formulario de campaña)")
    public ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList());
    }
}
