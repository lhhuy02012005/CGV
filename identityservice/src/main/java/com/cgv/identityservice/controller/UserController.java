package com.cgv.identityservice.controller;

import com.cgv.commondto.dto.ApiResponse;
import com.cgv.commondto.dto.PageResponse;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping()
    public ApiResponse<PageResponse<UserResponse>> findAll(
            @RequestParam(required = false) String keyword ,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "1") int size
    ){
        var response = userService.getAllUsers(keyword, sort, page, size);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .status(HttpStatus.OK.value())
                .data(response)
                .message("Danh sách người dùng")
                .build();
    }
}
