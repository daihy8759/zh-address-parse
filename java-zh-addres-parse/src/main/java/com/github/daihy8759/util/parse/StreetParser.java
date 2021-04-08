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
public class StreetParser {

  protected String parseStreet(ParseResult parseResult, String fragment) {
    if (StrUtil.isEmpty(parseResult.getStreetCode())) {
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String areaCode = parseResult.getAreaCode();
        List<Area> areaList = SqliteUtil.getArea(areaCode, str, 4);
        if (areaList.isEmpty()) {
          break;
        }
        if (areaList.size() == 1) {
          Area area = areaList.get(0);
          fragment = FillResult.replaceFragment(fragment, str, area);
          parseResult.setStreetCode(area.getCode());
          parseResult.setStreetName(area.getName());
          parseResult.setAreaCode(area.getParentCode());
          FillResult.setArea(parseResult);
          FillResult.setCity(parseResult);
          FillResult.setProvince(parseResult);
        }
      }
    }
    return fragment;
  }
}
