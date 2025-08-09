package com.dogukan.ratelimiter.core;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class DefaultKeyDerivation implements KeyDerivationStrategy {

  private static String crc32(String s) {
    CRC32 c = new CRC32();
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    c.update(b, 0, b.length);
    return Long.toHexString(c.getValue());
  }

  @Override
  public String key(RateContext ctx) {
    String tag = "{rl:" + ctx.tenantId() + "}"; // cluster hash tag
    String routeHash = crc32(ctx.routeId());
    String idHash = crc32(ctx.identity());
    return "rl:" + tag + ":" + routeHash + ":" + idHash + ":" + ctx.method();
  }
}
