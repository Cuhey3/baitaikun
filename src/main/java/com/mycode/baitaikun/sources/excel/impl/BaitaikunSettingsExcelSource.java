package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.FileBroker;
import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Headers;
import org.apache.camel.Route;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class BaitaikunSettingsExcelSource extends ExcelSource {
    
    @Getter
    Map<String, Map<String, String>> settings = new LinkedHashMap<>();
    
    public BaitaikunSettingsExcelSource() throws IOException, Exception {
        setSourceKind("excel.settings");
        setSourceName("媒体くん詳細設定のファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }
    
    @Override
    public void configure() {
        onException(java.io.FileNotFoundException.class).handled(true);
        from("direct:excel.settings")
                .to("file:backup/設定?fileName=${file:onlyname.noext}${date:now:yyyyMMdd}.${file:ext}")
                .bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .bean(this, "loadSheet")
                .filter().simple("property.CamelBatchComplete")
                .choice().when().simple("${header.change}")
                .bean(this, "updated");
    }
    
    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        settings.clear();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) {
                continue;
            }
            rowIterator.next();
            Map<String, String> setting = new LinkedHashMap<>();
            while (rowIterator.hasNext()) {
                String[] values = rowToStringArray(rowIterator.next(), formatter, evaluator);
                if (values[0] != null && !values[0].isEmpty() && values[1] != null && !values[1].isEmpty()) {
                    setting.put(values[0], values[1]);
                }
            }
            settings.put(sheetName, setting);
        }
        int hashCode = settings.hashCode();
        if (oldHash != hashCode) {
            header.put("change", true);
            oldHash = hashCode;
        }
        CamelContext context = factory.getBean(CamelContext.class);
        try {
            if (context.getRouteStatus("fileBrokerRoute").isStopped()) {
                context.startRoute("fileBrokerRoute");
            }
            
            factory.getBean(FileBroker.class).createPatternToSlipUri();
        } catch (Exception ex) {
            Logger.getLogger(BaitaikunSettingsExcelSource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
