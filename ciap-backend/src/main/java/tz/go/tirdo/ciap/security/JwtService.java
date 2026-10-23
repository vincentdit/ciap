package tz.go.tirdo.ciap.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tz.go.tirdo.ciap.domain.user.UserRepository;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Issues and validates JWT access/refresh tokens.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final UserRepository userRepository;
    private final SecretKey key;

    public JwtService(JwtProperties props, UserRepository userRepository) {
        this.props = props;
        this.userRepository = userRepository;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }

    public String generateAccessToken(UserDetails user, List<String> roles, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getUsername())
                .claim("roles", roles)
                .claim("type", "access")
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.accessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UserDetails user, String tokenId, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())
                .subject(user.getUsername())
                .id(tokenId)
                .claim("type", "refresh")
                .claim("ver", tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.refreshTokenTtlDays(), ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isValid(String token, UserDetails user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isExpired(token);
    }

    /**
     * Access tokens only. Refresh tokens, expired tokens, disabled accounts,
     * and tokens issued before logout are rejected.
     */
    public boolean isCurrentAccessToken(String token, UserDetails user) {
        try {
            if (!"access".equals(extractTokenType(token)) || !user.isEnabled() || !isValid(token, user)) {
                return false;
            }
            Number version = extractClaim(token, claims -> claims.get("ver", Number.class));
            int current = userRepository.findByEmail(user.getUsername())
                    .map(account -> account.getTokenVersion())
                    .orElse(-1);
            return version != null && version.intValue() == current;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Integer extractTokenVersion(String token) {
        Number version = extractClaim(token, claims -> claims.get("ver", Number.class));
        return version == null ? null : version.intValue();
    }

    /** Returns the custom "type" claim ("access" or "refresh"). */
    public String extractTokenType(String token) {
        return extractClaim(token, c -> c.get("type", String.class));
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    public long getAccessTokenTtlMinutes() {
        return props.accessTokenTtlMinutes();
    }
}
