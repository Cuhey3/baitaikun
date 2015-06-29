package com.mycode.baitaikun.sources.excel;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.Source;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public abstract class ExcelSource extends Source {

    @Setter
    String sourceNamePattern;
    @Setter
    @Getter
    String sourceName;
    @Getter
    public final List<String[]> stringArrayList = new ArrayList<>();
    public String startEndpoint;
    public int oldHash = -1;

    @Override
    public void buildEndpoint() throws Exception {
        startEndpoint = "direct:" + sourceKind;
    }

    @Override
    public void configure() {
        onException(java.io.FileNotFoundException.class).handled(true);
        from(startEndpoint)
                .bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .bean(this, "loadSheet")
                .filter().simple("${header.change}")
                .bean(this, "updated");
    }

    public Workbook openWorkbook(@Body InputStream inputStream, @Header("CamelFileName") String fileName) throws IOException {
        try {
            if (fileName.endsWith(".xlsx")) {
                return new XSSFWorkbook(inputStream);
            } else if (fileName.endsWith(".xls")) {
                return new HSSFWorkbook(inputStream);
            } else {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        stringArrayList.clear();
        stringArrayList.addAll(new Utility().sheetToStringArrayList(workbook.getSheetAt(0)));
        updateHash(header, stringArrayList.hashCode());
    }

    public void updateHash(Map header, int newHash) {
        if (oldHash != newHash) {
            header.put("change", true);
            oldHash = newHash;
        }
    }

    @Override
    public boolean isUpToDate() {
        return isReady();
    }
}
