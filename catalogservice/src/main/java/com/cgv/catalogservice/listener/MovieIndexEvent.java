package com.cgv.catalogservice.listener;

import java.util.UUID;

/** Yêu cầu đồng bộ lại 1 phim lên Elasticsearch (VD: khi thêm/sửa/xóa diễn viên). */
public record MovieIndexEvent(UUID movieId) {
}
