package com.library.system.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.MultiValueMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XSS请求包装器
 * 包装HttpServletRequest，对所有参数进行XSS过滤
 * 
 * FIXED: SEC-005 支持JSON body的XSS过滤
 *
 * @author Security Team
 * @version 2.0.0
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = requestInputStream.readAllBytes();
    }

    /**
     * 获取请求参数并过滤XSS
     */
    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return filterXss(value);
    }

    /**
     * 获取多个参数值并过滤XSS
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] filteredValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            filteredValues[i] = filterXss(values[i]);
        }
        return filteredValues;
    }

    /**
     * 获取所有参数并过滤XSS
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> originalMap = super.getParameterMap();
        Map<String, String[]> filteredMap = new HashMap<>();

        for (Map.Entry<String, String[]> entry : originalMap.entrySet()) {
            String[] values = entry.getValue();
            String[] filteredValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                filteredValues[i] = filterXss(values[i]);
            }
            filteredMap.put(entry.getKey(), filteredValues);
        }

        return filteredMap;
    }

    /**
     * 获取请求头并过滤XSS
     */
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return filterXss(value);
    }

    /**
     * FIXED: SEC-005 获取JSON请求体并进行XSS过滤
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (isJsonRequest()) {
            // 对JSON body进行XSS过滤
            String filteredBody = filterJsonBody(new String(cachedBody, StandardCharsets.UTF_8));
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                    filteredBody.getBytes(StandardCharsets.UTF_8));
            
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    // 不支持异步读取
                }

                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };
        } else {
            return super.getInputStream();
        }
    }

    /**
     * FIXED: SEC-005 获取请求体字符串
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (isJsonRequest()) {
            String filteredBody = filterJsonBody(new String(cachedBody, StandardCharsets.UTF_8));
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(filteredBody.getBytes(StandardCharsets.UTF_8)), 
                    StandardCharsets.UTF_8));
        } else {
            return super.getReader();
        }
    }

    /**
     * FIXED: SEC-005 检查是否为JSON请求
     */
    private boolean isJsonRequest() {
        String contentType = getHeader("Content-Type");
        return contentType != null && contentType.contains("application/json");
    }

    /**
     * FIXED: SEC-005 过滤JSON body中的所有字符串值
     */
    private String filterJsonBody(String body) {
        if (StrUtil.isBlank(body)) {
            return body;
        }

        try {
            // 尝试解析为JSON并过滤所有字符串值
            if (JSONUtil.isJson(body)) {
                Object parsed = JSONUtil.parse(body);
                if (parsed instanceof JSONObject) {
                    JSONObject jsonObject = (JSONObject) parsed;
                    filterJsonObject(jsonObject);
                    return jsonObject.toString();
                }
            }
        } catch (Exception e) {
            // 如果JSON解析失败，使用通用的XSS过滤
            log.warn("JSON body解析失败，使用通用过滤: {}", e.getMessage());
        }

        // 通用XSS过滤
        return filterXss(body);
    }

    /**
     * FIXED: SEC-005 递归过滤JSON对象中的所有字符串值
     */
    private void filterJsonObject(JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value == null) {
                continue;
            }
            
            if (value instanceof String) {
                // 过滤字符串值
                jsonObject.set(key, filterXss((String) value));
            } else if (value instanceof JSONObject) {
                // 递归处理嵌套JSON对象
                filterJsonObject((JSONObject) value);
            } else if (value instanceof cn.hutool.json.JSONArray) {
                // 处理JSON数组
                cn.hutool.json.JSONArray jsonArray = (cn.hutool.json.JSONArray) value;
                for (int i = 0; i < jsonArray.size(); i++) {
                    Object arrItem = jsonArray.get(i);
                    if (arrItem instanceof String) {
                        jsonArray.set(i, filterXss((String) arrItem));
                    } else if (arrItem instanceof JSONObject) {
                        filterJsonObject((JSONObject) arrItem);
                    }
                }
            }
        }
    }

    /**
     * XSS过滤核心方法
     * 使用Hutool的XssUtil.stripTags()并增强危险标签检测
     *
     * @param value 原始值
     * @return 过滤后的值
     */
    private String filterXss(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 第一步：使用Hutool的HtmlUtil进行基础HTML标签剥离
        String filtered = cn.hutool.http.HtmlUtil.filter(value);

        // 第二步：增强过滤 - 处理HTML实体编码的XSS
        filtered = removeXssPatterns(filtered);

        return filtered;
    }

    /**
     * 移除XSS攻击模式
     */
    private String removeXssPatterns(String value) {
        if (value == null) {
            return null;
        }

        // 移除javascript:协议
        value = value.replaceAll("(?i)javascript\\s*:", "");
        // 移除data:协议
        value = value.replaceAll("(?i)data\\s*:", "");
        // 移除vbscript:协议
        value = value.replaceAll("(?i)vbscript\\s*:", "");

        // 移除事件处理器（各种变体）
        String[] dangerousHandlers = {
                "onerror", "onload", "onclick", "ondblclick",
                "onmouseover", "onmouseout", "onmousedown", "onmouseup",
                "onfocus", "onblur", "onchange", "onsubmit",
                "onreset", "onselect", "onkeydown", "onkeyup", "onkeypress"
        };

        for (String handler : dangerousHandlers) {
            // 移除onxxx=或onxxx\s*=的模式
            value = value.replaceAll("(?i)" + handler + "\\s*=", "");
        }

        // 移除expression()
        value = value.replaceAll("(?i)expression\\s*\\([^)]*\\)", "");

        // 移除<style>标签内容
        value = value.replaceAll("(?i)<style[^>]*>.*?</style>", "");
        value = value.replaceAll("(?i)<style[^>]*\\s*/\\s*>", "");

        return value;
    }
}
