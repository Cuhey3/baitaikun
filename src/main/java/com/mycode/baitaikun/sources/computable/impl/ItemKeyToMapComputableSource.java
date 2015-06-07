package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemKeyToMapComputableSource extends ComputableSource {

    @Getter
    public Map<String, Map<String, String>> itemKeyToMap;
    @Autowired
    NoukiComputableSource nouki;
    @Autowired
    NewCatalogComputableSource newCatalog;
    @Autowired
    OldCatalogComputableSource oldCatalog;

    public ItemKeyToMapComputableSource() throws Exception {
        setSourceKind("computable.itemKeyToMap");
        getSuperiorSourceClasses().add(NoukiComputableSource.class);
        getSuperiorSourceClasses().add(NewCatalogComputableSource.class);
        getSuperiorSourceClasses().add(OldCatalogComputableSource.class);
        setCheckForUpdateTime(-1L);
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
        itemKeyToMap = new LinkedHashMap<>();
        List<Map<String, String>> mapList = nouki.getMapList();
        String settingName = nouki.getSettingName();
        for (Map<String, String> map : mapList) {
            String itemKey = map.get("ITEM_KEY");
            Map<String, String> exist = itemKeyToMap.get(itemKey);
            if (exist == null) {
                exist = new LinkedHashMap<>();
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                exist.put(settingName + "." + entry.getKey(), entry.getValue());
            }
            itemKeyToMap.put(itemKey, exist);
        }
        mapList = newCatalog.getMapList();
        settingName = newCatalog.getSettingName();
        for (Map<String, String> map : mapList) {
            String itemKey = map.get("ITEM_KEY");
            Map<String, String> exist = itemKeyToMap.get(itemKey);
            if (exist == null) {
                exist = new LinkedHashMap<>();
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                exist.put(settingName + "." + entry.getKey(), entry.getValue());
            }
            itemKeyToMap.put(itemKey, exist);
        }
        mapList = oldCatalog.getMapList();
        settingName = oldCatalog.getSettingName();
        for (Map<String, String> map : mapList) {
            String itemKey = map.get("ITEM_KEY");
            Map<String, String> exist = itemKeyToMap.get(itemKey);
            if (exist == null) {
                exist = new LinkedHashMap<>();
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                exist.put(settingName + "." + entry.getKey(), entry.getValue());
            }
            itemKeyToMap.put(itemKey, exist);
        }
        //System.out.println(itemKeyToMap);
        return null;
    }
}
