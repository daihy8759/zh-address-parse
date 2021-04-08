package com.github.daihy8759.util.parse;

import com.github.daihy8759.util.common.StrUtil;
import com.github.daihy8759.util.db.SqliteUtil;
import com.github.daihy8759.util.model.Area;
import com.github.daihy8759.util.model.ParseResult;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
class CityParser {

  protected String parseCity(ParseResult parseResult, String fragment) {
    if (StrUtil.isEmpty(parseResult.getCityCode())) {
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String provinceCode = parseResult.getProvinceCode();
        List<Area> areaList = SqliteUtil.getArea(provinceCode, str, 2);
        if (areaList.isEmpty()) {
          break;
        }
        if (areaList.size() == 1) {
          Area area = areaList.get(0);
          fragment = FillResult.replaceFragment(fragment, str, area);
          parseResult.setCityCode(area.getCode());
          parseResult.setCityName(area.getName());
          parseResult.setProvinceCode(area.getParentCode());
          FillResult.setProvince(parseResult);
        }
      }
    }
    return fragment;
  }

}
