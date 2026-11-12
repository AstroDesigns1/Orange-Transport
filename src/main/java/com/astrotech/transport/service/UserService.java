package com.astrotech.transport.service;


import com.astrotech.transport.core.*;
import com.astrotech.transport.customCache.CustomCacheable;
import com.astrotech.transport.dto.response.SliceResponse;
import com.astrotech.transport.dto.response.UserEntitiesResponse;
import com.astrotech.transport.dto.response.UserResponse;
import com.astrotech.transport.entities.User;

import com.astrotech.transport.enums.OnlineStatus;
import com.astrotech.transport.enums.UserRole;
import com.astrotech.transport.enums.UserStatus;
import com.astrotech.transport.exceptions.BadRequestException;
import com.astrotech.transport.exceptions.ConflictException;
import com.astrotech.transport.exceptions.ResourceNotFoundException;
import com.astrotech.transport.exceptions.UnAuthorizedUserException;
import com.astrotech.transport.mappers.UserMapper;
import com.astrotech.transport.repositories.DriverProfileRepository;
import com.astrotech.transport.repositories.UserRepository;
import com.astrotech.transport.validators.role.RoleRequired;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserService implements UserDetailsService {
    private final UserRepository userRepo;

    private final DriverProfileRepository driverProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final GetCurrentUser getCurrentUser;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var allowedStatuses = List.of(UserStatus.ACTIVE);
        var user = userRepo.findByEmailAndStatuses(email, allowedStatuses)
                .orElseThrow(() -> new UsernameNotFoundException("User deleted, suspended, or does not exist"));

        if (user.isDeleted()) {
            throw new DisabledException(
                    "Account deleted");
        }

        if (user.isSuspended()) {
            throw new LockedException(
                    "Account suspended");
        }
        return new CustomUserDetails(user);

    }

    @Transactional
    @RoleRequired({UserRole.ADMIN, UserRole.TERMINAL_SUPERVISOR})
    @CustomCacheable(
            value = "users",
            key = "#userId",
            ttl = 600,
            timeUnit = TimeUnit.SECONDS
    )
    public UserResponse getUser(UUID userId) {
        return userRepo.findById(userId)
                .map(UserMapper::response)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid User"));
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#user.getId()"),
            @CacheEvict(value = "user-searches", allEntries = true)
    })
    public void updateUser(User user, String email, String phoneNumber, String firstName, String lastName, String newPassword, String oldPassword) {

        var trimmedFirstName = TrimWhiteSpace.trimWhiteSpace(firstName);
        var trimmedLastName = TrimWhiteSpace.trimWhiteSpace(lastName);
        var trimmedEmail = TrimWhiteSpace.trimWhiteSpaceWithUpperCase(email, false);
        var trimmedPhoneNumber = TrimWhiteSpace.trimWhiteSpace(phoneNumber);


        if (firstName != null || lastName != null) {

            var names = AppBuilders.splitStrings(user.getFullName());


            var getFirstName = trimmedFirstName != null
                    ? trimmedFirstName
                    : names.firstName();

            var getLastName = trimmedLastName != null
                    ? trimmedLastName
                    : names.lastName();

            user.setFullName(AppBuilders.joinStrings(getFirstName, null, getLastName));
        }

        if (email != null) {

            if (!trimmedEmail.equalsIgnoreCase(user.getEmail())
                    && userRepo.existsByEmail(trimmedEmail)) {
                throw new ConflictException("Email already exists.");
            }

            user.setEmail(email);
            user.setVerified(false);
            user.setVerifiedAt(null);
        }

        if (phoneNumber != null) {
            if (!trimmedPhoneNumber.equals(user.getPhoneNumber())
                    && userRepo.existsByPhoneNumber(trimmedPhoneNumber)) {
                throw new ConflictException("Phone number already exists.");
            }

            user.setPhoneNumber(trimmedPhoneNumber);
        }

        if (newPassword != null) {

            if (oldPassword == null) {
                throw new BadRequestException("Old password is required.");
            }

            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new UnAuthorizedUserException("Old password is incorrect.");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
        }


    }


    @Transactional(readOnly = true)
    @RoleRequired(UserRole.ADMIN)
    public SliceResponse<UserResponse> getUsers(int page, int size) {
        var pageable = GetPageRequest.getPageableWithSorting(page, size, "fullName", true, User.class, true);
        var result = userRepo.findAllBy(pageable);
        var content = result.getContent()
                .stream()
                .map(UserMapper::response)
                .toList();
        return new SliceResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );

    }

    @Transactional(readOnly = true)
    @RoleRequired(UserRole.ADMIN)
    public SliceResponse<UserResponse> getAllTerminalSupervisors(int page, int size) {
        var pageable = GetPageRequest.getPageableWithSorting(page, size, "fullName", true, User.class, true);
        var result = userRepo.findAllUsersDetails(UserStatus.ACTIVE, UserRole.TERMINAL_SUPERVISOR, pageable);
        var content = result.getContent()
                .stream()
                .map(UserMapper::response)
                .toList();
        return new SliceResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );

    }

    @Transactional(readOnly = true)
    @RoleRequired(UserRole.ADMIN)
    public SliceResponse<UserResponse> getUnassignedTerminalSupervisors(int page, int size) {
        var pageable = GetPageRequest.getPageableWithSorting(page, size, "fullName", true, User.class, true);
        var result = userRepo.findUnassignedUsersByRole(UserStatus.ACTIVE, UserRole.TERMINAL_SUPERVISOR, pageable);
        var content = result.getContent()
                .stream()
                .map(UserMapper::response)
                .toList();
        return new SliceResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );

    }

    @Transactional(readOnly = true)
    @RoleRequired(UserRole.ADMIN)
    public SliceResponse<UserResponse> getUnassignedRegionalSupervisors(int page, int size) {
        var pageable = GetPageRequest.getPageableWithSorting(page, size, "fullName", true, User.class, true);
        var result = userRepo.findUnassignedUsersByRole(UserStatus.ACTIVE, UserRole.REGIONAL_SUPERVISOR, pageable);
        var content = result.getContent()
                .stream()
                .map(UserMapper::response)
                .toList();
        return new SliceResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );

    }


    @Transactional(readOnly = true)
    @RoleRequired(UserRole.ADMIN)
    @CustomCacheable(
            value = "user-searches",
            key = "'kw-' + #searchKeyWord + '-p' + #page + '-s' + #size",
            ttl = 120,
            timeUnit = TimeUnit.SECONDS
    )
    public SliceResponse<UserResponse> searchUsers(int page, int size, String searchKeyWord) {
        var searchedWord = TrimWhiteSpace.trimWhiteSpaceWithUpperCase(searchKeyWord, false);
        var pageable = GetPageRequest.getPageableWithSorting(page, size, searchedWord, false, User.class, false);
        var result = searchUser(searchedWord, pageable);
        var content = result.getContent()
                .stream()
                .map(UserMapper::response)
                .toList();
        return new SliceResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );

    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "user-searches", allEntries = true)
    })
    @RoleRequired(UserRole.ADMIN)
    public Map<String, String> deleteAccount(UUID userId) {

        var currentUserRole = getCurrentUser.getCurrentUserIdAndRole();
        if (currentUserRole.role() == UserRole.ADMIN) {
            throw new IllegalStateException("Not Allowed");
        }


        var user = getAuthorizedUser(userId);
        user.setDeletedAt(Instant.now());
        user.setStatus(UserStatus.DELETED);
        user.setDeleted(true);

        driverProfileRepository.findByUserId(user.getId())
                .ifPresent(
                        driverProfile -> {
                            driverProfile.setActive(false);
                            driverProfileRepository.save(driverProfile);
                        });

        userRepo.save(user);
        log.info("User account soft-deleted: {}", userId);
        return Map.of(
                "message", "Account Deleted successfully"
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "user-searches", allEntries = true)
    })
    @RoleRequired(UserRole.ADMIN)
    public Map<String, String> suspendAccount(UUID userId) {
        var currentUserId = getCurrentUser.getCurrentUserIdAndRole().userId();
        if (userId.equals(currentUserId)) {
            throw new IllegalStateException("You cannot suspend your own accounts");
        }

        return suspendOrRestoreUser(userId, true);


    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "user-searches", allEntries = true)
    })
    @RoleRequired(UserRole.ADMIN)
    public Map<String, String> restoreAccount(UUID userId) {

        return suspendOrRestoreUser(userId, false);


    }


    public User getAuthorizedUser(UUID userId) {
        return findUserId(userId, UserStatus.ACTIVE).orElseThrow(() -> new ResourceNotFoundException("Invalid User"));
    }

    @CustomCacheable(
            value = "userOrNull",
            key = "#userId",
            ttl = 600,
            timeUnit = TimeUnit.SECONDS
    )
    public Optional<User> findUserId(UUID userId, UserStatus userStatus) {
        return userRepo.findByIdAndStatus(userId, userStatus);
    }

    private Slice<User> searchUser(String searchWord, Pageable pageable) {
        var keyword = searchWord == null || searchWord.isBlank()
                ? null
                : searchWord.trim();

        return userRepo.searchUsers(keyword, pageable);
    }

    private @NonNull Map<String, String> suspendOrRestoreUser(UUID userId, boolean suspendUser) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid User"));
        

        boolean isDriver = user.getRole() == UserRole.DRIVER;
        UserStatus currentStatus = user.getStatus();


        if (suspendUser && currentStatus == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.SUSPENDED);
            user.setSuspendedAt(Instant.now());
            user.setSuspended(true);

            if (isDriver) {
                var driverProfile = driverProfileRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
                driverProfile.setActive(false);
                driverProfileRepository.save(driverProfile);
            }

            userRepo.save(user);
            return Map.of("message", "User suspended successfully");
        }


        if (!suspendUser && currentStatus == UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.ACTIVE);
            user.setSuspendedAt(null);
            user.setSuspended(false);

            if (isDriver) {
                var driverProfile = driverProfileRepository.findByUserId(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));
                driverProfile.setActive(true);
                driverProfileRepository.save(driverProfile);
            }

            userRepo.save(user);
            return Map.of("message", "User restored successfully");
        }


        return Map.of(
                "message", "Cannot execute command",
                "reason", "User is already " + currentStatus.toString().toLowerCase()
        );
    }


    public UserEntitiesResponse findAllByUserIds(UUID supervisorId, UUID currentUserId) {
        var listsUserIds = List.of(supervisorId, currentUserId);

        var users = findAll(listsUserIds, UserStatus.ACTIVE);

        var userIds = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity()
                ));

        var supervisor = Optional.ofNullable(
                userIds.get(supervisorId)
        ).orElseThrow(() ->
                new ResourceNotFoundException("Supervisor not found")
        );

        var currentUser = Optional.ofNullable(
                userIds.get(currentUserId)
        ).orElseThrow(() ->
                new ResourceNotFoundException("Invalid User")
        );


        return new UserEntitiesResponse(
                supervisor,
                currentUser
        );
    }


    @Transactional
    public void updateOnlineStatus(String userId, OnlineStatus onlineStatus) {
        userRepo.updateOnlineStatus(UUID.fromString(userId), onlineStatus);
    }

    @Transactional
    public List<User> findAll(List<UUID> userIds, UserStatus userStatus) {
        return userRepo.findAllByIdInAndStatus(userIds, userStatus);
    }


}
