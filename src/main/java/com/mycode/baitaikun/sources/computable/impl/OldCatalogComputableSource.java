package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import com.mycode.baitaikun.sources.excel.impl.ItemKeyReplaceExcelSource;
import com.mycode.baitaikun.sources.excel.impl.OldCatalogExcelSource;
import com.mycode.baitaikun.sources.excel.impl.RecordAppenderExcelSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OldCatalogComputableSource extends ComputableSource {

    @Getter
    final List<Map<String, String>> mapList = new ArrayList<>();
    @Getter
    @Setter
    public String settingName;
    @Autowired
    BaitaikunSettingsExcelSource baitaikun;
    @Autowired
    OldCatalogExcelSource oldCatalog;
    @Autowired
    ItemKeyReplaceExcelSource replacer;
    @Autowired
    RecordAppenderExcelSource appender;

    public OldCatalogComputableSource() throws Exception {
        setSourceKind("computable.oldcatalog");
        getSuperiorSourceClasses().add(BaitaikunSettingsExcelSource.class);
        getSuperiorSourceClasses().add(OldCatalogExcelSource.class);
        getSuperiorSourceClasses().add(ItemKeyReplaceExcelSource.class);
        getSuperiorSourceClasses().add(RecordAppenderExcelSource.class);
        setCheckForUpdateTime(-1L);
        setSettingName("カタログ（旧）");
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
        int skip = Integer.parseInt(baitaikun.getSettings().get(settingName).get("読み飛ばす行の数"));
        mapList.clear();
        mapList.addAll(new Utility().createMapList(oldCatalog.getStringArrayList(), skip));
        appender.appendAll(settingName, mapList);
        replacer.createItemKey(settingName, mapList);
        return null;
    }
}
