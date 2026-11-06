package com.cgv.bookingservice;
import com.cgv.bookingservice.entity.*;
import com.cgv.bookingservice.enums.BookingStatus;
import com.cgv.bookingservice.repository.BookingRepository;
import jakarta.persistence.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.flyway.enabled=false","spring.jpa.database-platform=org.hibernate.dialect.H2Dialect","spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.generate_statistics=true"})
class BookingNPlusOneIntegrationTest {
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired BookingRepository repository;
    @Test void entityGraphReducesFourStatementsToOneForThreeBookings() throws Exception {
        for(int i=0;i<3;i++) {
            Booking b=Booking.builder().userId("nplusone-user").showtimeId(UUID.randomUUID())
                    .totalBaseAmount(BigDecimal.TEN).finalAmount(BigDecimal.TEN).status(BookingStatus.PAYMENT_PENDING).build();
            BookingSeat seat=BookingSeat.builder().booking(b).seatId(UUID.randomUUID()).showtimeId(b.getShowtimeId()).price(BigDecimal.TEN).build();
            b.getBookingSeats().add(seat); em.persist(b);
        }
        em.flush(); em.clear();
        var stats=emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        var baseline=em.createQuery("select b from Booking b where b.userId = :user",Booking.class).setParameter("user","nplusone-user").getResultList();
        assertEquals(3,baseline.stream().mapToInt(b -> b.getBookingSeats().size()).sum());
        long before=stats.getPrepareStatementCount();
        em.clear(); stats.clear();
        var fetched=repository.findByUserIdOrderByCreatedAtDesc("nplusone-user");
        assertEquals(3,fetched.stream().mapToInt(b -> b.getBookingSeats().size()).sum());
        long after=stats.getPrepareStatementCount(); assertEquals(4,before); assertEquals(1,after);
        String result="{\"database\":\"H2 test database\",\"bookings\":3,\"before_sql\":"+before+",\"after_sql\":"+after+",\"optimization\":\"BookingRepository EntityGraph bookingSeats\"}";
        Files.writeString(Path.of("target/n-plus-one.json"),result); System.out.println("N_PLUS_ONE "+result);
    }
}
