package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import com.mycode.baitaikun.sources.excel.impl.ItemKeyReplaceExcelSource;
import com.mycode.baitaikun.sources.excel.impl.NewCatalogExcelSource;
import com.mycode.baitaikun.sources.excel.impl.RecordAppenderExcelSource;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NewCatalogComputableSource extends ComputableSource {

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
    NewCatalogExcelSource newCatalog;
    @Autowired
    RecordAppenderExcelSource appender;
    @Autowired
    ItemKeyReplaceExcelSource replacer;

    public NewCatalogComputableSource() throws Exception {
        setSourceKind("computable.newcatalog");
        getSuperiorSourceClasses().add(BaitaikunSettingsExcelSource.class);
        getSuperiorSourceClasses().add(NewCatalogExcelSource.class);
        getSuperiorSourceClasses().add(ItemKeyReplaceExcelSource.class);
        getSuperiorSourceClasses().add(RecordAppenderExcelSource.class);
        setCheckForUpdateTime(-1L);
        setSettingName("カタログ（最新）");
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
        mapList = utility.createMapList(newCatalog.getStringArrayList(), skip);
        appender.appendAll(settingName, mapList);
        replacer.createItemKey(settingName, mapList);
        return null;
    }
}
