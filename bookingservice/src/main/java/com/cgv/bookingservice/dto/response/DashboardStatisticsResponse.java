package com.cgv.bookingservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatisticsResponse {
    BigDecimal revenueToday;
    BigDecimal revenueThisWeek;
    BigDecimal revenueTotal;
    long ticketsSoldToday;
    long ticketsSoldTotal;
    long activeBookingsCount;
    Map<String, Long> statusBreakdown;
    List<DailyRevenueItem> weeklyRevenue;
    List<TopMovieItem> topMovies;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DailyRevenueItem {
        String day;
        String date;
        BigDecimal amount;
        long count;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TopMovieItem {
        String movieId;
        String movieTitle;
        String posterUrl;
        long bookingCount;
        BigDecimal totalAmount;
    }
}
