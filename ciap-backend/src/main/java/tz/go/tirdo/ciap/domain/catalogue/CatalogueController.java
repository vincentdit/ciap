package tz.go.tirdo.ciap.domain.catalogue;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.catalogue.dto.CategoryResponse;
import tz.go.tirdo.ciap.domain.catalogue.dto.PriceVersionResponse;
import tz.go.tirdo.ciap.domain.catalogue.dto.SaveCategoryRequest;
import tz.go.tirdo.ciap.domain.catalogue.dto.SaveServiceRequest;
import tz.go.tirdo.ciap.domain.catalogue.dto.ServiceOfferResponse;

import java.util.List;

@RestController
@RequestMapping("/consultancy-catalogue")
public class CatalogueController {

    private final CatalogueService catalogueService;

    public CatalogueController(CatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> categories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.listCategories(false)));
    }

    @GetMapping("/categories/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> manageCategories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.listCategories(true)));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody SaveCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Category saved", catalogueService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id, @Valid @RequestBody SaveCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Category saved", catalogueService.updateCategory(id, request)));
    }

    @GetMapping("/categories/{id}/services")
    public ResponseEntity<ApiResponse<List<ServiceOfferResponse>>> services(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.listServices(id, false)));
    }

    @GetMapping("/categories/{id}/services/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ServiceOfferResponse>>> manageServices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.listServices(id, true)));
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ApiResponse<ServiceOfferResponse>> service(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.getService(id)));
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceOfferResponse>> createService(@Valid @RequestBody SaveServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Service saved", catalogueService.createService(request)));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceOfferResponse>> updateService(
            @PathVariable Long id, @Valid @RequestBody SaveServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Service saved", catalogueService.updateService(id, request)));
    }

    @GetMapping("/services/{id}/prices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PriceVersionResponse>>> prices(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogueService.priceHistory(id)));
    }
}
