package com.hedgecourt.spring.lib.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Run these two commands to create private/public key pair:
 *
 * <pre>
 * openssl genrsa -out private.pem 2048
 * openssl rsa -in private.pem -outform PEM -pubout -out public.pem
 * </pre>
 */
@Service
public class HcJwtService {
  private static final Logger log = LoggerFactory.getLogger(HcJwtService.class);

  public enum JwtPart {
    HEADER,
    PAYLOAD,
    SIGNATURE
  }

  public static final String JWT_CLAIM_HC_ENV = "hc/env";

  @Autowired private ResourceLoader resourceLoader;

  @Value("${hc.jwt.auth-enabled:false}")
  private boolean authEnabled;

  @Value("${hc.jwt.private-key-resource}")
  private Resource jwtPrivateKeyResource;

  @Value("${hc.jwt.public-key-resource}")
  private Resource jwtPublicKeyResource;

  @Value("${hc.jwt.expiry-millis}")
  private long jwtExpiryMillis;

  @Value("${hc.jwt.generate-keys}")
  private boolean generateKeys;

  @Value("${hc.env}")
  private String hcEnv;

  private PrivateKey jwtSigningKey = null;
  private PublicKey jwtVerificationKey = null;
  private String jwtVerificationPem = null;

  private boolean needToLoadJwtSigningKey = true;
  private boolean needToLoadJwtVerificationKey = true;

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    if (needToLoadJwtVerificationKey) loadPublicKey();

    try {
      return Jwts.parser()
          // TODO .requireXXX for stuff like Issuer, hcEnv, etc
          .verifyWith(jwtVerificationKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (SignatureException e) {
      if (log.isErrorEnabled()) log.error("Invalid JWT signature", e);
      throw new IllegalArgumentException("Invalid JWT signature", e);
    } catch (Exception e) {
      if (log.isErrorEnabled()) log.error("Failed to decode JWT", e);
      throw new IllegalArgumentException("Failed to decode JWT", e);
    }
  }

  private RawAndDecodedResource readBase64DecodedKey(Resource keyResource) throws IOException {
    if (log.isDebugEnabled()) log.debug("readBase64DecodedKey({})", keyResource.getFilename());

    String rawKey = null;

    try (var inputStream = keyResource.getInputStream()) {
      rawKey = new String(inputStream.readAllBytes());
    }

    return new RawAndDecodedResource(
        rawKey,
        Base64.getDecoder()
            .decode(
                rawKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")));
  }

  private synchronized void loadPrivateKey() {
    if (!authEnabled) {
      if (log.isErrorEnabled()) log.error("Jwt Auth is not enabled, loadPrivateKey() exiting");
      return;
    }

    if (!needToLoadJwtSigningKey) {
      if (log.isDebugEnabled())
        log.debug("jwtSigningKey does not need to be loaded, skipping readPrivateKey()");
      return;
    }

    if (generateKeys) {
      generateTestKeys();
      return;
    }

    try {
      if (log.isInfoEnabled()) log.info("loading jwt private key");
      jwtSigningKey =
          KeyFactory.getInstance("RSA")
              .generatePrivate(
                  new PKCS8EncodedKeySpec(
                      readBase64DecodedKey(jwtPrivateKeyResource).decodedBytes));
      if (log.isInfoEnabled())
        log.info("loaded jwt private key, algorithm={}", jwtSigningKey.getAlgorithm());
    } catch (IOException ex) {
      log.error("Error loading jwt private key", ex);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Error with private key algorithm", ex);
    } catch (InvalidKeySpecException ex) {
      log.error("Error with private key spec", ex);
    } finally {
      // whether success or failure, don't keep loading this.  errors will fail downstream.
      needToLoadJwtSigningKey = false;
    }
  }

  private synchronized void loadPublicKey() {
    if (!needToLoadJwtVerificationKey) {
      if (log.isDebugEnabled())
        log.debug("jwtVerificationKey does not need to be loaded, skipping readPublicKey()");
      return;
    }

    if (generateKeys) {
      generateTestKeys();
      return;
    }

    try {
      if (log.isInfoEnabled()) log.info("loading jwt public key");
      RawAndDecodedResource publicKeyInfo = readBase64DecodedKey(jwtPublicKeyResource);

      jwtVerificationKey =
          KeyFactory.getInstance("RSA")
              .generatePublic(new X509EncodedKeySpec(publicKeyInfo.decodedBytes));

      jwtVerificationPem = publicKeyInfo.rawString;

      if (log.isInfoEnabled())
        log.info("loaded jwt public key, algorithm={}", jwtVerificationKey.getAlgorithm());

    } catch (IOException ex) {
      log.error("Error loading jwt public key", ex);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Error with jwt public key algorithm", ex);
    } catch (InvalidKeySpecException ex) {
      log.error("Error with jwt public key spec", ex);
    } finally {
      // whether success or failure, don't keep loading this. errors will fail downstream.
      needToLoadJwtVerificationKey = false;
    }
  }

  public String getJwtVerificationPem() {
    if (needToLoadJwtVerificationKey) loadPublicKey();
    return jwtVerificationPem;
  }

  /** Generate throw-away keys for testing. */
  private synchronized void generateTestKeys() {
    try {
      if (log.isInfoEnabled()) log.info("Generating RSA JWT-signing key pair for testing");

      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      jwtSigningKey = keyPair.getPrivate();
      jwtVerificationKey = keyPair.getPublic();
      jwtVerificationPem = "UNIMPLEMENTED PEM FOR GENERATED KEY";

    } catch (NoSuchAlgorithmException ex) {
      if (log.isErrorEnabled()) log.error("Error generating RSA key pair for testing", ex);
      throw new IllegalStateException("Cannot generate RSA keys for testing", ex);
    } finally {
      needToLoadJwtVerificationKey = false;
      needToLoadJwtSigningKey = false;
    }
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(userDetails, new HashMap<>());
  }

  public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
    return buildToken(userDetails, extraClaims);
  }

  private String buildToken(UserDetails userDetails, Map<String, Object> extraClaims) {
    if (!authEnabled) {
      if (log.isErrorEnabled()) log.error("Jwt Auth is not enabled, buildToken() exiting");
      return null;
    }

    if (needToLoadJwtSigningKey) loadPrivateKey();

    Date issuedAt = new Date();
    Date expiresAt = new Date(System.currentTimeMillis() + jwtExpiryMillis);

    if (extraClaims.containsKey("issuedAt")) {
      if (log.isDebugEnabled())
        log.debug("extra claims include 'issuedAt', using {}", extraClaims.get("issuedAd"));
      issuedAt = new Date((Long) extraClaims.remove("issuedAt"));
    }

    if (extraClaims.containsKey("expiresAt")) {
      if (log.isDebugEnabled())
        log.debug("extra claims include 'expiresAt', using {}", extraClaims.get("expiresAt"));
      expiresAt = new Date((Long) extraClaims.remove("expiresAt"));
    }

    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .issuedAt(issuedAt)
        .expiration(expiresAt)
        .claim(JWT_CLAIM_HC_ENV, hcEnv)
        .subject(userDetails.getUsername())
        .claim(
            "authorities",
            userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
        .claims(extraClaims)
        .signWith(jwtSigningKey)
        .compact();
  }

  @Data
  @AllArgsConstructor
  static class RawAndDecodedResource {
    private String rawString;
    private byte[] decodedBytes;
  }
}
