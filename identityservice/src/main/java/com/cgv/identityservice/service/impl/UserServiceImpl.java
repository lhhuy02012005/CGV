package com.cgv.identityservice.service.impl;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.mapper.UserMapper;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getAllUsers(String keyword, String sort, int page, int size ) {
        Sort order = Sort.by(Sort.Direction.DESC, "createdAt");
        if(sort != null && !sort.isEmpty()) {
            Pattern pattern = Pattern.compile("(\\w+?)(:)(.*)");
            Matcher matcher = pattern.matcher(sort);
            if (matcher.find()) {
                String columnName = matcher.group(1);
                order = matcher.group(3).equalsIgnoreCase("asc")
                        ? Sort.by(Sort.Direction.ASC, columnName)
                        : Sort.by(Sort.Direction.DESC, columnName);
            }
        }

        int pageNo = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageNo, size, order);
        Page<User> users = null;
        if(keyword == null || keyword.isEmpty()){
            users = userRepository.findAll(pageable);
        }else {
            keyword = "%" + keyword.toLowerCase() + "%";
            users = userRepository.findByKeyword(keyword, pageable);
        }

        return getUserPageResponse(pageNo,size,users);
    }

    private PageResponse<UserResponse> getUserPageResponse(int pageNo , int size , Page<User> users){
        List<UserResponse> userResponses = users.stream().map(user -> userMapper.toUserResponse(user)).toList();
        PageResponse<UserResponse> response = new PageResponse<>();
        response.setPageNumber(pageNo + 1);
        response.setData(userResponses);
        response.setPageSize(size);
        response.setTotalElements(users.getTotalElements());
        response.setTotalPages(users.getTotalPages());
        return response;
    }
}
