package tn.esprit.tic.civiAgora.auth;


import org.springframework.security.authentication.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tn.esprit.tic.civiAgora.dao.entity.enums.Role;
import tn.esprit.tic.civiAgora.dao.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.tic.civiAgora.config.JwtService;
import tn.esprit.tic.civiAgora.dao.entity.Token;
import tn.esprit.tic.civiAgora.dao.entity.enums.TokenType;
import tn.esprit.tic.civiAgora.dao.repository.TokenRepository;
import tn.esprit.tic.civiAgora.dao.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dto.usersDto.UserProfileDto;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AuthenticationService  {

    private final UserRepository repository;
    private final TokenRepository tokenRepository;
//    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;



    public AuthenticationResponse register(RegisterRequest request)  {
        // 1 Fast existence check (no exception, no extra query later)
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already in use");
            // or: throw new EmailAlreadyExistsException("Email already in use");
        }
        // 2 Create user
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .birthDate(request.getBirthDate())
                .enabled(true)
                .archived(false)
                .password(bCryptPasswordEncoder.encode(request.getPassword()))
                .role(Role.CITIZEN)
                .build();
        var savedUser = repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        saveUserToken(user, jwtToken);

        UserProfileDto userProfile = UserProfileDto.builder()
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .userProfileDto(userProfile)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. Fetch User by Email first to check status flags
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // 2. Check Custom Conditions: user and organization state
        if (user.getOrganization() != null) {
            if (user.getOrganization().getStatus() == null || user.getOrganization().getStatus() != tn.esprit.tic.civiAgora.dao.entity.enums.OrganizationStatus.ACTIVE) {
                throw new DisabledException("Your organization is not active or not yet approved.");
            }
        }

        // Condition A: Not enabled AND Not archived -> "Organization not available"
        if (!user.isEnabled() && !user.getArchived()) {
            throw new DisabledException("Your organization is not available now");
        }

        // Condition B: Enabled AND Archived -> "Account suspended"
        if (user.isEnabled() && user.getArchived()) {
            throw new LockedException("Your account is suspended contact the support");
        }

        // (Optional) You might want to handle !enabled && archived here too,
        // otherwise AuthenticationManager might throw a generic "User is disabled" error.

        // 3. Verify Password using AuthenticationManager
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 4. Generate Tokens
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        UserProfileDto userProfile = UserProfileDto.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .userProfileDto(userProfile)
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null ||!authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }
}