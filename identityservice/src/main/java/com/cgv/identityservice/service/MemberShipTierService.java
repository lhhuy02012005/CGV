package com.cgv.identityservice.service;

import com.cgv.identityservice.dto.request.MemberShipTierRequest;
import com.cgv.identityservice.dto.response.MemberShipTierResponse;
import com.cgv.identityservice.entity.MemberShipTier;

import java.math.BigDecimal;
import java.util.List;

public interface MemberShipTierService {

    MemberShipTierResponse createTier(MemberShipTierRequest request);

    MemberShipTierResponse updateTier(String code, MemberShipTierRequest request);

    List<MemberShipTierResponse> getAllTiers();

    MemberShipTierResponse getTierByCode(String code);

    void deleteTier(String code);

    MemberShipTier determineTierBySpend(BigDecimal totalSpendYtd);
}
