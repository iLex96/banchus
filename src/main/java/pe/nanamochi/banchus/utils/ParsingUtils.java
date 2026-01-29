package pe.nanamochi.banchus.utils;

public class ParsingUtils {

  private ParsingUtils() {}

  public static Integer parseIntSafe(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
