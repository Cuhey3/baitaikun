package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class ItemKeyReplaceExcelSource extends ExcelSource {

    @Getter
    Map<String, Map<String, Set<String>>> replaceSources = new LinkedHashMap<>();

    public ItemKeyReplaceExcelSource() throws Exception {
        setSourceKind("excel.itemKeyReplace");
        setSourceNamePattern(Settings.get("商品コード置換リストのファイル名"));
        buildEndpoint();
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        replaceSources.clear();
        String[] sheetNames = new String[]{"納期案内", "媒体一覧", "カタログ（最新）", "カタログ（旧）"};
        for (String s : sheetNames) {
            Sheet sheet = workbook.getSheet(s);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.rowIterator();
            rowIterator.next();
            Map<String, Set<String>> replaceSource = new LinkedHashMap<>();
            while (rowIterator.hasNext()) {

                String[] values = rowToStringArray(rowIterator.next(), formatter, evaluator);
                if (values[0] != null && !values[0].isEmpty() && values[1] != null && !values[1].isEmpty()) {
                    Set<String> replace = replaceSource.get(values[0]);
                    if (replace == null) {
                        replace = new HashSet<>();
                    }
                    if (values.length == 3 && values[2].length() > 0) {
                        replace.add(values[1] + "#" + values[2]);
                    } else {
                        replace.add(values[1]);
                    }
                    replaceSource.put(values[0], replace);
                }
            }
            replaceSources.put(s, replaceSource);
        }
        int hashCode = replaceSources.hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
    }

    public Set<String> getReplacedKeys(String sheetName, String itemKey, String itemName) {
        Set<String> replace = replaceSources.get(sheetName).get(itemKey);
        Set<String> replacedKeys = new LinkedHashSet<>();
        replace.stream().forEach((r) -> {
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
        Iterator<Map<String, String>> iterator = mapList.iterator();
        List<Map<String, String>> newRows = new ArrayList<>();
        while (iterator.hasNext()) {
            Map<String, String> row = iterator.next();
            String code = row.get(codeField);
            String node = row.get(nodeField);
            code = Normalizer.normalize(code, Normalizer.Form.NFKC).replaceAll("[^\\da-zA-Z\\*]", "");
            if (node.contains(" ")) {
                String[] nodeSplit = node.split(" ");
                for (String n : nodeSplit) {
                    n = Normalizer.normalize(n, Normalizer.Form.NFKC).replaceAll("[^\\da-zA-Z\\*]", "");
                    String itemKey = code + "-" + n;
                    if (itemKeyReplacer.hasReplacedKey(sheetName, itemKey)) {
                        Set<String> replacedKeys = itemKeyReplacer.getReplacedKeys(sheetName, itemKey, row.get(itemNameField));
                        replacedKeys.stream().filter((s) -> (!s.equals("削除"))).map((s) -> {
                            row.put("ITEM_KEY", s);
                            return s;
                        }).map((_item) -> {
                            return new LinkedHashMap<>(row);
                        }).forEach(newRows::add);
                    } else {
                        row.put("ITEM_KEY", itemKey);
                        Map<String, String> newRow = new LinkedHashMap<>(row);
                        newRows.add(newRow);
                    }
                }
                iterator.remove();
            } else {
                node = Normalizer.normalize(node, Normalizer.Form.NFKC).replaceAll("[^\\da-zA-Z\\*]", "");
                String itemKey;
                if (node.isEmpty()) {
                    itemKey = code;
                } else {
                    itemKey = code + "-" + node;
                }
                if (itemKey.isEmpty()) {
                    iterator.remove();
                } else {
                    if (itemKeyReplacer.hasReplacedKey(sheetName, itemKey)) {
                        Set<String> replacedKeys = itemKeyReplacer.getReplacedKeys(sheetName, itemKey, row.get(itemNameField));
                        replacedKeys.stream().map((s) -> {
                            row.put("ITEM_KEY", s);
                            return s;
                        }).map((_item) -> new LinkedHashMap<>(row)).forEach((newRow) -> {
                            newRows.add(newRow);
                        });
                        iterator.remove();
                    } else {
                        row.put("ITEM_KEY", itemKey);
                    }
                }
            }
        }
        mapList.addAll(newRows);
    }

    public Map<String, Map<String, String>> createItemKeyToMap(List<Map<String, String>> mapList) {
        Map<String, Map<String, String>> itemKeyToMap = new LinkedHashMap<>();
        mapList.stream().forEach((map) -> {
            String itemKey = map.get("ITEM_KEY");
            itemKeyToMap.put(itemKey, map);
        });
        return itemKeyToMap;
    }
}
