package com.mycode.baitaikun.sources.excel.impl;

import com.google.common.collect.HashBiMap;
import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BaitaikunBrowserSettingExcelSource extends ExcelSource {

    @Getter
    final LinkedHashMap<String, String> needFields = new LinkedHashMap<>();
    @Getter
    final ArrayList<Map<String, String>> listFields = new ArrayList<>();
    @Getter
    final ArrayList<Map<String, String>> detailFields = new ArrayList<>();
    @Getter
    final ArrayList<String> priceFields = new ArrayList<>();
    @Getter
    final Map<String, String> keywordToClassName = new LinkedHashMap<>();
    @Getter
    final HashBiMap<String, String> classNameToCSSStyle = HashBiMap.create();
    @Getter
    String css = "";
    @Getter
    final LinkedHashMap<String, Integer> sortSetting = new LinkedHashMap<>();
    @Getter
    String[] argsSetting;
    @Autowired
    Utility utility;

    public BaitaikunBrowserSettingExcelSource() throws Exception {
        setSourceKind("excel.browserSetting");
        setSourceName("検索画面表示設定のファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }

    @Override
    public void configure() {
        onException(java.io.FileNotFoundException.class).handled(true);
        from(startEndpoint)
                .bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .bean(this, "loadSheet")
                .choice().when().simple("${header.change}")
                .bean(this, "updated").to("direct:waitSetting");
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        char[] chars = "abcdefghijklmnopqrstuvwuxyz".toCharArray();
        needFields.clear();
        listFields.clear();
        detailFields.clear();
        priceFields.clear();
        keywordToClassName.clear();
        classNameToCSSStyle.clear();
        sortSetting.clear();
        css = "";
        TreeMap<Integer, String> sortFields = new TreeMap<>();
        TreeMap<Integer, Integer> sortRule = new TreeMap<>();
        utility.sheetToStringArrayList(workbook.getSheet("検索画面表示設定")).stream()
                .skip(1)
                .filter((values) -> {
                    return values.length > 5 && utility.isAnyNotEmpty(values); // 大丈夫？？？
                }).filter((values) -> {
                    String fieldName = values[1] + "." + values[2];
                    String sign = needFields.get(fieldName);
                    if (sign == null) {
                        sign = chars[needFields.size()] + "";
                        needFields.put(fieldName, sign);
                    }
                    return true;
                }).filter((values) -> {
                    if (values[4].equals("金額")) {
                        priceFields.add(values[1] + "." + values[2]);
                    }
                    return true;
                }).filter((values) -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("sign", needFields.get(values[1] + "." + values[2]));
                    m.put("exp", values[3]);
                    switch (values[5]) {
                        case "一覧":
                            listFields.add(m);
                            break;
                        case "詳細":
                            detailFields.add(m);
                            break;
                    }
                    return true;
                }).filter((values) -> {
                    return values.length > 7
                    && values[6] != null && !values[6].isEmpty()
                    && values[7] != null && !values[7].isEmpty()
                    && values[6].matches("^\\d+$");
                })
                .forEach((values) -> {
                    sortFields.put(Integer.parseInt(values[6]), values[1] + "." + values[2]);
                    switch (values[7]) {
                        case "昇順":
                            sortRule.put(Integer.parseInt(values[6]), 1);
                            break;
                        case "降順":
                            sortRule.put(Integer.parseInt(values[6]), -1);
                            break;
                        default:
                            sortRule.put(Integer.parseInt(values[6]), 0);
                    }
                });

        sortFields.entrySet().stream()
                .filter((entry) -> (sortRule.get(entry.getKey()) != 0))
                .forEach((entry) -> {
                    sortSetting.put(entry.getValue(), sortRule.get(entry.getKey()));
                });

        argsSetting = utility.sheetToStringArrayList(workbook.getSheet("引数設定")).stream()
                .filter((values) -> {
                    return values.length > 1 && values[1] != null && !values[1].isEmpty();
                }).map((values) -> {
                    return values[1];
                }).toArray((size) -> new String[size]);

        StringBuilder sb = new StringBuilder();
        utility.sheetToStringArrayList(workbook.getSheet("強調キーワード")).stream()
                .skip(1)
                .filter((values) -> {
                    return values.length > 1 && utility.isAnyNotEmpty(values);
                }).forEach((values) -> {
                    String style = Normalizer.normalize(values[1], Normalizer.Form.NFKC);
                    String className = classNameToCSSStyle.inverse().get(style);
                    if (className == null) {
                        className = "highlight" + classNameToCSSStyle.size();
                        classNameToCSSStyle.put(className, values[1]);
                        sb.append(String.format(".%s{%s}", className, style)).append("\r\n");
                    }
                    keywordToClassName.put(values[0], className);
                });
        css = new String(sb);
        int[] hash = new int[]{
            needFields.hashCode(),
            listFields.hashCode(),
            detailFields.hashCode(),
            priceFields.hashCode(),
            keywordToClassName.hashCode(),
            classNameToCSSStyle.hashCode(),
            css.hashCode(),
            sortSetting.hashCode(),
            Arrays.hashCode(argsSetting)
        };
        updateHash(header, Arrays.hashCode(hash));
    }
}
