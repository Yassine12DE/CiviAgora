package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasUserCreateRequest;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasUserResetPasswordRequest;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasUserUpdateRequest;
import tn.esprit.tic.civiAgora.dto.usersDto.BackOfficeSAASUserDto;
import tn.esprit.tic.civiAgora.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/saas/users")
@RequiredArgsConstructor
public class SAASController {

    private final UserService service;

    @GetMapping
    public ResponseEntity<List<BackOfficeSAASUserDto>> getBackOfficeSaasUsers() {
        return ResponseEntity.ok(service.getBackOfficeSaasUsers());
    }

    @PostMapping
    public ResponseEntity<BackOfficeSAASUserDto> createUser(@RequestBody SaasUserCreateRequest request) {
        return ResponseEntity.ok(service.createSaasUser(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BackOfficeSAASUserDto> updateUser(
            @PathVariable("id") Integer id,
            @RequestBody SaasUserUpdateRequest request
    ) {
        return ResponseEntity.ok(service.updateSaasUser(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<BackOfficeSAASUserDto> archiveUser(
            @PathVariable("id") Integer id,
            @RequestParam("archived") boolean archived
    ) {
        return ResponseEntity.ok(service.archiveSaasUser(id, archived));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<BackOfficeSAASUserDto> resetPassword(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) SaasUserResetPasswordRequest request
    ) {
        String newPassword = request == null ? null : request.getNewPassword();
        return ResponseEntity.ok(service.resetSaasUserPassword(id, newPassword));
    }
}
