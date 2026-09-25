package com.cgv.identityservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.identityservice.dto.request.UpdateProfileRequest;
import com.cgv.identityservice.dto.response.UserResponse;

public interface UserService {
    PageResponse<UserResponse> getAllUsers(String keyword, String sort, int page, int size);
    UserResponse getMyProfile(String userId);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
}
