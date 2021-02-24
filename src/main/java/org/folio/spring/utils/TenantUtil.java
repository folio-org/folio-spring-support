package org.folio.spring.utils;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TenantUtil {
  private static final Pattern NON_WORD_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_]");

  public static boolean isValidTenantName(String tenant) {
    return isNotBlank(tenant) && !NON_WORD_CHARACTERS.matcher(tenant).find();
  }
}
