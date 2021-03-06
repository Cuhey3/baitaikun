package com.mycode.baitaikun.sources.excel.impl;

import com.monitorjbl.xlsx.StreamingReader;
import com.monitorjbl.xlsx.impl.StreamingRow;
import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.IntStream;
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
                        System.out.print("[MESSAGE] ファイルを開いています...");
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
        try {
            StreamingReader reader = StreamingReader.builder()
                    .rowCacheSize(100)
                    .bufferSize(4096)
                    .sheetIndex(0)
                    .read(inputStream);
            stringArrayList.clear();
            if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                System.out.println(" データを読み込んでいます...");
            }
            int max = 0;
            for (Row row : reader) {
                max = Math.max(max, ((StreamingRow) row).getCellMap().size());
                stringArrayList.add(IntStream.range(0, max)
                        .mapToObj((i) -> {
                            Cell cell = row.getCell(i);
                            if (cell == null) {
                                return "";
                            } else {
                                return cell.getStringCellValue().replaceAll("\r\n|\n|\r", " ");
                            }
                        })
                        .toArray((size) -> new String[size]));
            }
            updateHash(header, new Utility().getHashCodeWhenListContainsArray(stringArrayList));
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
}
