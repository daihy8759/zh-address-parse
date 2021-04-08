package com.github.daihy8759.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.daihy8759.util.model.ParseResult;
import com.github.daihy8759.util.parse.ZhAddressParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
public class ZhAddressParserTest {

  private String getResourceAsStr(String path) {
    InputStream inputStream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(path);
    return new BufferedReader(new InputStreamReader(inputStream)).lines()
        .collect(Collectors.joining(""));
  }

  @SneakyThrows
  private String getFieldValue(ParseResult parseResult, String field) {
    Class c = ParseResult.class;
    Field f = c.getDeclaredField(field);
    f.setAccessible(true);
    return (String) f.get(parseResult);
  }

  @Test
  public void testParse() {
    String addressTestParse = getResourceAsStr("addressParse.json");
    JSONArray testArray = JSON.parseArray(addressTestParse);

    int i = 1;
    for (Object o : testArray) {
      JSONObject jsonObject = (JSONObject) o;
      log.info("parse:{}", jsonObject.getString("address"));
      ParseResult parseResult = ZhAddressParser.parse(jsonObject.getString("address"), true,
          true, true);
      JSONObject assertResult = jsonObject.getJSONObject("result");
      for (String key : assertResult.keySet()) {
        assertEquals(assertResult.getString(key), getFieldValue(parseResult, key), key);
      }
      log.info("pass:{}/{}", i, testArray.size());
      i++;
    }
  }

  @Test
  @DisplayName("直辖市测试")
  public void testMunicipality() {
    ParseResult parseResult = ZhAddressParser.parse("王晓光 重庆市 垫江县 太平镇，13311111111", true,
        true, true);
    assertEquals("重庆市", parseResult.getCityName());
    assertEquals("垫江县", parseResult.getAreaName());
  }

//  @Test
//  @DisplayName("issue")
//  public void testIssue() {
//    ParseResult parseResult = ZhAddressParser.parse("先生15232889377山东省青岛市即墨市龙泉镇镇上", true,
//        true, true);
//    assertEquals("重庆市", parseResult.getCityName());
//    assertEquals("垫江县", parseResult.getAreaName());
//  }

  @Test
  public void testParseAddressOnly() {
    String addressTestParse = getResourceAsStr("addressOnlyParse.json");
    JSONArray testArray = JSON.parseArray(addressTestParse);

    int i = 1;
    for (Object o : testArray) {
      JSONObject jsonObject = (JSONObject) o;
      ParseResult parseResult = ZhAddressParser.parse(jsonObject.getString("address"), false,
          false, false);
      JSONObject assertResult = jsonObject.getJSONObject("result");
      for (String key : assertResult.keySet()) {
        assertEquals(assertResult.getString(key), getFieldValue(parseResult, key), key);
      }
      log.info("pass:{}/{}", i, testArray.size());
      i++;
    }
  }
}
