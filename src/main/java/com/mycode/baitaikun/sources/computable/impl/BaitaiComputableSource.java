package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaiExcelSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import com.mycode.baitaikun.sources.excel.impl.ItemKeyReplaceExcelSource;
import com.mycode.baitaikun.sources.excel.impl.RecordAppenderExcelSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BaitaiComputableSource extends ComputableSource {

    @Getter
    final List<Map<String, String>> mapList = new ArrayList<>();
    @Getter
    @Setter
    public String settingName;
    @Autowired
    BaitaikunSettingsExcelSource settings_e;
    @Autowired
    BaitaiExcelSource baitai_e;
    @Autowired
    RecordAppenderExcelSource appender_e;
    @Autowired
    ItemKeyReplaceExcelSource itemKeyReplace_e;
    @Autowired
    ItemKeyToMapComputableSource itemKeyToMap_c;
    private final Pattern datePattern = Pattern.compile("(\\d{4}/)(\\d{1,2}/)(\\d{1,2})");
    private final Pattern serialDatePattern = Pattern.compile("^[3456789]\\d{4}$");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

    public BaitaiComputableSource() throws Exception {
        setSourceKind("computable.baitai");
        getSuperiorSourceClasses().add(BaitaikunSettingsExcelSource.class);
        getSuperiorSourceClasses().add(BaitaiExcelSource.class);
        getSuperiorSourceClasses().add(ItemKeyReplaceExcelSource.class);
        getSuperiorSourceClasses().add(ItemKeyToMapComputableSource.class);
        getSuperiorSourceClasses().add(RecordAppenderExcelSource.class);
        setCheckForUpdateTime(-1L);
        setSettingName("媒体一覧");
        buildEndpoint();
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        from(computeImplEndpoint)
                .bean(this, "compute()")
                .bean(this, "updated()");
    }

    @Override
    public Object compute() {
        Map<String, Map<String, String>> settings = settings_e.getSettings();
        int skip = Integer.parseInt(settings.get(settingName).get("読み飛ばす行の数"));
        mapList.clear();
        mapList.addAll(new Utility().createMapList(baitai_e.getStringArrayList(), skip));
        formatDate(mapList, settings.get(settingName).get("日付の列名"), settings.get(settingName).get("日付エラーに付加する値"));
        createRecordFromCatalog("カタログ（最新）");
        createRecordFromCatalog("カタログ（旧）");
        appender_e.appendAll(settingName, mapList);
        itemKeyReplace_e.createItemKey(settingName, mapList);
        return null;
    }

    public void createRecordFromCatalog(String catalogName) {
        Map<String, String> catalogSettings = settings_e.getSettings().get(catalogName);
        Map<String, String> referFields = new LinkedHashMap<>();
        Map<String, String> fillFields = new LinkedHashMap<>();
        settings_e.getSettings().get(settingName).entrySet().stream()
                .forEach((entry) -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key.startsWith("カタログの値を参照する列名")) {
                        referFields.put(value, catalogSettings.get(value));
                    } else if (key.startsWith("カタログ設定から値を補う列名")) {
                        fillFields.put(value, catalogSettings.get(value));
                    }
                });
        itemKeyToMap_c.getItemKeyToMap().values().stream()
                .filter(map
                        -> map.containsKey(catalogName + ".ITEM_KEY"))
                .map(map -> {
                    Map<String, String> record = new LinkedHashMap<>();
                    referFields.entrySet().stream().forEach((entry)
                            -> record.put(entry.getKey(), map.get(catalogName + "." + entry.getValue())));
                    fillFields.entrySet().stream().forEach((entry)
                            -> record.put(entry.getKey(), entry.getValue()));
                    return record;
                })
                .forEach(mapList::add);
    }

    public void formatDate(List<Map<String, String>> mapList, String dateField, String errorValue) {
        mapList.stream()
                .forEach((map) -> {
                    String date = map.get(dateField);
                    if (date == null) {
                        map.put(dateField, errorValue);
                    } else {
                        Matcher m = datePattern.matcher(date);
                        if (m.find()) {
                            String month = m.group(2);
                            if (month.length() == 2) {
                                month = "0" + month;
                            }
                            String day = m.group(3);
                            if (day.length() == 1) {
                                day = "0" + day;
                            }
                            map.put(dateField, m.group(1) + month + day);
                        } else {
                            if (serialDatePattern.matcher(date).find()) {
                                Calendar cal = Calendar.getInstance();
                                cal.set(1900, 0, -1);
                                cal.add(Calendar.HOUR, 24 * Integer.parseInt(date));
                                map.put(dateField, sdf.format(cal.getTime()));
                            } else {
                                map.put(dateField, errorValue + date);
                            }
                        }
                    }
                });
    }
}
