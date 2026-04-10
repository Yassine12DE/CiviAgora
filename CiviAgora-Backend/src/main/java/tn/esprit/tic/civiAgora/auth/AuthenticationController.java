package tn.esprit.tic.civiAgora.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import tn.esprit.tic.civiAgora.dao.entity.Organization;
import tn.esprit.tic.civiAgora.dao.entity.PasswordResetToken;
import tn.esprit.tic.civiAgora.dao.entity.User;
import tn.esprit.tic.civiAgora.dto.usersDto.CurrentUserProfileDto;
import tn.esprit.tic.civiAgora.dto.usersDto.UpdateCurrentUserProfileRequest;
import tn.esprit.tic.civiAgora.service.EmailService;
import tn.esprit.tic.civiAgora.service.TenantAccessService;
import tn.esprit.tic.civiAgora.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth/")
@RequiredArgsConstructor
public class AuthenticationController {
    @Autowired
    private  AuthenticationService service;
    @Autowired
    private UserService userService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private TenantAccessService tenantAccessService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            AuthenticationResponse response = service.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Server error. Please try again later."
                    ));
        }
    }

    @PostMapping("login")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            return ResponseEntity.ok(service.authenticate(request));
        } catch (DisabledException | LockedException e) {
            // Returns 403 Forbidden with the specific message (Org unavailable OR Suspended)
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (BadCredentialsException e) {
            // Returns 401 Unauthorized for wrong email/password
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred during login"));
        }
    }

    @PostMapping("saas-login")
    public ResponseEntity<?> authenticateSaas(@RequestBody AuthenticationRequest request) {
        try {
            return ResponseEntity.ok(service.authenticateSaas(request));
        } catch (DisabledException | LockedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred during SaaS login"));
        }
    }

    @PostMapping("refresh-token")
    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        service.refreshToken(request, response);
    }

    @GetMapping("verifyMail/{email}")
    public ResponseEntity<Boolean> verifyemail(@PathVariable("email")String email){
        try {
            userService.getUserByEmail(email);
            return new ResponseEntity<>(true,HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(false, HttpStatus.OK);
        }
    }

    @GetMapping("forgot-password/{email}")
    public ResponseEntity<?> sendResetMail(@PathVariable("email") String email) {
        try {
            // 1️⃣ Get user by email
            User user = userService.getUserByEmail(email);

            // 2️⃣ Create a password reset token (valid 15 min)
            PasswordResetToken resetToken = userService.createPasswordResetToken(user);

            // 3️⃣ Build the reset link
            String resetLink = "http://localhost:5173/reset-password?token=" + resetToken.getToken();


            // 4️⃣ Generate email content
            String htmlContent = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Password Reset</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            background-color: #eef1f5;
            font-family: Arial, sans-serif;
        }
        .wrapper {
            width: 100%;
            padding: 40px 0;
            background-color: #eef1f5;
        }
        .email-container {
            max-width: 600px;
            margin: auto;
            background-color: #ffffff;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 8px 30px rgba(0,0,0,0.12);
        }
        .header {
            background-color: #4CAF50;
            padding: 30px;
            text-align: center;
            color: #ffffff;
        }
        .content {
            padding: 30px;
            color: #444;
            font-size: 14px;
            line-height: 1.6;
        }
        .highlight {
            background-color: #f4f8f5;
            padding: 12px;
            border-left: 4px solid #4CAF50;
            margin: 20px 0;
            font-size: 14px;
        }
        .btn {
            display: inline-block;
            margin: 25px 0;
            padding: 14px 26px;
            background-color: #4CAF50;
            color: #ffffff !important;
            text-decoration: none;
            border-radius: 30px;
            font-weight: bold;
        }
        .footer {
            background-color: #fafafa;
            padding: 15px;
            text-align: center;
            font-size: 12px;
            color: #888;
        }
    </style>
</head>

<body>
<div class="wrapper">
    <div class="email-container">

        <div class="header">
            <h2>CiviAgora</h2>
            <p>Secure Account Management</p>
        </div>

        <div class="content">
            <p>Hello,</p>

            <p>You requested to reset the password for the account associated with:</p>

            <div class="highlight">
                <strong>{{EMAIL}}</strong>
            </div>

            <p>This link is valid for <strong>15 minutes</strong>.</p>

            <p style="text-align:center;">
                <a href="{{LINK}}" class="btn">Reset Your Password</a>
            </p>

            

            <p style="font-size:13px;color:#777;">
                If you did not request this, you can safely ignore this email.
            </p>
        </div>

        <div class="footer">
            &copy; 2026 CiviAgora — All rights reserved.
        </div>

    </div>
</div>
</body>
</html>
""";

// SAFE replacements (NO %s, NO formatting crashes)
            htmlContent = htmlContent
                    .replace("{{EMAIL}}", user.getEmail())
                    .replace("{{LINK}}", resetLink);

            // 5️⃣ Send the email
            emailService.sendHtmlMessage(user.getEmail(), "Password Reset Request - CiviAgora App", htmlContent);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    @PostMapping("reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword
    ) {
        try {
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Password reset successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        tenantAccessService.assertTenantMatchOrThrow();
        return ResponseEntity.ok(toCurrentUserProfileDto(user));
    }

    @PutMapping("me")
    public ResponseEntity<?> updateMe(
            Authentication authentication,
            @RequestBody UpdateCurrentUserProfileRequest request
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized"));
        }

        User updatedUser;
        if (tenantAccessService.isCurrentUserSuperAdmin()) {
            updatedUser = userService.updateCurrentSuperAdminProfile(user.getId(), request);
        } else {
            Integer organizationId = tenantAccessService.getCurrentOrganizationEntityOrThrow().getId();
            updatedUser = userService.updateCurrentUserProfile(user.getId(), organizationId, request);
        }

        return ResponseEntity.ok(toCurrentUserProfileDto(updatedUser));
    }

    private CurrentUserProfileDto toCurrentUserProfileDto(User user) {
        Organization organization = tenantAccessService.getCurrentOrganizationEntityOrThrow();

        return CurrentUserProfileDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .birthDate(user.getBirthDate())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .enabled(user.getEnabled())
                .archived(user.getArchived())
                .accountStatus(resolveAccountStatus(user))
                .organizationId(organization.getId())
                .organizationSlug(organization.getSlug())
                .organizationName(organization.getName())
                .build();
    }

    private String resolveAccountStatus(User user) {
        if (Boolean.TRUE.equals(user.getArchived())) {
            return "ARCHIVED";
        }
        if (Boolean.TRUE.equals(user.getEnabled())) {
            return "ACTIVE";
        }
        return "DISABLED";
    }

}
