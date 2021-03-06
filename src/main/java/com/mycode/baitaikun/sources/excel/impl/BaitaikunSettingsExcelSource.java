package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.FileBroker;
import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

@Component
public class BaitaikunSettingsExcelSource extends ExcelSource {

    @Getter
    Map<String, Map<String, String>> settings = Collections.synchronizedMap(new LinkedHashMap<>());

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
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.println("[MESSAGE] ファイルの変更を検出しました。" + ex.getIn().getHeader(Exchange.FILE_NAME_ONLY, String.class));
                    }
                })
                .to("file:backup/設定?fileName=${file:onlyname.noext}${date:now:yyyyMMdd}.${file:ext}")
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.print("[MESSAGE] ファイルを開いています...");
                    }
                })
                .bean(this, "openWorkbook")
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.println(" データを読み込んでいます...");
                    }
                })
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .bean(this, "loadSheet")
                .filter().simple("${header.change}")
                .bean(this, "updated");
    }

    @Override
    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        settings.clear();
        Utility utility = new Utility();
        IntStream.range(0, workbook.getNumberOfSheets())
                .mapToObj((i)
                        -> workbook.getSheetAt(i))
                .forEach((sheet) -> {
                    String sheetName = sheet.getSheetName();
                    List<String[]> sal = utility.sheetToStringArrayList(sheet);
                    Map<String, String> setting = new LinkedHashMap<>();
                    sal.stream().skip(1)
                    .forEach((values) -> {
                        if (utility.isFilled(values, 2)) {
                            setting.put(values[0], values[1]);
                        }
                    });
                    settings.put(sheetName, setting);
                });
        updateHash(header, settings.hashCode());

        factory.getBean(FileBroker.class).createPatternToSlipUri();
        CamelContext context = factory.getBean(CamelContext.class);
        try {
            if (context.getRouteStatus("fileBrokerRoute").isStopped()) {
                context.startRoute("fileBrokerRoute");
            }
            if (context.getRouteStatus("broker.notate").isStopped()) {
                context.startRoute("broker.notate");
            }
        } catch (Exception ex) {
        }
    }
}
