package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BaitaikunBrowserSettingExcelSource extends ExcelSource {

    @Getter
    Map<String, ArrayList<String>> browserSetting = new LinkedHashMap<>();
    @Getter
    LinkedHashMap<String, Integer> sortSetting = new LinkedHashMap<>();
    @Autowired
    Utility utility;
    @Getter
    String[] argsSetting;

    public BaitaikunBrowserSettingExcelSource() throws Exception {
        setSourceKind("excel.browserSetting");
        setSourceNamePattern(Settings.get("検索画面表示設定のファイル名"));
        buildEndpoint();
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        Sheet sheet = workbook.getSheet("検索画面表示設定");
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();
        ArrayList<String> needFields = new ArrayList<>();
        ArrayList<String> listFields = new ArrayList<>();
        ArrayList<String> detailFields = new ArrayList<>();
        TreeMap<Integer, String> sortFields = new TreeMap<>();
        TreeMap<Integer, Integer> sortRule = new TreeMap<>();
        while (rowIterator.hasNext()) {
            String[] values = rowToStringArray(rowIterator.next(), formatter, evaluator);
            if (values.length > 4 && utility.isNotEmpty(values)) {
                needFields.add(values[1] + "." + values[2]);
                switch (values[4]) {
                    case "一覧":
                        listFields.add(values[3]);
                        break;
                    case "詳細":
                        detailFields.add(values[3]);
                        break;
                }
                if (values.length > 6 && values[5] != null && values[6] != null) {
                    System.out.println(Arrays.toString(values));
                    System.out.println(values[5] + "\t" + values[6]);
                    if (values[5].matches("^\\d+$")) {
                        sortFields.put(Integer.parseInt(values[5]), values[1] + "." + values[2]);
                        switch (values[6]) {
                            case "昇順":
                                sortRule.put(Integer.parseInt(values[5]), 1);
                                break;
                            case "降順":
                                sortRule.put(Integer.parseInt(values[5]), -1);
                                break;
                            default:
                                sortRule.put(Integer.parseInt(values[5]), 0);
                        }
                    }
                }
            }
        }
        Sheet sheet1 = workbook.getSheet("引数設定");
        Iterator<Row> rowIterator1 = sheet1.rowIterator();
        rowIterator1.next();
        ArrayList<String> args = new ArrayList<>();
        while (rowIterator1.hasNext()) {
            String[] values = rowToStringArray(rowIterator1.next(), formatter, evaluator);
            if (values.length > 1 && utility.isNotEmpty(values)) {
                args.add(values[1]);
            }
        }
        argsSetting = args.toArray(new String[args.size()]);
        System.out.println(sortFields);
        System.out.println(sortRule);
        sortFields.entrySet().stream().filter((entry) -> (sortRule.get(entry.getKey()) != 0)).forEach((entry) -> {
            sortSetting.put(entry.getValue(), sortRule.get(entry.getKey()));
        });
        System.out.println(sortSetting);
        browserSetting.put("必要列名", needFields);
        browserSetting.put("一覧列名", listFields);
        browserSetting.put("詳細列名", detailFields);
        System.out.println(browserSetting);
        int hashCode = browserSetting.hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
    }
}
