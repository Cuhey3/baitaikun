package com.mycode.baitaikun.sources.excel.impl;

import com.monitorjbl.xlsx.StreamingReader;
import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

@Component
public class BaitaiExcelSource extends ExcelSource {

    public BaitaiExcelSource() throws Exception {
        setSourceKind("excel.baitai");
        setSourceName("媒体カレンダーのファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }

    @Override
    public void configure() {
        onException(java.io.FileNotFoundException.class).handled(true);
        from(startEndpoint)
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.print("[MESSAGE] データファイルを開いています...");
                    }
                })
                .choice().when((Exchange exchange)
                        -> exchange.getIn().getHeader(Exchange.FILE_NAME, String.class).endsWith(".xlsx"))
                .to("direct:xssf")
                .otherwise()
                .to("direct:hssf")
                .end()
                .filter().simple("${header.change}")
                .bean(this, "updated");

        from("direct:xssf").bean(this, "customLoad");

        from("direct:hssf").bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.println(" データを読み込んでいます...");
                    }
                }).bean(this, "loadSheet");
    }

    public void customLoad(@Body InputStream inputStream, @Headers Map header) throws IOException {
        Utility utility = new Utility();
        StreamingReader reader = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .sheetIndex(0)
                .read(inputStream);
        stringArrayList.clear();
        if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
            System.out.println(" データを読み込んでいます...");
        }
        for (Row row : reader) {
            Iterable<Cell> iterable = () -> row.cellIterator();
            String[] toArray = StreamSupport.stream(iterable.spliterator(), false)
                    .map((cell) -> cell.getStringCellValue().replaceAll("\r\n|\n|\r", " "))
                    .toArray((size) -> new String[size]);
            stringArrayList.add(toArray);
        }
        updateHash(header, utility.getHashCodeWhenListContainsArray(stringArrayList));
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
