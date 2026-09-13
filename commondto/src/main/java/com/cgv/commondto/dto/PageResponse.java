package com.cgv.commondto.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse <T> implements Serializable {
    List<T> data;
    int pageNumber;
    int pageSize;
    int totalPages;
    long totalElements;
}
