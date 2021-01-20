package com.github.daihy8759.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
public class ZhAddressParserTest {

  @Test
  public void testParse() {
    String addressTestParse = ResourceUtil.readStr("addressParse.json", CharsetUtil.CHARSET_UTF_8);
    JSONArray testArray = JSON.parseArray(addressTestParse);

    int i = 1;
    for (Object o : testArray) {
      JSONObject jsonObject = (JSONObject) o;
      JSONObject parseResult = ZhAddressParser.parse(jsonObject.getString("address"), true,
          true, true);
      JSONObject assertResult = jsonObject.getJSONObject("result");
      for (String key : assertResult.keySet()) {
        assertEquals(assertResult.getString(key), parseResult.getString(key));
      }
      log.info("pass:{}/{}", i, testArray.size());
      i++;
    }
  }

  @Test
  @DisplayName("直辖市测试")
  public void testMunicipality() {
    JSONObject parseResult = ZhAddressParser.parse("王晓光 重庆市 垫江县 太平镇，13311111111", true,
        true, true);
    assertEquals("重庆市", parseResult.getString(ZhAddressParser.KEY_CITY));
    assertEquals("垫江县", parseResult.getString(ZhAddressParser.KEY_AREA));
  }

  @Test
  public void testParseAddressOnly() {
    String addressTestParse = ResourceUtil
        .readStr("addressOnlyParse.json", CharsetUtil.CHARSET_UTF_8);
    JSONArray testArray = JSON.parseArray(addressTestParse);

    int i = 1;
    for (Object o : testArray) {
      JSONObject jsonObject = (JSONObject) o;
      JSONObject parseResult = ZhAddressParser.parse(jsonObject.getString("address"), false,
          false, false);
      JSONObject assertResult = jsonObject.getJSONObject("result");
      for (String key : assertResult.keySet()) {
        assertEquals(assertResult.getString(key), parseResult.getString(key));
      }
      log.info("pass:{}/{}", i, testArray.size());
      i++;
    }
  }
}
