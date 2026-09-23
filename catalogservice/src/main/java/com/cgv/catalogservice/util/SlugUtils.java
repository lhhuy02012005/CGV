package com.cgv.catalogservice.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String value) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Không thể tạo slug từ chuỗi rỗng"
            );
        }

        String slug = Normalizer.normalize(
                        value.trim(),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "Không thể tạo slug hợp lệ"
            );
        }

        return slug;
    }

    public static String generateUniqueSlug(
            String value,
            Predicate<String> slugExists
    ) {
        String baseSlug = toSlug(value);
        String slug = baseSlug;
        int suffix = 2;

        while (slugExists.test(slug)) {
            slug = baseSlug + "-" + suffix;
            suffix++;
        }

        return slug;
    }
}