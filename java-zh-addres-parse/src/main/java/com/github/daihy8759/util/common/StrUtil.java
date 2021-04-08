package com.github.daihy8759.util.common;

public class StrUtil {

  public static boolean isEmpty(String str) {
    return str == null || "".equals(str);
  }

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }

  public static String trim(String str) {
    return (null == str) ? null : str.trim();
  }

  public static boolean isBlank(String str) {
    return isEmpty(str) || isEmpty(trim(str));
  }

  public static boolean isNotBlank(String str) {
    return !isBlank(str);
  }


}
