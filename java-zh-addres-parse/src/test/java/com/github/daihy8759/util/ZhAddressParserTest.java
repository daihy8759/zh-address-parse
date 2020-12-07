package com.github.daihy8759.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
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
}
