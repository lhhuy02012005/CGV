package com.cgv.identityservice.mapper;

import com.cgv.identityservice.dto.response.MemberShipTierResponse;
import com.cgv.identityservice.entity.MemberShipTier;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MemberShipTierMapper {
    MemberShipTierResponse toMemberShipTierResponse(MemberShipTier memberShipTier);
}
