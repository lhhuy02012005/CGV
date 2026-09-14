package com.cgv.identityservice.service;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.identityservice.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {
    PageResponse<UserResponse> getAllUsers(String keyword , String sort , int page , int size);
}
