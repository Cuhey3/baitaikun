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
    final public Map<String, Map<String, String>> itemKeyToMap = new LinkedHashMap<>();
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
        itemKeyToMap.clear();
        updateItemKeyToMap(nouki.getMapList(), nouki.getSettingName());
        updateItemKeyToMap(newCatalog.getMapList(), newCatalog.getSettingName());
        updateItemKeyToMap(oldCatalog.getMapList(), oldCatalog.getSettingName());
        return null;
    }

    private void updateItemKeyToMap(List<Map<String, String>> mapList, String settingName) {
        mapList.stream().forEach((map) -> {
            String itemKey = map.get("ITEM_KEY");
            Map<String, String> exist
                    = itemKeyToMap.containsKey(itemKey) ? itemKeyToMap.get(itemKey) : new LinkedHashMap<>();
            map.entrySet().stream()
                    .forEach((entry) -> {
                        exist.put(settingName + "." + entry.getKey(), entry.getValue());
                    });
            itemKeyToMap.put(itemKey, exist);
        });
    }
}
