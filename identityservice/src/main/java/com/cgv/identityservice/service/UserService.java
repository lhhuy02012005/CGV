package com.cgv.identityservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.identityservice.dto.request.UpdateProfileRequest;
import com.cgv.identityservice.dto.response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> getAllUsers(String keyword, String sort, int page, int size);
    UserResponse getMyProfile(String userId);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    UserResponse createAdminUser(com.cgv.identityservice.dto.request.AdminUserRequest request);
    UserResponse updateAdminUser(String userId, com.cgv.identityservice.dto.request.AdminUserRequest request);
    void setBlocked(String userId, String actorId, boolean blocked);
    void deleteAdminUser(String userId, String actorId);
    void assignAdminRole(String userId, String actorId, String role);
    void removeAdminRole(String userId, String actorId, String role);
}
