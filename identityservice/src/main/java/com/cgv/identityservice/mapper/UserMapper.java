package com.cgv.identityservice.mapper;

import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
