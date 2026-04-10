package tn.esprit.tic.civiAgora.service;

import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.OrganizationRequest;
import tn.esprit.tic.civiAgora.dao.entity.PasswordResetToken;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dao.repository.OrganizationRepository;
import tn.esprit.tic.civiAgora.dao.repository.PasswordResetTokenRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import tn.esprit.tic.civiAgora.dto.usersDto.BackOfficeSAASUserDto;
import tn.esprit.tic.civiAgora.dto.usersDto.TenantUserRequest;
import tn.esprit.tic.civiAgora.dto.usersDto.UpdateCurrentUserProfileRequest;
import tn.esprit.tic.civiAgora.dto.usersDto.UserProfileDto;
import tn.esprit.tic.civiAgora.mappers.usersMapper.BackOfficeSAASUserMapper;
import tn.esprit.tic.civiAgora.mappers.usersMapper.UserProfileMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    // 🔧 CHANGED: variable naming (camelCase)
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private  OrganizationRepository organizationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    /* ========================= USERS ========================= */

    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserProfileMapper::getUserProfileDto)
                .toList();
    }

    // Get BackOffice SAAS Users with specific roles
    public List<BackOfficeSAASUserDto> getBackOfficeSaasUsers() {
        return userRepository.findAll()
                .stream()
                .map(BackOfficeSAASUserMapper::toBackOfficeSAASUserDto)
                .filter(dto -> dto != null)
                .toList();
    }

    public User getUserById(Integer id) throws Exception {
        return userRepository.findById(id)
                .orElseThrow(() -> new Exception("User with ID " + id + " not found"));
    }

    public User getUserByEmail(String email) throws Exception {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("User with email " + email + " not found"));
    }

    public List<User> getUsersByArchived(Boolean archived) {
        return userRepository.findByArchived(archived);
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User updateUser(User user) throws Exception {
        User existingUser = getUserById(user.getId());

        // 🔧 CHANGED: password update logic (SAFE)
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            // only encode if password is new (not already encoded)
            if (!passwordEncoder.matches(user.getPassword(), existingUser.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }
        } else {
            // keep old password if empty
            user.setPassword(existingUser.getPassword());
        }

        return userRepository.save(user);
    }

    public User updateCurrentUserProfile(Integer userId, Integer organizationId, UpdateCurrentUserProfileRequest request) {
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new RuntimeException("Current user profile not found"));

        applyCurrentProfileUpdate(user, request);
        return userRepository.save(user);
    }

    public User updateCurrentSuperAdminProfile(Integer userId, UpdateCurrentUserProfileRequest request) {
        User user = userRepository.findById(userId)
                .filter(candidate -> candidate.getRole() == Role.SUPER_ADMIN)
                .orElseThrow(() -> new RuntimeException("Current super admin profile not found"));

        applyCurrentProfileUpdate(user, request);
        return userRepository.save(user);
    }

    private void applyCurrentProfileUpdate(User user, UpdateCurrentUserProfileRequest request) {
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setBirthDate(request.getBirthDate());
    }

    public List<User> getTenantUsers(Integer organizationId) {
        return userRepository.findByOrganizationId(organizationId);
    }

    public User createTenantUser(Integer organizationId, TenantUserRequest request, User actor) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already in use");
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        Role requestedRole = resolveTenantAssignableRole(request.getRole());

        if (!canCreateRole(actor, requestedRole)) {
            throw new AccessDeniedException("This role cannot create the requested user role");
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .enabled(true)
                .archived(false)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(requestedRole)
                .organization(organization)
                .build();

        User savedUser = userRepository.save(user);
        updateUsersCount(organizationId);
        return savedUser;
    }

    public User setTenantUserArchived(Integer organizationId, Integer userId, Boolean archived, User actor) {
        User target = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new RuntimeException("User not found in this organization"));

        if (actor.getId().equals(target.getId())) {
            throw new AccessDeniedException("You cannot archive your own account from user management");
        }

        if (!canManageTargetUser(actor, target)) {
            throw new AccessDeniedException("This role cannot manage the selected user");
        }

        target.setArchived(Boolean.TRUE.equals(archived));
        return userRepository.save(target);
    }

    public void deleteUser(Integer id) throws Exception {
        getUserById(id); // check existence
        userRepository.deleteById(id);
    }

    public User setArchive(Integer id, Boolean archived) throws Exception {
        User user = getUserById(id);
        user.setArchived(archived);
        return userRepository.save(user);
    }

    /* ========================= PASSWORD RESET ========================= */

    public PasswordResetToken createPasswordResetToken(User user) {

        // 🔧 CHANGED: delete old token if exists
        passwordResetTokenRepository.findByUser(user)
                .ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        return passwordResetTokenRepository.save(resetToken);
    }

    public User validatePasswordResetToken(String token) throws Exception {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new Exception("Invalid password reset token"));

        if (resetToken.isExpired()) {
            throw new Exception("Token expired");
        }

        return resetToken.getUser();
    }

    public void resetPassword(String token, String newPassword) throws Exception {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new Exception("Invalid password reset token"));

        if (resetToken.isExpired()) {
            throw new Exception("Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 🔧 CHANGED: invalidate token after use
        passwordResetTokenRepository.delete(resetToken);
    }
    public User addUserToOrganization(Integer userId, Integer organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        user.setOrganization(org); // <-- assign the organization

        userRepository.save(user);

        // Update usersCount
        updateUsersCount(organizationId);

        return user;
    }
    public void updateUsersCount(Integer organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        int count = userRepository.countByOrganizationId(organizationId);
        org.setUsersCount(count);
        organizationRepository.save(org);
    }

    private Role resolveTenantAssignableRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return Role.CITIZEN;
        }

        Role role = Role.valueOf(rawRole.trim().toUpperCase());
        return switch (role) {
            case ADMIN, MANAGER, MODERATOR, CITIZEN -> role;
            case SUPER_ADMIN, OBSERVER -> throw new AccessDeniedException("This role cannot be assigned in tenant user management");
        };
    }

    private boolean canCreateRole(User actor, Role requestedRole) {
        if (actor == null || actor.getRole() == null || requestedRole == null) {
            return false;
        }
        if (actor.getRole() == Role.SUPER_ADMIN || actor.getRole() == Role.ADMIN) {
            return requestedRole != Role.SUPER_ADMIN;
        }
        if (actor.getRole() == Role.MANAGER) {
            return requestedRole == Role.CITIZEN;
        }
        return false;
    }

    private boolean canManageTargetUser(User actor, User target) {
        if (actor == null || target == null || actor.getRole() == null || target.getRole() == null) {
            return false;
        }
        if (actor.getRole() == Role.SUPER_ADMIN) {
            return true;
        }
        if (actor.getRole() == Role.ADMIN) {
            return target.getRole() != Role.SUPER_ADMIN;
        }
        if (actor.getRole() == Role.MANAGER) {
            return target.getRole() == Role.CITIZEN;
        }
        return false;
    }

    public void createInitialAdminForOrganization(Organization organization, OrganizationRequest request) {
        if (organization == null || request == null || request.getContactEmail() == null) {
            return;
        }

        if (userRepository.existsByEmail(request.getContactEmail())) {
            return; // user already exists, do not overwrite
        }

        String[] nameParts = request.getContactPersonName() != null ? request.getContactPersonName().split(" ", 2) : new String[]{"Admin", ""};

        User user = User.builder()
                .firstName(nameParts[0])
                .lastName(nameParts.length > 1 ? nameParts[1] : "")
                .email(request.getContactEmail())
                .phone(request.getPhone())
                .enabled(true)
                .archived(false)
                .password(passwordEncoder.encode("TempPass@123"))
                .role(Role.ADMIN)
                .organization(organization)
                .build();

        userRepository.save(user);
        updateUsersCount(organization.getId());
    }
}
