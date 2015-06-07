package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunBrowserSettingExcelSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateJsonComputableSource extends ComputableSource {

    @Autowired
    JoinAndFunctionComputableSource joinAndFunctionComputableSource;
    @Autowired
    BaitaikunBrowserSettingExcelSource baitaikunBrowserSettingExcelSource;
    @Getter
    public String json = "{}";

    public CreateJsonComputableSource() throws Exception {
        setSourceKind("computable.createJson");
        getSuperiorSourceClasses().add(JoinAndFunctionComputableSource.class);
        getSuperiorSourceClasses().add(BaitaikunBrowserSettingExcelSource.class);
        setCheckForUpdateTime(-1L);
        buildEndpoint();
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        from(computeImplEndpoint)
                .bean(this, "compute()")
                .marshal().json(JsonLibrary.Jackson)
                .bean(this, "setJson")
                .bean(this, "updated()");

        from(initImplEndpoint)
                .to("mock:initImpl");
    }

    @Override
    public Object compute() {
        List<Map<String, String>> mapList = joinAndFunctionComputableSource.getMapList();
        mapList.sort(new MyComparator(baitaikunBrowserSettingExcelSource.getSortSetting()));
        Map<String, ArrayList<String>> browserSetting = baitaikunBrowserSettingExcelSource.getBrowserSetting();
        ArrayList<String> needFields = browserSetting.get("必要列名");
        List<Map<String, String>> result = new ArrayList<>();
        char[] chars = "abcdefghijklmnopqrstuvwuxyz".toCharArray();
        mapList.stream().map((record) -> {
            Map<String, String> newRecord = new LinkedHashMap<>();
            for (int i = 0; i < needFields.size(); i++) {
                newRecord.put(chars[i] + "", record.get(needFields.get(i)));
            }
            return newRecord;
        }).forEach((newRecord) -> {
            result.add(newRecord);
        });
        ArrayList<String> listSetting = browserSetting.get("一覧列名");
        ArrayList<String> detailSetting = browserSetting.get("詳細列名");
        Map<String, String> listSettingToMap = new LinkedHashMap<>();
        Map<String, String> detailSettingToMap = new LinkedHashMap<>();
        for (int i = 0; i < listSetting.size(); i++) {
            listSettingToMap.put(chars[i] + "", listSetting.get(i));
        }
        for (int i = 0; i < detailSetting.size(); i++) {
            detailSettingToMap.put(chars[i + listSetting.size()] + "", detailSetting.get(i));
        }

        Map map = new LinkedHashMap();
        map.put("data", result);
        map.put("listColumn", listSettingToMap);
        map.put("detailColumn", detailSettingToMap);
        map.put("time", System.currentTimeMillis() + "");
        return map;
    }

    public void setJson(@Body String body) {
        json = body;
        System.out.println(json);
    }

    class MyComparator implements Comparator<Map<String, String>> {

        String[] ruleField;
        Integer[] signField;
        int fieldSize;

        public MyComparator(LinkedHashMap<String, Integer> rule) {
            ArrayList<String> keys = new ArrayList<>(rule.keySet());
            fieldSize = keys.size();
            ruleField = keys.toArray(new String[fieldSize]);
            ArrayList<Integer> signs = new ArrayList<>(rule.values());
            signField = signs.toArray(new Integer[fieldSize]);
            System.out.println(Arrays.toString(ruleField));
            System.out.println(Arrays.toString(signField));
        }

        @Override
        public int compare(Map<String, String> map1, Map<String, String> map2) {
            for (int i = 0; i < fieldSize; i++) {
                String get1 = map1.get(ruleField[i]);
                String get2 = map2.get(ruleField[i]);
                if (get1 == null && get2 == null) {
                } else if (get1 == null) {
                    return signField[i];
                } else if (get2 == null) {
                    return -signField[i];
                } else if (get1.equals(get2)) {

                } else {
                    return get1.compareTo(get2) * signField[i];
                }
            }
            return 0;
        }
    }
}
