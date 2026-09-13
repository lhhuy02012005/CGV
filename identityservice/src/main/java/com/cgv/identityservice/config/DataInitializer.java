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
                    .name("Member")
                    .minSpend(BigDecimal.ZERO)
                    .description("Hạng thành viên tiêu chuẩn")
                    .build());
            log.info("Created MemberShipTier: MEMBER");
        }

        if (!memberShipTierRepository.existsById("VIP")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("VIP")
                    .name("VIP")
                    .minSpend(BigDecimal.valueOf(2_000_000))
                    .description("Hạng thành viên VIP")
                    .build());
            log.info("Created MemberShipTier: VIP");
        }

        if (!memberShipTierRepository.existsById("VVIP")) {
            memberShipTierRepository.save(MemberShipTier.builder()
                    .code("VVIP")
                    .name("VVIP")
                    .minSpend(BigDecimal.valueOf(5_000_000))
                    .description("Hạng thành viên VVIP")
                    .build());
            log.info("Created MemberShipTier: VVIP");
        }
    }
}
