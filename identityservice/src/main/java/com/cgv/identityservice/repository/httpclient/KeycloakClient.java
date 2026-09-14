package com.cgv.identityservice.repository.httpclient;

import com.cgv.identityservice.dto.request.RoleRepresentation;
import com.cgv.identityservice.dto.request.UserCreationParam;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "keycloak-client", url = "${keycloak.auth-server-url:http://localhost:8180}")
public interface KeycloakClient {

    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    JsonNode exchangeToken(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );

    @PostMapping(value = "/admin/realms/{realm}/users",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<?> createUser(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @RequestBody UserCreationParam param
    );

    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void logout(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );

    /**
     * Role
     */
    @PostMapping(value = "/admin/realms/{realm}/roles", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> createRole(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @RequestBody RoleRepresentation role
    );

    @GetMapping(value = "/admin/realms/{realm}/roles/{role-name}" , consumes = MediaType.APPLICATION_JSON_VALUE)
    RoleRepresentation getRoleByName(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @PathVariable("role-name") String roleName
    );

    @PutMapping(value = "/admin/realms/{realm}/roles/{role-name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> updateRole(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @PathVariable("role-name") String roleName,
            @RequestBody RoleRepresentation role
    );

    @DeleteMapping(value = "/admin/realms/{realm}/roles/{role-name}")
    ResponseEntity<?> deleteRole(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @PathVariable("role-name") String roleName
    );

    @PostMapping(value = "/admin/realms/{realm}/users/{userId}/role-mappings/realm", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> assignRoleToUser(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @PathVariable("userId") String userId,
            @RequestBody List<RoleRepresentation> roles
    );

    @GetMapping(value = "/admin/realms/{realm}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    List<RoleRepresentation> getAllRoles(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm
    );

    @PostMapping(value = "/admin/realms/{realm}/roles/{role-name}/composites", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> addCompositeRoles(
            @RequestHeader("Authorization") String adminToken,
            @PathVariable("realm") String realm,
            @PathVariable("role-name") String parentRoleName,
            @RequestBody List<RoleRepresentation> childRoles
    );
}