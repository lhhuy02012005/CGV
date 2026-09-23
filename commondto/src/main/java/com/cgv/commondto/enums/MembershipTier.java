package com.cgv.commondto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipTier {
    ALL("ALL", 0, "Tất cả khách hàng"),
    MEMBER("MEMBER", 1, "Thành viên Tiêu chuẩn"),
    SILVER("SILVER", 2, "Thành viên Bạc"),
    GOLD("GOLD", 3, "Thành viên Vàng"),
    PLATINUM("PLATINUM", 4, "Thành viên Bạch Kim");

    private final String code;
    private final int level;
    private final String name;

    public boolean canApply(MembershipTier tier){
        if(this == ALL){
            return true;
        }
        if(tier == null){
            return false;
        }
        return tier.level >= this.level;
    }

    public static MembershipTier fromCode(String code) {
        if (code == null || code.isBlank()) {
            return ALL;
        }
        for (MembershipTier tier : values()) {
            if (tier.code.equalsIgnoreCase(code)) {
                return tier;
            }
        }
        if ("VIP".equalsIgnoreCase(code)) {
            return GOLD;
        }
        if ("VVIP".equalsIgnoreCase(code)) {
            return PLATINUM;
        }
        return MEMBER;
    }
}
