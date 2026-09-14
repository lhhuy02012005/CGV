package com.cgv.identityservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.identityservice.dto.request.RoleRepresentation;
import com.cgv.identityservice.service.RoleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {

    RoleService roleService;

    @PostMapping
    public ApiResponse<Void> createRole(@RequestBody RoleRepresentation request) {
        roleService.createRole(request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo Role thành công")
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoleRepresentation>> getAllRoles() {
        return ApiResponse.<List<RoleRepresentation>>builder()
                .status(HttpStatus.OK.value())
                .data(roleService.getAllRoles())
                .message("Danh sách Role")
                .build();
    }

    @GetMapping("/{roleName}")
    public ApiResponse<RoleRepresentation> getRole(@PathVariable String roleName) {
        return ApiResponse.<RoleRepresentation>builder()
                .status(HttpStatus.OK.value())
                .data(roleService.getRole(roleName))
                .message("Chi tiết Role")
                .build();
    }

    @PutMapping("/{roleName}")
    public ApiResponse<Void> updateRole(@PathVariable String roleName, @RequestBody RoleRepresentation request) {
        roleService.updateRole(roleName, request);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật Role thành công")
                .build();
    }

    @DeleteMapping("/{roleName}")
    public ApiResponse<Void> deleteRole(@PathVariable String roleName) {
        roleService.deleteRole(roleName);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa Role thành công")
                .build();
    }

    @PostMapping("/{roleName}/assign-to/{userId}")
    public ApiResponse<Void> assignRoleToUser(
            @PathVariable String roleName,
            @PathVariable String userId) {
        roleService.assignRoleToUser(userId, roleName);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Gán Role cho User thành công")
                .build();
    }

    @PostMapping("/{parentRoleName}/composites")
    public ApiResponse<Void> addCompositeRoles(
            @PathVariable String parentRoleName,
            @RequestBody List<String> childRoleNames) {

        roleService.addAssociatedRoles(parentRoleName, childRoleNames);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Gộp quyền thành công vào chức danh")
                .build();
    }
}