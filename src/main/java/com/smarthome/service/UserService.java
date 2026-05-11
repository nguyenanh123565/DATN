package com.smarthome.service;

import com.smarthome.common.BusinessException;
import com.smarthome.common.ResourceNotFoundException;
import com.smarthome.dto.UserDto;
import com.smarthome.entity.Address;
import com.smarthome.entity.User;
import com.smarthome.repository.AddressRepository;
import com.smarthome.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepo;
    private final AddressRepository addressRepo;
    private final PasswordEncoder passwordEncoder;
    private final LoyaltyRankService loyaltyRankService;

    public UserService(UserRepository userRepo, AddressRepository addressRepo, PasswordEncoder passwordEncoder, LoyaltyRankService loyaltyRankService) {
        this.userRepo = userRepo;
        this.addressRepo = addressRepo;
        this.passwordEncoder = passwordEncoder;
        this.loyaltyRankService = loyaltyRankService;
    }

    // ─── Profile ──────────────────────────────────────────────────────

    public UserDto.Response getProfile(Long userId) {
        return toResponse(userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)));
    }

    @Transactional
    public UserDto.Response updateProfile(Long userId, UserDto.UpdateProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setFullName(req.getFullName());
        if (req.getPhone() != null)     user.setPhone(req.getPhone());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        return toResponse(userRepo.save(user));
    }

    @Transactional
    public void changePassword(Long userId, UserDto.ChangePasswordRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new BusinessException("Mật khẩu hiện tại không đúng!");

        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new BusinessException("Mật khẩu mới và xác nhận không khớp!");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }

    // ─── Admin: User management ────────────────────────────────────────

    public Page<UserDto.Response> getAllUsers(int page, int size, String role, String rank, String sortDir) {
        Sort sort = "asc".equalsIgnoreCase(sortDir) 
            ? Sort.by("createdAt").ascending() 
            : Sort.by("createdAt").descending();
            
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        // Logic lọc đơn giản (Có thể cải tiến bằng Specification nếu cần phức tạp hơn)
        if (role != null && !role.isBlank() && rank != null && !rank.isBlank()) {
            return userRepo.findByRoleAndLoyaltyRank(User.Role.valueOf(role), User.LoyaltyRank.valueOf(rank), pageRequest).map(this::toResponse);
        } else if (role != null && !role.isBlank()) {
            return userRepo.findByRole(User.Role.valueOf(role), pageRequest).map(this::toResponse);
        } else if (rank != null && !rank.isBlank()) {
            return userRepo.findByLoyaltyRank(User.LoyaltyRank.valueOf(rank), pageRequest).map(this::toResponse);
        }

        return userRepo.findAll(pageRequest).map(this::toResponse);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setStatus(User.Status.BANNED);
        userRepo.save(user);
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setStatus(User.Status.ACTIVE);
        userRepo.save(user);
    }

    // ─── Addresses ──────────────────────────────────────────────────────

    public List<UserDto.AddressResponse> getAddresses(Long userId) {
        return addressRepo.findByUserId(userId).stream().map(this::toAddressResponse).toList();
    }

    @Transactional
    public UserDto.AddressResponse addAddress(Long userId, UserDto.AddressRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Nếu là mặc định, bỏ mặc định các địa chỉ cũ
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            addressRepo.findByUserId(userId).forEach(a -> { a.setIsDefault(false); addressRepo.save(a); });
        }

        Address address = new Address();
        address.setUser(user);
        address.setName(req.getName());
        address.setPhone(req.getPhone());
        address.setFullAddress(req.getFullAddress());
        address.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()));
        return toAddressResponse(addressRepo.save(address));
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepo.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ", addressId));
        if (!address.getUser().getId().equals(userId))
            throw new BusinessException("Bạn không có quyền xoá địa chỉ này!");
        addressRepo.delete(address);
    }

    // ─── Mappers ──────────────────────────────────────────────────────

    private UserDto.Response toResponse(User u) {
        UserDto.Response res = new UserDto.Response();
        res.setId(u.getId()); res.setEmail(u.getEmail());
        res.setFullName(u.getFullName()); res.setPhone(u.getPhone());
        res.setAvatarUrl(u.getAvatarUrl());
        res.setRole(u.getRole().name()); res.setStatus(u.getStatus().name());
        res.setCreatedAt(u.getCreatedAt());
        // Loyalty Rank
        User.LoyaltyRank rank = u.getLoyaltyRank() != null ? u.getLoyaltyRank() : User.LoyaltyRank.NORMAL;
        res.setLoyaltyRank(rank.name());
        res.setTotalSpent(u.getTotalSpent() != null ? u.getTotalSpent() : java.math.BigDecimal.ZERO);
        res.setLoyaltyDiscountRate(loyaltyRankService.getDiscountRate(rank));
        // Ngưỡng hạng kế tiếp
        java.math.BigDecimal nextThreshold = switch (rank) {
            case NORMAL -> loyaltyRankService.getSilverThreshold();
            case SILVER -> loyaltyRankService.getGoldThreshold();
            case GOLD   -> loyaltyRankService.getVipThreshold();
            case VIP    -> null; // Đã đạt hạng cao nhất
        };
        res.setNextRankThreshold(nextThreshold);
        return res;
    }

    private UserDto.AddressResponse toAddressResponse(Address a) {
        UserDto.AddressResponse res = new UserDto.AddressResponse();
        res.setId(a.getId()); res.setName(a.getName()); res.setPhone(a.getPhone());
        res.setFullAddress(a.getFullAddress());
        res.setIsDefault(a.getIsDefault());
        return res;
    }
}
