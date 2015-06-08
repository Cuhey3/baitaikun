package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import com.mycode.baitaikun.sources.excel.impl.ItemKeyReplaceExcelSource;
import com.mycode.baitaikun.sources.excel.impl.NoukiExcelSource;
import com.mycode.baitaikun.sources.excel.impl.RecordAppenderExcelSource;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NoukiComputableSource extends ComputableSource {

    @Getter
    List<Map<String, String>> mapList;
    @Getter
    public Map<String, Map<String, String>> itemKeyToMap;
    @Getter
    @Setter
    public String settingName;
    @Autowired
    Utility utility;
    @Autowired
    BaitaikunSettingsExcelSource baitaikun;
    @Autowired
    NoukiExcelSource nouki;
    @Autowired
    RecordAppenderExcelSource appender;
    @Autowired
    ItemKeyReplaceExcelSource replacer;
    private final Pattern datePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/([12]\\d)");

    public NoukiComputableSource() throws Exception {
        setSourceKind("computable.nouki");
        getSuperiorSourceClasses().add(BaitaikunSettingsExcelSource.class);
        getSuperiorSourceClasses().add(NoukiExcelSource.class);
        getSuperiorSourceClasses().add(ItemKeyReplaceExcelSource.class);
        getSuperiorSourceClasses().add(RecordAppenderExcelSource.class);
        setCheckForUpdateTime(-1L);
        setSettingName("納期案内");
        buildEndpoint();
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        from(computeImplEndpoint)
                .bean(this, "compute()")
                .bean(this, "updated()");

        from(initImplEndpoint)
                .to("mock:initImpl");
    }

    @Override
    public Object compute() {

        int skip = Integer.parseInt(baitaikun.getSettings().get(settingName).get("読み飛ばす行の数"));
        mapList = utility.createMapList(nouki.getStringArrayList(), skip);
        appender.appendAll(settingName, mapList);
        replacer.createItemKey(settingName, mapList);
        String noukiField = baitaikun.getSettings().get("納期案内").get("納期の列名");
        String dateField = baitaikun.getSettings().get("納期案内").get("日付の列名");
        mapList.stream().forEach(map -> {
            noukiClean(noukiField, map);
            formatDate(map, dateField);
        });

        return null;
    }

    public void noukiClean(String noukiField, Map<String, String> map) {
        String noukiExpression = map.get(noukiField);
        if (noukiExpression != null) {
            map.put(noukiField, Normalizer.normalize(noukiExpression, Normalizer.Form.NFKC).replaceAll("納期 ?(?=\\d)", ""));
        }
    }

    public void formatDate(Map<String, String> map, String dateField) {
        String date = map.get(dateField);
        if (date != null) {
            Matcher m = datePattern.matcher(date);
            if (m.find()) {
                String month = m.group(1);
                if (month.length() == 1) {
                    month = "0" + month;
                }
                String day = m.group(2);
                if (day.length() == 1) {
                    day = "0" + day;
                }
                map.put(dateField, "20" + m.group(3) + "/" + month + "/" + day);
            }
        }
    }
}
