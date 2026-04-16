package tn.esprit.tic.civiAgora.controller.back_office;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.organizationDto.UserToOrganizationDto;
import tn.esprit.tic.civiAgora.dto.usersDto.TenantUserRequest;
import tn.esprit.tic.civiAgora.mappers.organizationMappers.UserToOrganizationMapper;
import tn.esprit.tic.civiAgora.service.RbacService;
import tn.esprit.tic.civiAgora.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/org/{organizationId}/users")
@RequiredArgsConstructor
public class TenantUserManagementController {

    private final UserService userService;
    private final RbacService rbacService;
    private final UserToOrganizationMapper userMapper;

    @GetMapping
    public ResponseEntity<List<UserToOrganizationDto>> getUsers(
            @PathVariable("organizationId") Integer organizationId
    ) {
        rbacService.requireTenantUserManagementAccess(organizationId);

        List<UserToOrganizationDto> users = userService.getTenantUsers(organizationId)
                .stream()
                .map(userMapper::toUserToOrganizationDto)
                .toList();

        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @PathVariable("organizationId") Integer organizationId,
            @RequestBody TenantUserRequest request
    ) {
        rbacService.requireTenantUserManagementAccess(organizationId);

        try {
            User actor = rbacService.getCurrentUserOrThrow();
            User created = userService.createTenantUser(organizationId, request, actor);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(userMapper.toUserToOrganizationDto(created));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{userId}/archive")
    public ResponseEntity<UserToOrganizationDto> setArchived(
            @PathVariable("organizationId") Integer organizationId,
            @PathVariable("userId") Integer userId,
            @RequestParam("archived") Boolean archived
    ) {
        rbacService.requireTenantUserManagementAccess(organizationId);
        User actor = rbacService.getCurrentUserOrThrow();
        User updated = userService.setTenantUserArchived(organizationId, userId, archived, actor);
        return ResponseEntity.ok(userMapper.toUserToOrganizationDto(updated));
    }
}
