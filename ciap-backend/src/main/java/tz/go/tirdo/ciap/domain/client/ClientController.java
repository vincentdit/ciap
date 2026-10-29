package tz.go.tirdo.ciap.domain.client;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.client.dto.ClientRegistrationRequest;
import tz.go.tirdo.ciap.domain.client.dto.ClientResponse;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /** Public endpoint (permitted in SecurityConfig). */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ClientResponse>> register(@Valid @RequestBody ClientRegistrationRequest req) {
        Client c = clientService.register(req);
        return ResponseEntity.ok(ApiResponse.ok("Client registered", ClientResponse.from(c)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ApiResponse<ClientResponse>> me(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(ClientResponse.from(clientService.getByUserEmail(auth.getName()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FINANCE','EXPERT','HOD')")
    public ResponseEntity<ApiResponse<ClientResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ClientResponse.from(clientService.getById(id))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','FINANCE','HOD')")
    public ResponseEntity<ApiResponse<PageResponse<ClientResponse>>> list(Pageable pageable) {
        var page = clientService.list(pageable).map(ClientResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }
}
