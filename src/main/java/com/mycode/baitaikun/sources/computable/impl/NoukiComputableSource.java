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
        mapList.stream().forEach(map -> {
            noukiClean(noukiField, map);
        });

        return null;
    }

    public void noukiClean(String noukiField, Map<String, String> map) {
        String noukiExpression = map.get(noukiField);
        if (noukiExpression != null) {
            map.put(noukiField, Normalizer.normalize(noukiExpression, Normalizer.Form.NFKC).replaceAll("納期 ?(?=\\d)", ""));
        }
    }
}
