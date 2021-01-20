package com.github.daihy8759.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ZhAddressParser {

  private static final List<String> KEYWORDS = Arrays
      .asList("详细地址", "收货地址", "收件地址", "地址", "所在地区", "地区", "姓名", "收货人",
          "收件人", "联系人", "收", "邮编", "联系电话", "电话", "联系人手机号码", "手机号码", "手机号");

  private static final List<String> NAME_CALL = Arrays
      .asList("先生", "小姐", "同志", "哥哥", "姐姐", "妹妹", "弟弟", "妈妈", "爸爸",
          "爷爷", "奶奶", "姑姑", "舅舅");

  /**
   * 直辖市
   */
  private static final List<String> MUNICIPALITY = Arrays.asList("北京市", "天津市", "上海市", "重庆市");

  private static final List<String> SPECIAL_AREA = Arrays.asList("市辖区", "区", "县", "镇");

  private static final Pattern PATTERN_PHONE = Pattern
      .compile("(\\d{7,12})|(\\d{3,4}-\\d{6,8})|(86-[1][0-9]{10})|(86[1][0-9]{10})|([1][0-9]{10})");

  public static final Integer NAME_MAX_LENGTH = 4;

  private static final Pattern PATTERN_POSTAL_CODE = Pattern.compile("\\d{6}");
  private static final Pattern PATTERN_NAME = Pattern.compile("[\\u4E00-\\u9FA5]");
  private static final String PATTERN_ADDRESS_CODE = "[0-9]{1,6}";
  private static final String PATTERN_PROVINCE_CODE = "\\{\\\"code\\\":\\\"%s\\\",\\\"name\\\":\\\"[\\u4E00-\\u9FA5]+?\\\"\\}";
  private static final String PATTERN_CITY_CODE = "\\{\\\"code\\\":\\\"%s\\\",\\\"name\\\":\\\"[\\u4E00-\\u9FA5]+?\\\",\\\"provinceCode\\\":\\\"%s\\\"\\}";
  private static final String PATTERN_AREA_CODE = "\\{\\\"code\\\":\\\"%s\\\",\\\"name\\\":\\\"[\\u4E00-\\u9FA5]+?\\\",\\\"cityCode\\\":\\\"%s\\\",\\\"provinceCode\\\":\\\"%s\\\"\\}";

  private static String provinceString = "";
  private static String cityString = "";
  private static String areaString = "";
  private static String streetString = "";
  private static String zhCnNames = "";

  private static final String KEY_COUNT = "count";
  private static final String KEY_DATA = "data";
  private static final String KEY_CODE = "code";
  public static final String KEY_ADDRESS = "address";
  public static final String KEY_POSTAL_CODE = "postalCode";
  public static final String KEY_PHONE = "phone";
  public static final String KEY_PROVINCE = "province";
  public static final String KEY_CITY = "city";
  public static final String KEY_AREA = "area";
  public static final String KEY_STREET = "street";
  public static final String KEY_NAME = "name";
  public static final String KEY_DETAIL = "detail";

  static {
    try {
      provinceString = ResourceUtil.readStr("area/provinces.json", CharsetUtil.CHARSET_UTF_8);
      cityString = ResourceUtil.readStr("area/cities.json", CharsetUtil.CHARSET_UTF_8);
      areaString = ResourceUtil.readStr("area/areas.json", CharsetUtil.CHARSET_UTF_8);
      streetString = ResourceUtil.readStr("area/streets.json", CharsetUtil.CHARSET_UTF_8);
      zhCnNames = ResourceUtil.readStr("area/names.json", CharsetUtil.CHARSET_UTF_8);
    } catch (Exception e) {
      log.error("读取配置失败:{}", ExceptionUtil.stacktraceToString(e));
    }
  }

  /**
   * 清洗地址
   */
  private String cleanAddress(String address) {
    String cleanedAddress = address.replace("\r\n", " ").replace("\n", " ").replace("\t", " ");
    for (String keyword : KEYWORDS) {
      cleanedAddress = cleanedAddress.replace(keyword, " ");
    }

    cleanedAddress = cleanedAddress
        .replaceAll("[`~!@#$^&*()=|\\{}':;',\\[\\].<>/?~！@#￥……&*（）——|\\{}【】‘；：”“’。，、？]",
            " ");
    cleanedAddress = cleanedAddress.replaceAll(" {2,}", " ");
    return cleanedAddress;
  }

  /**
   * 解析手机号码
   */
  private JSONObject filterPhone(String address) {
    String phone = "";
    String replacement = "$1$2$3";
    String newAddress = address.replaceAll("(\\d{3})-(\\d{4})-(\\d{4})", replacement);
    newAddress = newAddress.replaceAll("(\\d{3}) (\\d{4}) (\\d{4})", replacement);
    newAddress = newAddress.replaceAll("(\\d{4}) \\d{4} \\d{4}", replacement);
    newAddress = newAddress.replaceAll("(\\d{4})", "$1");

    Matcher mobileMatcher = PATTERN_PHONE.matcher(newAddress);
    if (mobileMatcher.find()) {
      phone = mobileMatcher.group(0);
      newAddress = newAddress.replace(phone, " ");
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_ADDRESS, newAddress);
    jsonObject.put(KEY_PHONE, phone);
    return jsonObject;
  }

  /**
   * 解析邮编号码
   */
  private JSONObject filterPostalCode(String address) {
    String postalCode = "";
    String newAddress = address;
    Matcher mobileMatcher = PATTERN_POSTAL_CODE.matcher(address);
    if (mobileMatcher.find()) {
      postalCode = mobileMatcher.group(0);
      newAddress = newAddress.replace(postalCode, " ");
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(KEY_ADDRESS, newAddress);
    jsonObject.put(KEY_POSTAL_CODE, postalCode);
    return jsonObject;
  }

  private JSONArray getOrDefault(JSONObject hasParseResult, String key) {
    JSONArray province = hasParseResult.getJSONArray(key);
    if (province == null) {
      return new JSONArray();
    }
    return province;
  }

  /**
   * 判断是否是名字
   */
  private String judgeFragmentIsName(String fragment, int nameMaxLength) {
    if (StrUtil.isBlank(fragment)) {
      return "";
    }
    if (!PATTERN_NAME.matcher(fragment).find()) {
      return "";
    }

    for (String nameCall : NAME_CALL) {
      if (fragment.indexOf(nameCall) != -1) {
        return fragment;
      }
    }
    // 如果百家姓里面能找到这个姓，并且长度在1-5之间
    String nameFirst = fragment.substring(0, 1);
    if (fragment.length() <= nameMaxLength && fragment.length() > 1
        && zhCnNames.indexOf(nameFirst) != -1) {
      return fragment;
    }

    return "";
  }

  /**
   * 只匹配一次的情况返回数据,至多匹配两次
   */
  private JSONObject matchOnlyOnce(Matcher matcher) {
    JSONObject result = new JSONObject();
    JSONObject data = null;
    int matchCount = 0;
    int from = 0;
    String matchString = "";
    while (matcher.find(from)) {
      matchCount++;
      if (matchCount == 2) {
        break;
      }
      from = matcher.start() + 1;
      matchString = matcher.group(0);
    }
    if (matchCount == 1) {
      data = JSON.parseObject(matchString);
    }
    result.put(KEY_COUNT, matchCount);
    result.put(KEY_DATA, data);
    return result;
  }

  private String parseStreet(JSONArray street, JSONArray area, JSONArray city, JSONArray province,
      String fragment) {
    if (street.isEmpty()) {
      String matchStr = "";
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String pattern = "\\{\\\"code\\\":\\\"[0-9]{1,9}\\\",\\\"name\\\":\\\"%s[\\u4E00-\\u9FA5]*?\\\",\\\"areaCode\\\":\\\"%s\\\",\\\"cityCode\\\":\\\"%s\\\",\\\"provinceCode\\\":\\\"%s\\\"\\}";
        String areaCode =
            area.isEmpty() ? PATTERN_ADDRESS_CODE : area.getJSONObject(0).getString(KEY_CODE);
        String cityCode =
            city.isEmpty() ? PATTERN_ADDRESS_CODE : city.getJSONObject(0).getString(KEY_CODE);
        String provinceCode =
            province.isEmpty() ? PATTERN_ADDRESS_CODE
                : province.getJSONObject(0).getString(KEY_CODE);
        pattern = String.format(pattern, str, areaCode, cityCode, provinceCode);
        Matcher matchStreet = Pattern.compile(pattern).matcher(streetString);
        JSONObject matchResult = matchOnlyOnce(matchStreet);
        int matchCount = matchResult.getInteger(KEY_COUNT);
        if (matchCount == 0) {
          break;
        } else if (matchCount == 1) {
          street.clear();
          matchStr = str;
          street.add(matchResult.getJSONObject(KEY_DATA));
        }
      }
      if (!street.isEmpty()) {
        fragment = fragment.replaceFirst(matchStr, "");
        String provinceCode = street.getJSONObject(0).getString("provinceCode");
        String cityCode = street.getJSONObject(0).getString("cityCode");
        if (province.isEmpty()) {
          String pattern = String.format(PATTERN_PROVINCE_CODE, provinceCode);
          Matcher matchProvince = Pattern.compile(pattern).matcher(provinceString);
          if (matchProvince.find()) {
            province.add(JSON.parse(matchProvince.group(0)));
          }
        }
        if (city.isEmpty()) {
          String pattern = String.format(PATTERN_CITY_CODE, cityCode, provinceCode);
          Matcher matchCity = Pattern.compile(pattern).matcher(cityString);
          if (matchCity.find()) {
            city.add(JSON.parse(matchCity.group(0)));
          }
        }
        if (area.isEmpty()) {
          String areaCode = street.getJSONObject(0).getString("areaCode");
          String pattern = String.format(PATTERN_AREA_CODE, areaCode, cityCode, provinceCode);
          Matcher matchArea = Pattern.compile(pattern).matcher(areaString);
          if (matchArea.find()) {
            area.add(JSON.parse(matchArea.group(0)));
          }
        }
      }
    }
    return fragment;
  }

  private String parseArea(JSONArray area, JSONArray city, JSONArray province, String fragment) {
    if (area.isEmpty()) {
      String matchStr = "";
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String pattern = "\\{\\\"code\\\":\\\"[0-9]{1,6}\\\",\\\"name\\\":\\\"%s[\\u4E00-\\u9FA5]*?\\\",\\\"cityCode\\\":\\\"%s\\\",\\\"provinceCode\\\":\\\"%s\\\"\\}";
        String cityCode =
            city.isEmpty() ? PATTERN_ADDRESS_CODE : city.getJSONObject(0).getString(KEY_CODE);
        String provinceCode =
            province.isEmpty() ? PATTERN_ADDRESS_CODE
                : province.getJSONObject(0).getString(KEY_CODE);
        pattern = String.format(pattern, str, cityCode, provinceCode);
        Matcher matchArea = Pattern.compile(pattern).matcher(areaString);
        JSONObject matchResult = matchOnlyOnce(matchArea);
        int matchCount = matchResult.getInteger(KEY_COUNT);
        if (matchCount == 0) {
          break;
        } else if (matchCount == 1) {
          area.clear();
          matchStr = str;
          area.add(matchResult.getJSONObject(KEY_DATA));
        }
      }
      if (!area.isEmpty()) {
        fragment = fragment.replaceFirst(matchStr, "");
        String provinceCode = area.getJSONObject(0).getString("provinceCode");
        if (province.isEmpty()) {
          String pattern = String.format(PATTERN_PROVINCE_CODE, provinceCode);
          Matcher matchProvince = Pattern.compile(pattern).matcher(provinceString);
          if (matchProvince.find()) {
            province.add(JSON.parse(matchProvince.group(0)));
          }
        }
        if (city.isEmpty()) {
          String cityCode = area.getJSONObject(0).getString("cityCode");
          String pattern = String.format(PATTERN_CITY_CODE, cityCode, provinceCode);
          Matcher matchCity = Pattern.compile(pattern).matcher(cityString);
          if (matchCity.find()) {
            city.add(JSON.parse(matchCity.group(0)));
          }
        }
      }
    }
    return fragment;
  }

  private String parseCity(JSONArray city, JSONArray province, String fragment) {
    if (city.isEmpty()) {
      String matchStr = "";
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String pattern = "\\{\\\"code\\\":\\\"[0-9]{1,6}\\\",\\\"name\\\":\\\"%s[\\u4E00-\\u9FA5]*?\\\",\\\"provinceCode\\\":\\\"%s\\\"\\}";
        String provinceCode =
            province.isEmpty() ? PATTERN_ADDRESS_CODE
                : province.getJSONObject(0).getString(KEY_CODE);
        pattern = String.format(pattern, str, provinceCode);
        Matcher matchCity = Pattern.compile(pattern).matcher(cityString);
        JSONObject matchResult = matchOnlyOnce(matchCity);
        int matchCount = matchResult.getInteger(KEY_COUNT);
        if (matchCount == 0) {
          break;
        } else if (matchCount == 1) {
          city.clear();
          matchStr = str;
          city.add(matchResult.getJSONObject(KEY_DATA));
        }
      }
      if (!city.isEmpty()) {
        fragment = fragment.replaceFirst(matchStr, "");
        if (province.isEmpty()) {
          String provinceCode = city.getJSONObject(0).getString("provinceCode");
          String pattern = String.format(PATTERN_PROVINCE_CODE, provinceCode);
          Matcher matchProvince = Pattern.compile(pattern).matcher(provinceString);
          if (matchProvince.find()) {
            province.add(JSON.parse(matchProvince.group(0)));
          }
        }
      }
    }
    return fragment;
  }

  private String parseProvince(JSONArray province, String fragment) {
    if (province.isEmpty()) {
      String matchStr = "";
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String pattern = String.format(
            "\\{\\\"code\\\":\\\"[0-9]{1,6}\\\",\\\"name\\\":\\\"%s[\\u4E00-\\u9FA5]*?\\\"}", str);
        Matcher matchProvince = Pattern.compile(pattern).matcher(provinceString);
        JSONObject matchResult = matchOnlyOnce(matchProvince);
        int matchCount = matchResult.getInteger(KEY_COUNT);
        if (matchCount == 0) {
          break;
        } else if (matchCount == 1) {
          province.clear();
          matchStr = str;
          province.add(matchResult.getJSONObject(KEY_DATA));
        }
      }
      if (!province.isEmpty()) {
        fragment = fragment.replaceFirst(matchStr, "");
      }
    }
    return fragment;
  }

  /**
   * 使用正则解析地址
   *
   * @param address        地址
   * @param hasParseResult 已解析地址
   */
  private JSONObject parseRegionWithRegexp(String address, JSONObject hasParseResult) {
    JSONArray province = getOrDefault(hasParseResult, KEY_PROVINCE);
    JSONArray city = getOrDefault(hasParseResult, KEY_CITY);
    JSONArray area = getOrDefault(hasParseResult, KEY_AREA);
    JSONArray street = getOrDefault(hasParseResult, KEY_STREET);
    JSONArray detail = new JSONArray();

    String fragment = parseProvince(province, address);
    fragment = parseCity(city, province, fragment);
    fragment = parseArea(area, city, province, fragment);
    fragment = parseStreet(street, area, city, province, fragment);

    if (StrUtil.isNotEmpty(fragment)) {
      detail.add(fragment);
    }

    JSONObject parseResult = new JSONObject();
    parseResult.put(KEY_PROVINCE, province);
    parseResult.put(KEY_CITY, city);
    parseResult.put(KEY_AREA, area);
    parseResult.put(KEY_STREET, street);
    parseResult.put(KEY_DETAIL, detail);
    return parseResult;
  }

  /**
   * 解析地址
   *
   * @param address    待解析地址
   * @param parseName  是否解析用户名
   * @param parsePhone 是否解析手机号码
   * @param postalCode 是否解析邮编
   * @return
   */
  public JSONObject parse(String address, boolean parseName, boolean parsePhone,
      boolean postalCode) {
    JSONObject parseResult = new JSONObject();
    parseResult.put(KEY_PROVINCE, new JSONArray());
    parseResult.put(KEY_CITY, new JSONArray());
    parseResult.put(KEY_AREA, new JSONArray());
    parseResult.put(KEY_STREET, new JSONArray());
    parseResult.put(KEY_DETAIL, new JSONArray());
    parseResult.put(KEY_PHONE, "");
    parseResult.put(KEY_NAME, "");
    parseResult.put(KEY_POSTAL_CODE, "");
    if (StrUtil.isBlank(address)) {
      return parseResult;
    }
    String cleanedAddress = cleanAddress(address);
    log.info("清洗地址:{}", cleanedAddress);
    if (parsePhone) {
      JSONObject phoneResult = filterPhone(cleanedAddress);
      parseResult.put(KEY_PHONE, phoneResult.getString(KEY_PHONE));
      cleanedAddress = phoneResult.getString(KEY_ADDRESS);
    }
    if (postalCode) {
      JSONObject postalCodeResult = filterPostalCode(cleanedAddress);
      parseResult.put(KEY_POSTAL_CODE, postalCodeResult.getString(KEY_POSTAL_CODE));
      cleanedAddress = postalCodeResult.getString(KEY_ADDRESS);
    }
    List<String> splitAddressList = Arrays.stream(cleanedAddress.split(" "))
        .filter(StrUtil::isNotBlank)
        .map(StrUtil::trim).collect(Collectors.toList());
    log.info("分割地址:{}", splitAddressList);
    for (String splitAddress : splitAddressList) {
      if (parseResult.getJSONArray(KEY_PROVINCE).isEmpty() || parseResult.getJSONArray(KEY_CITY)
          .isEmpty()
          || parseResult.getJSONArray(KEY_AREA).isEmpty() || parseResult.getJSONArray(KEY_STREET)
          .isEmpty()) {
        JSONObject regionResult = parseRegionWithRegexp(splitAddress, parseResult);
        JSONArray detail = regionResult.getJSONArray(KEY_DETAIL);
        parseResult.put(KEY_PROVINCE, regionResult.getJSONArray(KEY_PROVINCE));
        parseResult.put(KEY_CITY, regionResult.getJSONArray(KEY_CITY));
        parseResult.put(KEY_AREA, regionResult.getJSONArray(KEY_AREA));
        parseResult.put(KEY_STREET, regionResult.getJSONArray(KEY_STREET));
        parseResult.getJSONArray(KEY_DETAIL).addAll(detail);
      } else {
        parseResult.getJSONArray(KEY_DETAIL).add(splitAddress);
      }
    }
    JSONArray detail = parseResult.getJSONArray(KEY_DETAIL);
    if (parseName) {
      if (!detail.isEmpty()) {
        JSONArray newDetail = ObjectUtil.cloneByStream(detail);
        newDetail.sort((o1, o2) -> {
          String str1 = (String) ObjectUtil.defaultIfNull(o1, "");
          String str2 = (String) ObjectUtil.defaultIfNull(o2, "");
          return str1.length() - str2.length();
        });

        String name = "";
        for (Object o : newDetail) {
          String fragment = (String) o;
          name = judgeFragmentIsName(fragment, NAME_MAX_LENGTH);
          if (StrUtil.isNotBlank(name)) {
            break;
          }
        }
        if (StrUtil.isBlank(name) && newDetail.getString(0).length() <= NAME_MAX_LENGTH
            && PATTERN_NAME.matcher(newDetail.getString(0)).find()) {
          name = newDetail.getString(0);
        }
        parseResult.put(KEY_NAME, name);
        if (StrUtil.isNotBlank(name)) {
          detail.remove(name);
        }
      }
    }
    log.info("解析结果:{}", parseResult);
    return flatResult(parseResult, detail);
  }

  /**
   * 替换直辖市
   *
   * @param provinceName 省份信息
   */
  private void replaceMunicipality(JSONObject result, String provinceName) {
    if (StrUtil.isBlank(provinceName)) {
      return;
    }
    String cityName = result.getString(KEY_CITY);
    if (StrUtil.isBlank(cityName)) {
      return;
    }
    if (MUNICIPALITY.contains(provinceName) && SPECIAL_AREA.contains(cityName)) {
      JSONArray cityDataArray = JSON.parseArray(cityString);
      List<Object> resultList = cityDataArray.stream()
          .filter(c -> provinceName.equals(((JSONObject) c).getString(KEY_NAME)))
          .collect(Collectors.toList());
      if (resultList.isEmpty()) {
        return;
      }
      result.put(KEY_CITY, provinceName);
      result.put("cityCode", ((JSONObject) resultList.get(0)).getString(KEY_CODE));
    }
  }

  private JSONObject flatResult(JSONObject parseResult, JSONArray detail) {
    JSONObject province = new JSONObject();
    JSONArray provinceArray = parseResult.getJSONArray(KEY_PROVINCE);
    if (!provinceArray.isEmpty()) {
      province = provinceArray.getJSONObject(0);
    }
    JSONObject city = new JSONObject();
    JSONArray cityArray = parseResult.getJSONArray(KEY_CITY);
    if (!cityArray.isEmpty()) {
      city = cityArray.getJSONObject(0);
    }
    JSONObject area = new JSONObject();
    JSONArray areaArray = parseResult.getJSONArray(KEY_AREA);
    if (!areaArray.isEmpty()) {
      area = areaArray.getJSONObject(0);
    }
    JSONObject street = new JSONObject();
    JSONArray streetArray = parseResult.getJSONArray(KEY_STREET);
    if (!streetArray.isEmpty()) {
      street = streetArray.getJSONObject(0);
    }

    JSONObject flatResult = new JSONObject();
    flatResult.putAll(parseResult);
    flatResult.put("provinceCode", StrUtil.nullToEmpty(province.getString(KEY_CODE)));
    String provinceName = StrUtil.nullToEmpty(province.getString(KEY_NAME));
    flatResult.put(KEY_PROVINCE, provinceName);
    String cityName = StrUtil.nullToEmpty(city.getString(KEY_NAME));
    flatResult.put("cityCode", StrUtil.nullToEmpty(city.getString(KEY_CODE)));
    flatResult.put(KEY_CITY, cityName);
    replaceMunicipality(flatResult, provinceName);

    flatResult.put("areaCode", StrUtil.nullToEmpty(area.getString(KEY_CODE)));
    flatResult.put(KEY_AREA, StrUtil.nullToEmpty(area.getString(KEY_NAME)));
    flatResult.put("streetCode", StrUtil.nullToEmpty(street.getString(KEY_CODE)));
    flatResult.put(KEY_STREET, StrUtil.nullToEmpty(street.getString(KEY_NAME)));
    flatResult.put(KEY_DETAIL, ArrayUtil.join(detail.toArray(new String[detail.size()]), ""));

    log.info("合并结果:{}", flatResult);
    return flatResult;
  }

}
