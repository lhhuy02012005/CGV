package com.cgv.identityservice.service.impl;

import com.cgv.commondto.exception.BusinessException;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.identityservice.dto.request.RoleRepresentation;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import com.cgv.identityservice.service.RoleService;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "ROLE-SERVICE")
public class RoleServiceImpl implements RoleService {
    KeycloakClient keycloakClient;

    @NonFinal
    @Value("${keycloak.realm:cgv-realm}")
    String realm;

    @NonFinal
    @Value("${keycloak.client-id:CGV_App}")
    String clientId;

    @NonFinal
    @Value("${keycloak.client-secret}")
    String clientSecret;

    private String getAdminAccessToken(){
        Map<String , String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", clientId);
        body.put("scope","openid");
        body.put("client_secret", clientSecret);
        JsonNode resposne = keycloakClient.exchangeToken(realm,body);

        return "Bearer " + resposne.get("access_token").asText();
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public void createRole(RoleRepresentation request) {
        try {
            String accessToken = getAdminAccessToken();
            keycloakClient.createRole(accessToken,realm,request);
            log.info("Đã tạo thành công Role: {}", request.getName());
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể tạo được vai trò !");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public RoleRepresentation getRole(String roleName) {
        try {
            String token = getAdminAccessToken();
            return keycloakClient.getRoleByName(token, realm, roleName);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể xem được vai trò !");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public List<RoleRepresentation> getAllRoles() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String token = getAdminAccessToken();
            return keycloakClient.getAllRoles(token, realm);
        } catch (Exception e) {
            log.error("Lỗi hệ thống hoặc Jackson: ", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể xem được danh sách vai trò !");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public void updateRole(String roleName, RoleRepresentation request) {
        try {
            String token = getAdminAccessToken();
            keycloakClient.updateRole(token, realm, roleName, request);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể cập nhật được vai trò !");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public void deleteRole(String roleName) {
        try {
            String accessToken = getAdminAccessToken();
            keycloakClient.deleteRole(accessToken,realm,roleName);
            log.info("Đã xoá thành công Role: {}", roleName);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể tạo được vai trò !");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public void assignRoleToUser(String userId, String roleName) {
        try {
            String token = getAdminAccessToken();

            RoleRepresentation roleInfo = keycloakClient.getRoleByName(token, realm, roleName);

            RoleRepresentation assignPayload = RoleRepresentation.builder()
                    .id(roleInfo.getId())
                    .name(roleInfo.getName())
                    .build();

            keycloakClient.assignRoleToUser(token, realm, userId, List.of(assignPayload));
            log.info("Đã gán Role {} cho User ID: {}", roleName, userId);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lỗi : Không thể gán vai trò cho người dùng");
        }
    }

    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Override
    public void addAssociatedRoles(String parentRoleName, List<String> childRoleNames) {
        try {
            String adminToken = getAdminAccessToken();

            List<RoleRepresentation> childRoles = childRoleNames.stream()
                    .map(roleName -> keycloakClient.getRoleByName(adminToken, realm, roleName))
                    .toList();

            keycloakClient.addCompositeRoles(adminToken, realm, parentRoleName, childRoles);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Lỗi: Không thể tạo nhóm vai trò con");
        }
    }
}
