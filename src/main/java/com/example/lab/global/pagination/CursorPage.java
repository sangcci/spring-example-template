package com.example.lab.global.pagination;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPage<T, C>(List<T> items, C nextCursor) {

    public CursorPage {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
