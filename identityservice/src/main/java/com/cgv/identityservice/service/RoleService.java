package com.cgv.identityservice.service;

import com.cgv.identityservice.dto.request.RoleRepresentation;

import java.util.List;

public interface RoleService {
    void createRole(RoleRepresentation request);
    RoleRepresentation getRole(String roleName);
    List<RoleRepresentation> getAllRoles();
    void updateRole(String roleName, RoleRepresentation request);
    void deleteRole(String roleName);
    void assignRoleToUser(String userId, String roleName);
    void addAssociatedRoles(String parentRoleName, List<String> childRoleNames);
}
