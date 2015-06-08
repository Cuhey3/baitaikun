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
    LinkedHashMap<String, String> needFields;
    @Getter
    ArrayList<Map<String, String>> listFields;
    @Getter
    ArrayList<Map<String, String>> detailFields;
    @Getter
    ArrayList<String> priceFields;
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
//        String[] signs = new String[]{"_01", "_02", "_03", "_04", "_05", "_06", "_07", "_08", "_09", "_10", "_11", "_12", "_13", "_14", "_15", "_16", "_17", "_18", "_19", "_20", "_21", "_22", "_23", "_24", "_25", "_26", "_27", "_28", "_29", "_30", "_31", "_32", "_33", "_34", "_35", "_36", "_37", "_38", "_39", "_40", "_41", "_42", "_43", "_44", "_45", "_46", "_47", "_48", "_49", "_50", "_51", "_52", "_53", "_54", "_55", "_56", "_57", "_58", "_59", "_60", "_61", "_62", "_63", "_64", "_65", "_66", "_67", "_68", "_69", "_70", "_71", "_72", "_73", "_74", "_75", "_76", "_77", "_78", "_79", "_80", "_81", "_82", "_83", "_84", "_85", "_86", "_87", "_88", "_89", "_90", "_91", "_92", "_93", "_94", "_95", "_96", "_97", "_98", "_99"};

        char[] chars = "abcdefghijklmnopqrstuvwuxyz".toCharArray();
        Sheet sheet = workbook.getSheet("検索画面表示設定");
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        DataFormatter formatter = new DataFormatter();
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next();
        needFields = new LinkedHashMap<>();
        listFields = new ArrayList<>();
        detailFields = new ArrayList<>();
        priceFields = new ArrayList<>();
        TreeMap<Integer, String> sortFields = new TreeMap<>();
        TreeMap<Integer, Integer> sortRule = new TreeMap<>();
        while (rowIterator.hasNext()) {
            String[] values = rowToStringArray(rowIterator.next(), formatter, evaluator);
            if (values.length > 5 && utility.isNotEmpty(values)) {
                String fieldName = values[1] + "." + values[2];
                String sign = needFields.get(fieldName);
                if (sign == null) {
                    sign = chars[needFields.size()] + "";
                    needFields.put(fieldName, sign);
                }

                Map<String, String> m = new LinkedHashMap<>();
                m.put("sign", sign);
                m.put("exp", values[3]);
                switch (values[5]) {
                    case "一覧":
                        listFields.add(m);
                        break;
                    case "詳細":
                        detailFields.add(m);
                        break;
                }
                if (values[4].equals("金額")) {
                    priceFields.add(fieldName);
                }
                if (values.length > 7 && values[6] != null && values[7] != null) {
                    if (values[6].matches("^\\d+$")) {
                        sortFields.put(Integer.parseInt(values[6]), fieldName);
                        switch (values[7]) {
                            case "昇順":
                                sortRule.put(Integer.parseInt(values[6]), 1);
                                break;
                            case "降順":
                                sortRule.put(Integer.parseInt(values[6]), -1);
                                break;
                            default:
                                sortRule.put(Integer.parseInt(values[6]), 0);
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
        sortFields.entrySet().stream().filter((entry) -> (sortRule.get(entry.getKey()) != 0)).forEach((entry) -> {
            sortSetting.put(entry.getValue(), sortRule.get(entry.getKey()));
        });
        int hashCode = sheet.rowIterator().hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
    }
}
