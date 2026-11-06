package com.cgv.catalogservice.service.impl;

import com.cgv.catalogservice.enums.ShowingStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovieShowingStatusTest {
    @Test
    void statusMatchesReleaseAndEndDateInclusively() {
        LocalDate today = LocalDate.of(2026, 11, 6);
        assertEquals(ShowingStatus.COMING_SOON,
                MovieServiceImpl.showingStatusFor(today.plusDays(1), today.plusDays(10), today));
        assertEquals(ShowingStatus.NOW_SHOWING,
                MovieServiceImpl.showingStatusFor(today, today, today));
        assertEquals(ShowingStatus.ENDED,
                MovieServiceImpl.showingStatusFor(today.minusDays(10), today.minusDays(1), today));
        assertEquals(ShowingStatus.NOW_SHOWING,
                MovieServiceImpl.showingStatusFor(today.minusDays(1), null, today));
        assertThrows(IllegalArgumentException.class,
                () -> MovieServiceImpl.showingStatusFor(today.plusDays(1), today, today));
        assertThrows(IllegalArgumentException.class,
                () -> MovieServiceImpl.showingStatusFor(null, today, today));
    }
}
