package com.trinket.trinketos.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class StringUtils {

  // Matches most Emojis and Symbols we want to block in descriptions
  // Excludes \p{So} (Symbol, other) which contains most Emojis.
  private static final String EMOJI_REGEX = "[\\p{So}]";

  // Strict name: Letters, Marks, Numbers, Spaces, common punctuation.
  private static final String STRICT_NAME_REGEX = "^[\\p{L}\\p{M}0-9\\p{Z}\\.\\-\\_\\(\\)]+$";

  public enum ValidationMode {
    STRICT_NAME,
    DESCRIPTION_NO_EMOJI,
    SLUG
  }

  private StringUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Validates a string based on the provided mode.
   * Optionally normalizes the string before validation (e.g., NFKC).
   * 
   * @param value     The value to validate
   * @param fieldName The name of the field (for error messages)
   * @param mode      The validation mode
   * @param normalize If true, applies NFKC normalization before validation checks
   */
  public static void validateString(String value, String fieldName, ValidationMode mode, boolean normalize) {
    if (value == null || value.isBlank()) {
      return; // Or throw if required, but usually handled by @NotNull annotations
    }

    String toCheck = value;
    if (normalize) {
      toCheck = Normalizer.normalize(value, Normalizer.Form.NFKC);
    }

    switch (mode) {
      case STRICT_NAME:
        if (!toCheck.matches(STRICT_NAME_REGEX)) {
          throw new IllegalArgumentException(
              String.format(
                  "The field '%s' contains invalid characters. Value received: '%s'. Only letters, numbers, spaces, and common punctuation (-, _, ., (, )) are allowed.",
                  fieldName, value));
        }
        break;
      case DESCRIPTION_NO_EMOJI:
        // We check if it contains any blocked character (emoji)
        // Using find() since we are looking for presence of illegal chars
        if (Pattern.compile(EMOJI_REGEX).matcher(toCheck).find()) {
          throw new IllegalArgumentException(
              String.format("The field '%s' contains invalid characters (Emojis are not allowed).", fieldName));
        }
        break;
      case SLUG:
        // Basic slug validation if needed, or rely on SlugUtils to generate it.
        if (!toCheck.matches("^[a-z0-9-]+$")) {
          throw new IllegalArgumentException(
              String.format(
                  "The field '%s' is not a valid slug. Only lowercase letters, numbers and dashes are allowed.",
                  fieldName));
        }
        break;
    }
  }

  public static String toSlug(String input) {
    return SlugUtils.toSlug(input);
  }
}
