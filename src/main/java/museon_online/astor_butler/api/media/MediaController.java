package museon_online.astor_butler.api.media;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Media API", description = "Upload, metadata, S3 links and soft delete")
public class MediaController {

    @PostMapping("/upload")
    @Operation(summary = "Register media upload")
    public ResponseEntity<MediaResponse> upload(@RequestBody MediaUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaResponse.from(UUID.randomUUID(), request, "UPLOADED"));
    }

    @GetMapping
    @Operation(summary = "List media")
    public ResponseEntity<PageResponse<MediaResponse>> list() {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get media metadata")
    public ResponseEntity<MediaResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(new MediaResponse(id, "stub", "application/octet-stream", null, "READY", Instant.now()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace media metadata")
    public ResponseEntity<MediaResponse> replace(@PathVariable("id") UUID id, @RequestBody MediaUploadRequest request) {
        return ResponseEntity.ok(MediaResponse.from(id, request, "READY"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch media metadata")
    public ResponseEntity<MediaResponse> patch(@PathVariable("id") UUID id, @RequestBody MediaUploadRequest request) {
        return ResponseEntity.ok(MediaResponse.from(id, request, "READY"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete media")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record MediaUploadRequest(String filename, String contentType, String objectKey) {
    }

    public record MediaResponse(UUID id, String filename, String contentType, String objectKey, String status, Instant updatedAt) {
        static MediaResponse from(UUID id, MediaUploadRequest request, String status) {
            return new MediaResponse(id, request.filename(), request.contentType(), request.objectKey(), status, Instant.now());
        }
    }
}
