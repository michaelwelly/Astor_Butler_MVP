package museon_online.astor_butler.api.content;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import museon_online.astor_butler.api.common.ApiCommandResponse;
import museon_online.astor_butler.api.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts/Content API", description = "Posts, afisha, promo blocks and SEO content")
public class ContentController {

    @PostMapping
    @Operation(summary = "Create post or afisha")
    public ResponseEntity<PostResponse> create(@RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(UUID.randomUUID(), request, "DRAFT"));
    }

    @GetMapping
    @Operation(summary = "List posts")
    public ResponseEntity<PageResponse<PostResponse>> list() {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post")
    public ResponseEntity<PostResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(new PostResponse(id, "stub", "", "DRAFT", List.of(), Instant.now()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace post")
    public ResponseEntity<PostResponse> replace(@PathVariable("id") UUID id, @RequestBody PostRequest request) {
        return ResponseEntity.ok(PostResponse.from(id, request, "DRAFT"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch post")
    public ResponseEntity<PostResponse> patch(@PathVariable("id") UUID id, @RequestBody PostRequest request) {
        return ResponseEntity.ok(PostResponse.from(id, request, "DRAFT"));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish post")
    public ResponseEntity<ApiCommandResponse> publish(@PathVariable("id") UUID id) {
        return ResponseEntity.accepted().body(ApiCommandResponse.accepted("POST_PUBLISH_ACCEPTED"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive post")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record PostRequest(String title, String body, String seoTitle, String seoDescription, List<UUID> mediaIds) {
    }

    public record PostResponse(UUID id, String title, String body, String status, List<UUID> mediaIds, Instant updatedAt) {
        static PostResponse from(UUID id, PostRequest request, String status) {
            return new PostResponse(id, request.title(), request.body(), status, request.mediaIds(), Instant.now());
        }
    }
}
