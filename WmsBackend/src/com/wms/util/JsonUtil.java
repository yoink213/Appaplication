package com.wms.util;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/**
 * [CLASS] JsonUtil
 * Operations:
 *   + getRequestBody(request : HttpServletRequest) : String
 *   + getJsonValue(json : String, key : String) : String
 *   Description: 原生 JSON 工具类，免去导入第三方 jar 包带来的类加载失败异常。
 */
public class JsonUtil {

    public static String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static String getJsonValue(String json, String key) {
        int index = json.indexOf("\"" + key + "\"");
        if (index == -1) return "";
        int start = json.indexOf(":", index) + 1;
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        return value.replace("\"", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
    }
}