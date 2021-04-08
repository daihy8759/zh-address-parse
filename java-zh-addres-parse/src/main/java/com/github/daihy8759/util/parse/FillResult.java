package com.github.daihy8759.util.parse;

import com.github.daihy8759.util.common.StrUtil;
import com.github.daihy8759.util.db.SqliteUtil;
import com.github.daihy8759.util.model.Area;
import com.github.daihy8759.util.model.ParseResult;

public class FillResult {

  private FillResult() {
  }

  protected static String replaceFragment(String fragment, String str, Area area) {
    if (fragment.startsWith(area.getName())) {
      fragment = fragment.replaceFirst(area.getName(), "");
    } else {
      fragment = fragment.replaceFirst(str, "");
    }
    return fragment;
  }

  protected static void setArea(ParseResult parseResult) {
    if (StrUtil.isEmpty(parseResult.getAreaCode())) {
      return;
    }
    Area area = SqliteUtil.getAreaSingle(parseResult.getAreaCode(), 3);
    if (area != null) {
      parseResult.setAreaName(area.getName());
      parseResult.setCityCode(area.getParentCode());
    }
  }

  protected static void setCity(ParseResult parseResult) {
    if (StrUtil.isEmpty(parseResult.getCityCode())) {
      return;
    }
    Area city = SqliteUtil.getAreaSingle(parseResult.getCityCode(), 2);
    if (city != null) {
      parseResult.setCityName(city.getName());
      parseResult.setProvinceCode(city.getParentCode());
    }
  }

  protected static void setProvince(ParseResult parseResult) {
    if (StrUtil.isEmpty(parseResult.getProvinceCode())) {
      return;
    }
    Area province = SqliteUtil.getAreaSingle(parseResult.getProvinceCode(), 1);
    if (province != null) {
      parseResult.setProvinceName(province.getName());
    }
  }
}
