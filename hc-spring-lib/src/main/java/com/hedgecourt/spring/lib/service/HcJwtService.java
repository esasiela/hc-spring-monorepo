package com.hedgecourt.spring.lib.service;

import com.hedgecourt.spring.lib.dto.JwkDto;
import com.hedgecourt.spring.lib.dto.JwksDto;
import com.hedgecourt.spring.lib.error.JwtSigningException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  @Autowired private ResourceLoader resourceLoader;

  @Value("${hc.jwt.auth-enabled:false}")
  private boolean authEnabled;

  @Value("${hc.jwt.private-key-resource:classpath\\:jwt/private.pem}")
  private Resource jwtPrivateKeyResource;

  @Value("${hc.jwt.public-key-resource:classpath\\:jwt/public.pem}")
  private Resource jwtPublicKeyResource;

  @Value("${hc.jwt.expiry-millis:86400000}")
  private long jwtExpiryMillis;

  @Value("${hc.jwt.generate-keys:true}")
  private boolean generateKeys;

  @Value("${hc.jwt.issuer:hedge-court-apps}")
  private String issuer;

  @Value("${hc.jwt.key-id:hc-apps}")
  private String keyId;

  @Value("${hc.env}")
  private String hcEnv;

  private PrivateKey privateKey = null;
  private PublicKey publicKey = null;
  private String publicKeyPem = null;

  private boolean needToLoadPrivateKey = true;
  private boolean needToLoadPublicKey = true;

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

    if (!needToLoadPrivateKey) {
      if (log.isDebugEnabled())
        log.debug("privateKey does not need to be loaded, skipping readPrivateKey()");
      return;
    }

    if (generateKeys) {
      generateTestKeys();
      return;
    }

    try {
      if (log.isInfoEnabled()) log.info("loading jwt private key");
      privateKey =
          KeyFactory.getInstance("RSA")
              .generatePrivate(
                  new PKCS8EncodedKeySpec(
                      readBase64DecodedKey(jwtPrivateKeyResource).decodedBytes));
      if (log.isInfoEnabled())
        log.info("loaded jwt private key, algorithm={}", privateKey.getAlgorithm());
    } catch (IOException ex) {
      log.error("Error loading jwt private key", ex);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Error with private key algorithm", ex);
    } catch (InvalidKeySpecException ex) {
      log.error("Error with private key spec", ex);
    } finally {
      // whether success or failure, don't keep loading this.  errors will fail downstream.
      needToLoadPrivateKey = false;
    }
  }

  private synchronized void loadPublicKey() {
    if (!authEnabled) {
      if (log.isErrorEnabled()) log.error("Jwt Auth is not enabled, loadPublicKey() exiting");
      return;
    }

    if (!needToLoadPublicKey) {
      if (log.isDebugEnabled())
        log.debug("publicKey does not need to be loaded, skipping readPublicKey()");
      return;
    }

    if (generateKeys) {
      generateTestKeys();
      return;
    }

    try {
      if (log.isInfoEnabled()) log.info("loading jwt public key");
      RawAndDecodedResource publicKeyInfo = readBase64DecodedKey(jwtPublicKeyResource);

      publicKey =
          KeyFactory.getInstance("RSA")
              .generatePublic(new X509EncodedKeySpec(publicKeyInfo.decodedBytes));

      publicKeyPem = publicKeyInfo.rawString;

      if (log.isInfoEnabled())
        log.info("loaded jwt public key, algorithm={}", publicKey.getAlgorithm());

    } catch (IOException ex) {
      log.error("Error loading jwt public key", ex);
    } catch (NoSuchAlgorithmException ex) {
      log.error("Error with jwt public key algorithm", ex);
    } catch (InvalidKeySpecException ex) {
      log.error("Error with jwt public key spec", ex);
    } finally {
      // whether success or failure, don't keep loading this. errors will fail downstream.
      needToLoadPublicKey = false;
    }
  }

  public JwksDto getJwks() {
    loadPublicKey();

    JwksDto jwksDto = new JwksDto();

    jwksDto.addJwk(
        JwkDto.builder()
            .kty("RSA")
            .alg("RS256")
            .use("sig")
            .kid(keyId)
            .n(toBase64Url(((RSAPublicKey) publicKey).getModulus()))
            .e(toBase64Url(((RSAPublicKey) publicKey).getPublicExponent()))
            .build());

    return jwksDto;
  }

  private String toBase64Url(BigInteger value) {
    byte[] byteArray = value.toByteArray();
    return Base64.getUrlEncoder().withoutPadding().encodeToString(byteArray);
  }

  public String getPublicKeyPem() {
    if (needToLoadPublicKey) loadPublicKey();
    return publicKeyPem;
  }

  /** Generate throw-away keys for testing. */
  private synchronized void generateTestKeys() {
    try {
      if (log.isInfoEnabled()) log.info("Generating RSA JWT-signing key pair for testing");

      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();

      privateKey = keyPair.getPrivate();
      publicKey = keyPair.getPublic();
      publicKeyPem = "UNIMPLEMENTED PEM FOR GENERATED KEY";

    } catch (NoSuchAlgorithmException ex) {
      if (log.isErrorEnabled()) log.error("Error generating RSA key pair for testing", ex);
      throw new IllegalStateException("Cannot generate RSA keys for testing", ex);
    } finally {
      needToLoadPublicKey = false;
      needToLoadPrivateKey = false;
    }
  }

  public String generateToken(UserDetails userDetails) throws JwtSigningException {
    return generateToken(userDetails, new HashMap<>());
  }

  public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims)
      throws JwtSigningException {
    return buildToken(userDetails, extraClaims);
  }

  private String buildToken(UserDetails userDetails, Map<String, Object> extraClaims)
      throws JwtSigningException {

    if (needToLoadPrivateKey) loadPrivateKey();

    Date issuedAt = new Date();
    Date expiresAt = new Date(System.currentTimeMillis() + jwtExpiryMillis);

    if (extraClaims.containsKey("issuedAt")) {
      if (log.isDebugEnabled())
        log.debug("extra claims include 'issuedAt', using {}", extraClaims.get("issuedAt"));
      issuedAt = new Date((Long) extraClaims.remove("issuedAt"));
    }

    if (extraClaims.containsKey("expiresAt")) {
      if (log.isDebugEnabled())
        log.debug("extra claims include 'expiresAt', using {}", extraClaims.get("expiresAt"));
      expiresAt = new Date((Long) extraClaims.remove("expiresAt"));
    }

    // TODO add extraClaims to JWT
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .issueTime(issuedAt)
            .expirationTime(expiresAt)
            .subject(userDetails.getUsername())
            .claim(
                "scope",
                userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()))
            .issuer(issuer)
            .audience("hc:" + hcEnv)
            .build();

    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build();

    SignedJWT signedJwt = new SignedJWT(header, claims);

    RSASSASigner signer = new RSASSASigner((RSAPrivateKey) privateKey);

    try {
      signedJwt.sign(signer);
    } catch (JOSEException e) {
      throw new JwtSigningException(e.getMessage(), e);
    }

    return signedJwt.serialize();
  }

  @Data
  @AllArgsConstructor
  static class RawAndDecodedResource {
    private String rawString;
    private byte[] decodedBytes;
  }
}
