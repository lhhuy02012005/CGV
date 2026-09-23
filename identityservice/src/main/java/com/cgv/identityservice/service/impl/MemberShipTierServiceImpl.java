package com.cgv.identityservice.service.impl;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.identityservice.dto.request.MemberShipTierRequest;
import com.cgv.identityservice.dto.response.MemberShipTierResponse;
import com.cgv.identityservice.entity.MemberShipTier;
import com.cgv.identityservice.mapper.MemberShipTierMapper;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import com.cgv.identityservice.service.MemberShipTierService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MemberShipTierServiceImpl implements MemberShipTierService {

    MemberShipTierRepository memberShipTierRepository;
    MemberShipTierMapper memberShipTierMapper;

    @Override
    @Transactional
    public MemberShipTierResponse createTier(MemberShipTierRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (memberShipTierRepository.existsById(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Hạng thành viên " + code + " đã tồn tại!");
        }

        MemberShipTier tier = MemberShipTier.builder()
                .code(code)
                .name(request.getName())
                .minSpend(request.getMinSpend())
                .description(request.getDescription())
                .build();

        MemberShipTier saved = memberShipTierRepository.save(tier);
        log.info("Đã tạo hạng thành viên mới: {} với minSpend: {}", code, request.getMinSpend());
        return memberShipTierMapper.toMemberShipTierResponse(saved);
    }

    @Override
    @Transactional
    public MemberShipTierResponse updateTier(String code, MemberShipTierRequest request) {
        MemberShipTier tier = memberShipTierRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy hạng thành viên: " + code));

        tier.setName(request.getName());
        tier.setMinSpend(request.getMinSpend());
        tier.setDescription(request.getDescription());

        MemberShipTier updated = memberShipTierRepository.save(tier);
        log.info("Đã cập nhật hạng thành viên: {}, minSpend mới: {}", code, request.getMinSpend());
        return memberShipTierMapper.toMemberShipTierResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberShipTierResponse> getAllTiers() {
        return memberShipTierRepository.findAll(Sort.by(Sort.Direction.ASC, "minSpend")).stream()
                .map(memberShipTierMapper::toMemberShipTierResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MemberShipTierResponse getTierByCode(String code) {
        MemberShipTier tier = memberShipTierRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy hạng thành viên: " + code));
        return memberShipTierMapper.toMemberShipTierResponse(tier);
    }

    @Override
    @Transactional
    public void deleteTier(String code) {
        if ("MEMBER".equalsIgnoreCase(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể xóa hạng thành viên mặc định MEMBER!");
        }
        MemberShipTier tier = memberShipTierRepository.findById(code.toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Không tìm thấy hạng thành viên: " + code));
        memberShipTierRepository.delete(tier);
        log.info("Đã xóa hạng thành viên: {}", code);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberShipTier determineTierBySpend(BigDecimal totalSpendYtd) {
        if (totalSpendYtd == null || totalSpendYtd.compareTo(BigDecimal.ZERO) <= 0) {
            return memberShipTierRepository.findById("MEMBER").orElse(null);
        }

        List<MemberShipTier> sortedTiers = memberShipTierRepository.findAll(Sort.by(Sort.Direction.DESC, "minSpend"));
        for (MemberShipTier tier : sortedTiers) {
            if (tier.getMinSpend() != null && totalSpendYtd.compareTo(tier.getMinSpend()) >= 0) {
                return tier;
            }
        }
        return memberShipTierRepository.findById("MEMBER").orElse(null);
    }
}
