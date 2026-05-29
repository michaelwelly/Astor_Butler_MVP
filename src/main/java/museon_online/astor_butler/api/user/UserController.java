package museon_online.astor_butler.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User API", description = "User profile, roles and lookup boundary")
public class UserController {

    @PostMapping
    @Operation(summary = "Create or update user profile")
    public ResponseEntity<UserResponse> upsert(@RequestBody UserUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new UserResponse(
                UUID.randomUUID(),
                request.telegramId(),
                request.phone(),
                request.displayName(),
                "GUEST",
                Instant.now()
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace user profile")
    public ResponseEntity<UserResponse> replace(@PathVariable UUID id, @RequestBody UserUpsertRequest request) {
        return ResponseEntity.ok(new UserResponse(id, request.telegramId(), request.phone(), request.displayName(), "GUEST", Instant.now()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user card")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(new UserResponse(id, null, null, "stub-user", "GUEST", Instant.now()));
    }

    @GetMapping
    @Operation(summary = "Search users")
    public ResponseEntity<UserSearchResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new UserSearchResponse(query, role, page, size, List.of()));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable UUID id, @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(new UserResponse(id, null, null, "stub-user", request.role(), Instant.now()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete or block user profile")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record UserUpsertRequest(Long telegramId, String phone, String displayName) {
    }

    public record ChangeRoleRequest(String role) {
    }

    public record UserResponse(UUID id, Long telegramId, String phone, String displayName, String role, Instant updatedAt) {
    }

    public record UserSearchResponse(String query, String role, int page, int size, List<UserResponse> items) {
    }
}
