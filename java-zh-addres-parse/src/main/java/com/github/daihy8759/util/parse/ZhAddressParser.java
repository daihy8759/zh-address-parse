package com.github.daihy8759.util.parse;

import com.alibaba.fastjson.JSON;
import com.github.daihy8759.util.common.StrUtil;
import com.github.daihy8759.util.constant.AddressConstant;
import com.github.daihy8759.util.db.SqliteUtil;
import com.github.daihy8759.util.model.Area;
import com.github.daihy8759.util.model.ParseResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
          "收件人", "联系人", "聯系人", "收", "邮编", "联系电话", "电话", "电話", "電話", "联系人手机号码", "手机号码", "手机号");

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

  /**
   * 清洗地址
   */
  private String cleanAddress(String address) {
    String cleanedAddress = address.replace("\r\n", " ").replace("\n", " ").replace("\t", " ");
    for (String keyword : KEYWORDS) {
      cleanedAddress = cleanedAddress.replace(keyword, " ");
    }

    cleanedAddress = cleanedAddress
        .replaceAll("[`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|}【】‘；：”“’。，、？]",
            " ");
    cleanedAddress = cleanedAddress.replaceAll(" {2,}", " ");
    return cleanedAddress;
  }

  /**
   * 解析手机号码
   */
  private Map<String, String> filterPhone(String address) {
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
    Map<String, String> map = new HashMap<>();
    map.put(AddressConstant.KEY_ADDRESS, newAddress);
    map.put(AddressConstant.KEY_PHONE, phone);
    return map;
  }

  /**
   * 解析邮编号码
   */
  private Map<String, String> filterPostalCode(String address) {
    String postalCode = "";
    String newAddress = address;
    Matcher mobileMatcher = PATTERN_POSTAL_CODE.matcher(address);
    if (mobileMatcher.find()) {
      postalCode = mobileMatcher.group(0);
      newAddress = newAddress.replace(postalCode, " ");
    }
    Map<String, String> map = new HashMap<>();
    map.put(AddressConstant.KEY_ADDRESS, newAddress);
    map.put(AddressConstant.KEY_POSTAL_CODE, postalCode);
    return map;
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
      if (fragment.contains(nameCall)) {
        return fragment;
      }
    }
    // 如果百家姓里面能找到这个姓，并且长度在1-5之间
    String nameFirst = fragment.substring(0, 1);
    if (fragment.length() <= nameMaxLength && fragment.length() > 1
        && AddressConstant.ZH_NAME.contains(nameFirst)) {
      return fragment;
    }

    return "";
  }

  /**
   * 解析地址
   *
   * @param address    待解析地址
   * @param parseName  是否解析用户名
   * @param parsePhone 是否解析手机号码
   * @param postalCode 是否解析邮编
   */
  public ParseResult parse(String address, boolean parseName, boolean parsePhone,
      boolean postalCode) {
    ParseResult parseResult = new ParseResult();
    if (StrUtil.isBlank(address)) {
      return parseResult;
    }
    String cleanedAddress = cleanAddress(address);
    log.info("清洗地址:{}", cleanedAddress);
    if (parsePhone) {
      Map<String, String> phoneResult = filterPhone(cleanedAddress);
      parseResult.setPhone(phoneResult.get(AddressConstant.KEY_PHONE));
      cleanedAddress = phoneResult.get(AddressConstant.KEY_ADDRESS);
    }
    if (postalCode) {
      Map<String, String> postalCodeResult = filterPostalCode(cleanedAddress);
      parseResult.setPostalCode(postalCodeResult.get(AddressConstant.KEY_POSTAL_CODE));
      cleanedAddress = postalCodeResult.get(AddressConstant.KEY_ADDRESS);
    }
    List<String> splitAddressList = Arrays.stream(cleanedAddress.split(" "))
        .filter(StrUtil::isNotBlank)
        .map(StrUtil::trim).collect(Collectors.toList());
    log.info("分割地址:{}", splitAddressList);
    List<String> detail = new ArrayList<>();
    for (String splitAddress : splitAddressList) {
      if (StrUtil.isEmpty(parseResult.getProvinceCode()) || StrUtil
          .isEmpty(parseResult.getCityCode())
          || StrUtil.isEmpty(parseResult.getAreaCode()) || StrUtil
          .isEmpty(parseResult.getStreetCode())) {
        String fragment = ProvinceParser.parseProvince(parseResult, splitAddress);
        fragment = CityParser.parseCity(parseResult, fragment);
        fragment = AreaParser.parseArea(parseResult, fragment);
        fragment = StreetParser.parseStreet(parseResult, fragment);
        if (StrUtil.isNotBlank(fragment)) {
          detail.add(fragment);
        }
      } else {
        detail.add(splitAddress);
      }
    }
    if (parseName) {
      if (!detail.isEmpty()) {
        List<String> sortDetail = new ArrayList<>(detail.size());
        sortDetail.addAll(detail);
        sortDetail.sort((o1, o2) -> {
          String str1 = Objects.toString(o1, "");
          String str2 = Objects.toString(o2, "");
          return str1.length() - str2.length();
        });

        String name = "";
        for (Object o : sortDetail) {
          String fragment = (String) o;
          name = judgeFragmentIsName(fragment, NAME_MAX_LENGTH);
          if (StrUtil.isNotBlank(name)) {
            break;
          }
        }
        if (StrUtil.isBlank(name) && sortDetail.get(0).length() <= NAME_MAX_LENGTH
            && PATTERN_NAME.matcher(sortDetail.get(0)).find()) {
          name = sortDetail.get(0);
        }
        parseResult.setName(name);
        if (StrUtil.isNotBlank(name)) {
          detail.remove(name);
        }
      }
    }
    parseResult.setDetail(detail.stream().collect(Collectors.joining("")));
    replaceMunicipality(parseResult);
    log.info("解析结果:{}", JSON.toJSONString(parseResult));
    return parseResult;
  }

  /**
   * 替换直辖市
   */
  private void replaceMunicipality(ParseResult parseResult) {
    String provinceName = parseResult.getProvinceName();
    if (StrUtil.isBlank(provinceName)) {
      return;
    }
    String cityName = parseResult.getCityName();
    if (StrUtil.isBlank(cityName)) {
      return;
    }
    if (MUNICIPALITY.contains(provinceName) && SPECIAL_AREA.contains(cityName)) {
        List<Area> areaList = SqliteUtil.getArea(null, parseResult.getProvinceName(),2);
        if(!areaList.isEmpty()) {
          Area area = areaList.get(0);
          parseResult.setCityCode(area.getCode());
          parseResult.setCityName(area.getName());
        }
    }
  }

}
