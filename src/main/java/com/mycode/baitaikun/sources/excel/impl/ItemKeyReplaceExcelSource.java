package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class ItemKeyReplaceExcelSource extends ExcelSource {

    @Getter
    Map<String, Map<String, Set<String>>> replaceSources = new LinkedHashMap<>();

    public ItemKeyReplaceExcelSource() throws Exception {
        setSourceKind("excel.itemKeyReplace");
        setSourceName("アイテムキー置換リストのファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        Utility utility = new Utility();
        replaceSources.clear();
        Stream.of("納期案内", "媒体一覧", "カタログ（最新）", "カタログ（旧）")
                .forEach((sheetName) -> {
                    Sheet sheet = workbook.getSheet(sheetName);
                    List<String[]> sal = utility.sheetToStringArrayList(sheet);
                    Map<String, Set<String>> replaceSource = new LinkedHashMap<>();
                    sal.stream().skip(1)
                    .forEach((values) -> {
                        if (utility.isFilled(values, 2)) {
                            Set<String> replace = replaceSource.containsKey(values[0]) ? replaceSource.get(values[0]) : new HashSet<>();
                            if (utility.isFilled(values, 3)) {
                                replace.add(values[1] + "#" + values[2]);
                            } else {
                                replace.add(values[1]);
                            }
                            replaceSource.put(values[0], replace);
                        }
                    });
                    replaceSources.put(sheetName, replaceSource);
                });
        updateHash(header, replaceSources.hashCode());
    }

    public Set<String> getReplacedKeys(String sheetName, String itemKey, String itemName) {
        Set<String> replace = replaceSources.get(sheetName).get(itemKey);
        Set<String> replacedKeys = new LinkedHashSet<>();
        replace.stream()
                .forEach((r) -> {
                    if (r.contains("#")) {
                        if (r.endsWith("#" + itemName)) {
                            replacedKeys.add(r.split("#")[0]);
                        } else {
                            replacedKeys.add(itemKey);
                        }
                    } else {
                        replacedKeys.add(r);
                    }
                });
        return replacedKeys;
    }

    public boolean hasReplacedKey(String sheetName, String itemKey) {
        return replaceSources.get(sheetName).containsKey(itemKey);
    }

    public void createItemKey(String sheetName, List<Map<String, String>> mapList) {
        Map<String, Map<String, String>> settings = getFactory().getBean(BaitaikunSettingsExcelSource.class).getSettings();
        ItemKeyReplaceExcelSource itemKeyReplacer = getFactory().getBean(ItemKeyReplaceExcelSource.class);
        Map<String, String> setting = settings.get(sheetName);
        String codeField = setting.get("商品コードの列名");
        String nodeField = setting.get("商品ノードの列名");
        String itemNameField = setting.get("商品名の列名");
        List<Map<String, String>> newRows = new ArrayList<>();

        mapList.stream()
                .flatMap((map)
                        -> Stream.of(map.get(nodeField).split(" "))
                        .map((n) -> {
                            String itemKey = Normalizer.normalize(map.get(codeField), Normalizer.Form.NFKC).replaceAll("[^\\da-zA-Z\\*]", "");
                            n = Normalizer.normalize(n, Normalizer.Form.NFKC).replaceAll("[^\\da-zA-Z\\*]", "");
                            itemKey = n.isEmpty() ? itemKey : itemKey + "-" + n;
                            Map<String, String> newRow = new LinkedHashMap<>();
                            newRow.putAll(map);
                            newRow.put("ITEM_KEY", itemKey);
                            return newRow;
                        })
                )
                .flatMap((newRow) -> {
                    String itemKey = newRow.get("ITEM_KEY");
                    if (itemKeyReplacer.hasReplacedKey(sheetName, itemKey)) {
                        return itemKeyReplacer.getReplacedKeys(sheetName, itemKey, newRow.get(itemNameField)).stream()
                        .filter((newKey)
                                -> !newKey.equals("削除"))
                        .map((newKey) -> {
                            Map<String, String> replacedRow = new LinkedHashMap<>();
                            replacedRow.putAll(newRow);
                            replacedRow.put("ITEM_KEY", newKey);
                            return replacedRow;
                        });
                    } else {
                        return Stream.of(newRow);
                    }
                })
                .forEach(newRows::add);
        mapList.clear();
        mapList.addAll(newRows);
    }

    public Map<String, Map<String, String>> createItemKeyToMap(List<Map<String, String>> mapList) {
        Map<String, Map<String, String>> itemKeyToMap = new LinkedHashMap<>();
        mapList.stream()
                .forEach((map) -> {
                    String itemKey = map.get("ITEM_KEY");
                    itemKeyToMap.put(itemKey, map);
                });
        return itemKeyToMap;
    }
}
