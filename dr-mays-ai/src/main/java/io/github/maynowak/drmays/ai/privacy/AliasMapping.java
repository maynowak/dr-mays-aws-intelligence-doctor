package io.github.maynowak.drmays.ai.privacy;

import io.github.maynowak.drmays.core.resource.ResourceIdentity;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Per-preview pseudonym map. It is intentionally not serializable and never exposes the reverse
 * mapping through toString or public collections.
 */
public final class AliasMapping {

  private static final int KEY_BYTES = 32;
  private static final int ALIAS_CHARS = 22;

  private final byte[] key;
  private final Map<String, String> aliasesBySource = new HashMap<>();
  private final Map<String, ResourceIdentity> resourcesByAlias = new HashMap<>();

  private AliasMapping(byte[] key) {
    if (key.length < 16) {
      throw new IllegalArgumentException("alias key must contain at least 16 bytes");
    }
    this.key = key.clone();
  }

  /**
   * Creates a map with a fresh random per-preview key.
   *
   * @return new alias map
   */
  public static AliasMapping random() {
    var bytes = new byte[KEY_BYTES];
    new SecureRandom().nextBytes(bytes);
    return new AliasMapping(bytes);
  }

  /**
   * Creates a deterministic map for tests or an externally managed per-preview key.
   *
   * @param key HMAC key
   * @return new alias map
   */
  static AliasMapping withKey(byte[] key) {
    return new AliasMapping(Objects.requireNonNull(key, "key").clone());
  }

  /**
   * Returns a stable alias for a resource within this map.
   *
   * @param identity resource identity
   * @return alias beginning with {@code res_}
   */
  public synchronized String aliasFor(ResourceIdentity identity) {
    Objects.requireNonNull(identity, "identity");
    var sourceKey = "res|" + identity.canonicalId();
    var existing = aliasesBySource.get(sourceKey);
    if (existing != null) {
      return existing;
    }
    var alias = uniqueAlias("res", identity.canonicalId(), sourceKey);
    aliasesBySource.put(sourceKey, alias);
    resourcesByAlias.put(alias, identity);
    return alias;
  }

  /**
   * Returns a stable alias for a non-resource identifier such as an account or security group.
   *
   * @param namespace short lowercase namespace
   * @param value raw identifier
   * @return alias beginning with {@code namespace + "_"}
   */
  public synchronized String aliasFor(String namespace, String value) {
    if (namespace == null || !namespace.matches("[a-z][a-z0-9]*")) {
      throw new IllegalArgumentException("namespace must be a short lowercase token");
    }
    Objects.requireNonNull(value, "value");
    var sourceKey = namespace + "|" + value;
    var existing = aliasesBySource.get(sourceKey);
    if (existing != null) {
      return existing;
    }
    var alias = uniqueAlias(namespace, value, sourceKey);
    aliasesBySource.put(sourceKey, alias);
    return alias;
  }

  /**
   * Resolves a resource alias back to its original identity for local validation.
   *
   * @param alias resource alias
   * @return original identity when known
   */
  public synchronized Optional<ResourceIdentity> resolveResource(String alias) {
    return Optional.ofNullable(resourcesByAlias.get(alias));
  }

  /** Returns the number of generated aliases. */
  public synchronized int size() {
    return aliasesBySource.size();
  }

  private String uniqueAlias(String namespace, String value, String sourceKey) {
    var candidate = namespace + "_" + digest(namespace, value);
    var counter = 0;
    while (isTaken(candidate, sourceKey)) {
      counter++;
      candidate = namespace + "_" + digest(namespace, value + "#" + counter);
    }
    return candidate;
  }

  private boolean isTaken(String candidate, String sourceKey) {
    var owner =
        aliasesBySource.entrySet().stream()
            .filter(entry -> entry.getValue().equals(candidate))
            .map(Map.Entry::getKey)
            .findFirst();
    return owner.isPresent() && !owner.get().equals(sourceKey)
        || resourcesByAlias.containsKey(candidate);
  }

  private String digest(String namespace, String value) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      var bytes = mac.doFinal((namespace + "\n" + value).getBytes(StandardCharsets.UTF_8));
      var encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      return encoded.substring(0, Math.min(ALIAS_CHARS, encoded.length()));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("HmacSHA256 is unavailable", exception);
    }
  }

  @Override
  public String toString() {
    return "AliasMapping[redacted]";
  }
}
