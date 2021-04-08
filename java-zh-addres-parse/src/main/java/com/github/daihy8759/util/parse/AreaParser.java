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
public class AreaParser {

  protected String parseArea(ParseResult parseResult, String fragment) {
    if (StrUtil.isEmpty(parseResult.getAreaCode())) {
      for (int i = 1; i < fragment.length(); i++) {
        String str = fragment.substring(0, i + 1);
        String cityCode = parseResult.getCityCode();
        List<Area> areaList = SqliteUtil.getArea(cityCode, str, 3);
        if (areaList.isEmpty()) {
          break;
        }
        if (areaList.size() == 1) {
          Area area = areaList.get(0);
          fragment = FillResult.replaceFragment(fragment, str, area);
          parseResult.setAreaCode(area.getCode());
          parseResult.setAreaName(area.getName());
          parseResult.setCityCode(area.getParentCode());
          FillResult.setCity(parseResult);
          FillResult.setProvince(parseResult);
        }
      }
    }
    return fragment;
  }

}
