package tn.esprit.tic.civiAgora.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tn.esprit.tic.civiAgora.dao.entity.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private String secretKey ="442A472D4B6150645367566B59703373367639792442264528482B4D62516554";

    private long jwtExpiration =1000*24*60*60 ;
    private long refreshExpiration=1000*15*24*60;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        enrichClaimsWithUserContext(extraClaims, userDetails);
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(
            UserDetails userDetails
    ) {
        Map<String, Object> claims = new HashMap<>();
        enrichClaimsWithUserContext(claims, userDetails);
        return buildToken(claims, userDetails, refreshExpiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void enrichClaimsWithUserContext(Map<String, Object> claims, UserDetails userDetails) {
        claims.put("role", userDetails.getAuthorities().toArray()[0].toString());

        if (userDetails instanceof User user) {
            claims.put("userId", user.getId());
            claims.put("email", user.getEmail());
            claims.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
            claims.put("organizationSlug", user.getOrganization() != null ? user.getOrganization().getSlug() : null);
        }
    }

    public String extractOrganizationSlug(String token) {
        return extractClaim(token, claims -> claims.get("organizationSlug", String.class));
    }

    public Integer extractOrganizationId(String token) {
        return extractClaim(token, claims -> claims.get("organizationId", Integer.class));
    }
}
