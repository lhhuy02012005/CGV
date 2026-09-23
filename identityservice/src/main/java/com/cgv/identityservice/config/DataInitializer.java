package com.cgv.identityservice.config;

import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DataInitializer implements CommandLineRunner {

    MemberShipTierRepository memberShipTierRepository;

    @Override
    public void run(String... args) {
        initMembershipTiers();
    }

    private void initMembershipTiers() {
        if (!memberShipTierRepository.existsById("MEMBER")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("MEMBER")
                    .name("Thành viên Tiêu chuẩn")
                    .minSpend(BigDecimal.ZERO)
                    .description("Hạng thành viên tiêu chuẩn")
                    .build());
            log.info("Created MemberShipTier: MEMBER");
        }

        if (!memberShipTierRepository.existsById("SILVER")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("SILVER")
                    .name("Thành viên Bạc")
                    .minSpend(BigDecimal.valueOf(2_000_000))
                    .description("Chi tiêu từ 2 triệu/năm, tích 5% điểm thưởng")
                    .build());
            log.info("Created MemberShipTier: SILVER");
        }

        if (!memberShipTierRepository.existsById("GOLD")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("GOLD")
                    .name("Thành viên Vàng")
                    .minSpend(BigDecimal.valueOf(5_000_000))
                    .description("Chi tiêu từ 5 triệu/năm, tích 7% điểm, bắp nước sinh nhật")
                    .build());
            log.info("Created MemberShipTier: GOLD");
        }

        if (!memberShipTierRepository.existsById("PLATINUM")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("PLATINUM")
                    .name("Thành viên Bạch Kim")
                    .minSpend(BigDecimal.valueOf(15_000_000))
                    .description("Chi tiêu từ 15 triệu/năm, tích 10% điểm, phòng chờ VIP")
                    .build());
            log.info("Created MemberShipTier: PLATINUM");
        }
    }
}
