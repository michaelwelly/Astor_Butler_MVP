package museon_online.astor_butler.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Page response for list endpoints")
public record PageResponse<T>(
        int page,
        int size,
        long total,
        List<T> items
) {
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(page, size, 0, List.of());
    }
}
