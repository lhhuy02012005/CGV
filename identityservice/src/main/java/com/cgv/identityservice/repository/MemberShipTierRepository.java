package com.cgv.identityservice.repository;

import com.cgv.identityservice.entity.MemberShipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberShipTierRepository extends JpaRepository<MemberShipTier, String> {
}
