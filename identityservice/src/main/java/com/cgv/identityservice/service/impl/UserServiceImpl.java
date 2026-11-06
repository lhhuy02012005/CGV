package com.cgv.identityservice.service.impl;

import com.cgv.commondto.dto.PageResponse;
import com.cgv.commondto.exception.ErrorCode;
import com.cgv.commondto.exception.BusinessException;
import com.cgv.identityservice.dto.request.UpdateProfileRequest;
import com.cgv.identityservice.dto.request.AdminUserRequest;
import com.cgv.identityservice.dto.request.UserCreationParam;
import com.cgv.identityservice.dto.request.RoleRepresentation;
import com.cgv.identityservice.dto.response.UserResponse;
import com.cgv.identityservice.entity.User;
import com.cgv.identityservice.repository.MemberShipTierRepository;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import com.cgv.identityservice.mapper.UserMapper;
import com.cgv.identityservice.repository.UserRepository;
import com.cgv.identityservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import feign.FeignException;
import tools.jackson.databind.JsonNode;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    KeycloakClient keycloakClient;
    MemberShipTierRepository tierRepository;

    @NonFinal @Value("${keycloak.realm:cgv-realm}")
    String realm;
    @NonFinal @Value("${keycloak.client-id:CGV_App}")
    String clientId;
    @NonFinal @Value("${keycloak.client-secret}")
    String clientSecret;

    private static final Set<String> APP_ROLES = Set.of("MEMBER_USER", "SUPER_ADMIN", "CINEMA_MANAGER",
            "TICKET_STAFF", "CONTENT_MANAGER");

    private String adminToken() {
        JsonNode response = keycloakClient.exchangeToken(realm, Map.of(
                "grant_type", "client_credentials", "client_id", clientId, "client_secret", clientSecret));
        return "Bearer " + response.get("access_token").asText();
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            Map<String, Object> remote = keycloakClient.getUser(adminToken(), realm, userId);
            String email = (String) remote.get("email");
            if (email == null || email.isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản Keycloak chưa có email");
            }
            return userRepository.save(User.builder().id(userId).email(email)
                    .fullName(remoteName(remote)).membershipTier(tierRepository.findById("MEMBER").orElseThrow())
                    .status(Boolean.TRUE.equals(remote.get("enabled")) ? "ACTIVE" : "BLOCKED").build());
        });
    }

    private String remoteName(Map<String, Object> remote) {
        String name = (String.valueOf(remote.getOrDefault("lastName", "")) + " "
                + String.valueOf(remote.getOrDefault("firstName", ""))).trim();
        return name.isBlank() ? String.valueOf(remote.getOrDefault("username", remote.get("email"))) : name;
    }

    private void requireOtherUser(String userId, String actorId) {
        if (userId.equals(actorId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Không thể tự khóa, xóa hoặc đổi vai trò của mình");
        }
    }

    private List<RoleRepresentation> assignedRoles(String token, String userId) {
        try {
            return keycloakClient.getUserRoles(token, realm, userId);
        } catch (FeignException.NotFound missingKeycloakAccount) {
            return List.of();
        }
    }

    private UserResponse adminResponse(User user, String token) {
        UserResponse response = userMapper.toUserResponse(user);
        response.setRoles(assignedRoles(token, user.getId()).stream().map(RoleRepresentation::getName)
                .filter(APP_ROLES::contains).map(role -> role.equals("MEMBER_USER") ? "USER" : role).toList());
        return response;
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(String keyword, String sort, int page, int size) {
        if (size < 1 || size > 100) throw new BusinessException(ErrorCode.BAD_REQUEST, "size phải từ 1 đến 100");
        String token = adminToken();
        // ponytail: Admin list <= 1000 accounts; use Keycloak server-side paging for larger realms.
        List<Map<String, Object>> remote = keycloakClient.listUsers(token, realm, 0, 1000);
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        List<UserResponse> all = new ArrayList<>();
        for (Map<String, Object> account : remote) {
            String userId = String.valueOf(account.get("id"));
            UserResponse response = userRepository.findById(userId).map(userMapper::toUserResponse)
                    .orElseGet(UserResponse::new);
            response.setId(userId);
            response.setEmail(String.valueOf(account.getOrDefault("email", "")));
            if (response.getFullName() == null) response.setFullName(remoteName(account));
            if (response.getStatus() == null) response.setStatus(Boolean.TRUE.equals(account.get("enabled")) ? "ACTIVE" : "BLOCKED");
            if (response.getCreatedAt() == null && account.get("createdTimestamp") instanceof Number timestamp) {
                response.setCreatedAt(Instant.ofEpochMilli(timestamp.longValue()));
            }
            response.setRoles(assignedRoles(token, userId).stream().map(RoleRepresentation::getName)
                    .filter(APP_ROLES::contains).map(role -> role.equals("MEMBER_USER") ? "USER" : role).toList());
            if (query.isEmpty() || (response.getEmail() + " " + response.getFullName() + " " + response.getPhone())
                    .toLowerCase().contains(query)) all.add(response);
        }
        all.sort(Comparator.comparing(UserResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        int from = (int) Math.min(all.size(), (long) Math.max(0, page - 1) * size);
        PageResponse<UserResponse> result = new PageResponse<>();
        result.setPageNumber(Math.max(1, page));
        result.setPageSize(size);
        result.setTotalElements((long) all.size());
        result.setTotalPages((int) Math.ceil((double) all.size() / size));
        result.setData(all.subList(from, Math.min(all.size(), from + size)));
        return result;
    }

    @Override
    public UserResponse getMyProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy thông tin tài khoản người dùng"));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTED, "Không tìm thấy thông tin tài khoản người dùng"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String newPhone = request.getPhone().trim();
            userRepository.findByPhone(newPhone).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(userId)) {
                    throw new BusinessException(ErrorCode.DUPLICATE, "Số điện thoại đã được đăng ký bởi tài khoản khác!");
                }
            });
            user.setPhone(newPhone);
        }

        User saved = userRepository.save(user);
        return userMapper.toUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse createAdminUser(AdminUserRequest request) {
        if (request.password() == null || request.password().length() < 6) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Mật khẩu tối thiểu 6 ký tự");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "Email đã tồn tại");
        }
        String token = adminToken();
        String name = request.fullName().trim();
        int split = name.lastIndexOf(' ');
        ResponseEntity<?> created = keycloakClient.createUser(token, realm, UserCreationParam.builder()
                .username(request.username() == null || request.username().isBlank() ? request.email() : request.username())
                .email(request.email()).firstName(name.substring(split + 1))
                .lastName(split < 0 ? "" : name.substring(0, split)).enabled(true).emailVerified(false)
                .credentials(List.of(UserCreationParam.Credential.builder().type("password")
                        .value(request.password()).temporary(false).build())).build());
        String location = created.getHeaders().getFirst("Location");
        if (location == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "Keycloak không trả về ID tài khoản");
        String userId = location.substring(location.lastIndexOf('/') + 1);
        try {
            User user = User.builder().id(userId).email(request.email()).fullName(name).phone(request.phone())
                    .membershipTier(tierRepository.findById("MEMBER").orElseThrow()).build();
            user = userRepository.saveAndFlush(user);
            assignRole(token, userId, request.role() == null ? "USER" : request.role());
            return adminResponse(user, token);
        } catch (RuntimeException failure) {
            keycloakClient.deleteUser(token, realm, userId);
            throw failure;
        }
    }

    @Override
    public UserResponse updateAdminUser(String userId, AdminUserRequest request) {
        User user = requireUser(userId);
        if ("DELETED".equals(user.getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã xóa");
        if (!request.email().equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE, "Email đã tồn tại");
        }
        String token = adminToken();
        String name = request.fullName().trim();
        int split = name.lastIndexOf(' ');
        keycloakClient.updateUser(token, realm, userId, Map.of("email", request.email(),
                "firstName", name.substring(split + 1), "lastName", split < 0 ? "" : name.substring(0, split)));
        user.setEmail(request.email());
        user.setFullName(name);
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone());
        return adminResponse(userRepository.save(user), token);
    }

    @Override
    public void setBlocked(String userId, String actorId, boolean blocked) {
        requireOtherUser(userId, actorId);
        User user = requireUser(userId);
        if ("DELETED".equals(user.getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã xóa");
        String token = adminToken();
        keycloakClient.updateUser(token, realm, userId, Map.of("enabled", !blocked));
        if (blocked) keycloakClient.logoutUser(token, realm, userId);
        user.setStatus(blocked ? "BLOCKED" : "ACTIVE");
        userRepository.save(user);
    }

    @Override
    public void deleteAdminUser(String userId, String actorId) {
        requireOtherUser(userId, actorId);
        User user = requireUser(userId);
        String token = adminToken();
        keycloakClient.updateUser(token, realm, userId, Map.of("enabled", false));
        keycloakClient.logoutUser(token, realm, userId);
        user.setStatus("DELETED");
        userRepository.save(user);
    }

    private void assignRole(String token, String userId, String role) {
        String targetRole = "USER".equals(role) ? "MEMBER_USER" : role;
        if (!APP_ROLES.contains(targetRole)) throw new BusinessException(ErrorCode.BAD_REQUEST, "Vai trò không hợp lệ");
        RoleRepresentation target = keycloakClient.getRoleByName(token, realm, targetRole);
        List<RoleRepresentation> oldRoles = assignedRoles(token, userId).stream()
                .filter(existing -> APP_ROLES.contains(existing.getName()) && !existing.getName().equals(targetRole))
                .toList();
        if (!oldRoles.isEmpty()) keycloakClient.removeUserRoles(token, realm, userId, oldRoles);
        keycloakClient.assignRoleToUser(token, realm, userId, List.of(target));
    }

    @Override
    public void assignAdminRole(String userId, String actorId, String role) {
        requireOtherUser(userId, actorId);
        if ("DELETED".equals(requireUser(userId).getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã xóa");
        assignRole(adminToken(), userId, role);
    }

    @Override
    public void removeAdminRole(String userId, String actorId, String role) {
        requireOtherUser(userId, actorId);
        if ("DELETED".equals(requireUser(userId).getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "Tài khoản đã xóa");
        String targetRole = "USER".equals(role) ? "MEMBER_USER" : role;
        if (!APP_ROLES.contains(targetRole)) throw new BusinessException(ErrorCode.BAD_REQUEST, "Vai trò không hợp lệ");
        String token = adminToken();
        List<RoleRepresentation> roles = assignedRoles(token, userId).stream()
                .filter(existing -> existing.getName().equals(targetRole)).toList();
        if (!roles.isEmpty()) keycloakClient.removeUserRoles(token, realm, userId, roles);
    }

}
