package com.cgv.catalogservice.util;

import com.cgv.commondto.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;

public final class PageResponseUtils {

    private PageResponseUtils() {
    }

    public static <E, R> PageResponse<R> fromPage(
            Page<E> page,
            Function<List<E>, List<R>> mapper
    ) {

        List<R> responses =
                mapper.apply(page.getContent());

        return PageResponse.<R>builder()
                .data(responses)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }

    public static <E, R> PageResponse<R> findAllAndMap(
            Function<Pageable, Page<E>> pageFetcher,
            Pageable pageable,
            Function<List<E>, List<R>> mapper
    ) {

        Page<E> page =
                pageFetcher.apply(pageable);

        return fromPage(page, mapper);
    }
}