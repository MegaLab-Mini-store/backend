package kg.megalab.ministorebackend.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        Long total,
        Integer limit,
        Integer offset
) {
    public static <T> PageResponse<T> empty(int limit, int offset) {
        return new PageResponse<>(List.of(), 0L, limit, offset);
    }
}
