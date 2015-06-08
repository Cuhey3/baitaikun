package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunBrowserSettingExcelSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
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
    @Getter
    @Setter
    public String time;
    private final Pattern noDigit = Pattern.compile("[^\\d]");
    private final Pattern digit = Pattern.compile("^(\\d+)(\\d{4})$");
    
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
        LinkedHashMap<String, String> needFields = baitaikunBrowserSettingExcelSource.getNeedFields();
        ArrayList<String> priceFields = baitaikunBrowserSettingExcelSource.getPriceFields();
        List<Map<String, String>> result = new ArrayList<>();
        mapList.stream().map((record) -> {
            Map<String, String> newRecord = new LinkedHashMap<>();
            needFields.entrySet().stream().forEach((entry) -> {
                if (priceFields.contains(entry.getKey())) {
                    newRecord.put(entry.getValue(), convertPrice(record.get(entry.getKey())));
                } else {
                    newRecord.put(entry.getValue(), record.get(entry.getKey()));
                }
            });
            return newRecord;
        }).forEach((newRecord) -> {
            result.add(newRecord);
        });
        ArrayList<Map<String, String>> listSetting = baitaikunBrowserSettingExcelSource.getListFields();
        ArrayList<Map<String, String>> detailSetting = baitaikunBrowserSettingExcelSource.getDetailFields();
        Map map = new LinkedHashMap();
        map.put("data", result);
        map.put("listColumn", listSetting);
        map.put("detailColumn", detailSetting);
        long currentTimeMillis = System.currentTimeMillis();
        map.put("time", currentTimeMillis + "");
        this.setTime(currentTimeMillis + "");
        return map;
    }
    
    public void setJson(@Body String body) {
        json = body;
    }
    
    public String convertPrice(String price) {
        if (price == null) {
            return null;
        } else {
            String replace = noDigit.matcher(Normalizer.normalize(price, Normalizer.Form.NFKC)).replaceAll("");
            if (replace.length() > 0) {
                if (replace.length() > 4) {
                    replace = digit.matcher(replace).replaceFirst("$1万$2");
                }
                return replace + " 円";
            } else {
                return price;
            }
        }
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
