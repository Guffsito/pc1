package pe.utec.dbp.labreserve.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del token: clave HMAC en Base64 y vigencia en segundos.
 */
@ConfigurationProperties(prefix = "labreserve.jwt")
public record JwtProperties(String secret, long expiresIn) {
}
