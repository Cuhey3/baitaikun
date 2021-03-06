package com.mycode.baitaikun.sources.excel;

import com.mycode.baitaikun.Utility;
import com.mycode.baitaikun.sources.Source;
import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

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
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.print("[MESSAGE] ファイルを開いています...");
                    }
                })
                .bean(this, "openWorkbook")
                .filter().simple("${body} is 'org.apache.poi.ss.usermodel.Workbook'")
                .process((ex) -> {
                    if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                        System.out.println(" データを読み込んでいます...");
                    }
                }).bean(this, "loadSheet")
                .filter().simple("${header.change}")
                .bean(this, "updated");
    }

    public Workbook openWorkbook(@Body InputStream inputStream, @Header("CamelFileName") String fileName) throws IOException {
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            return workbook;
        } catch (Throwable t) {
            return null;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public void loadSheet(@Body Workbook workbook, @Headers Map header) {
        Utility utility = new Utility();
        stringArrayList.clear();
        stringArrayList.addAll(utility.sheetToStringArrayList(workbook.getSheetAt(0)));
        updateHash(header, utility.getHashCodeWhenListContainsArray(stringArrayList));
    }

    public void updateHash(Map header, int newHash) {
        if (oldHash != newHash) {
            header.put("change", true);
            oldHash = newHash;
        } else {
            System.out.println("[MESSAGE] 内容が同一のため、再計算は行いません。" + this.getClass().getSimpleName() + "\n[MESSAGE]");
        }
    }

    @Override
    public boolean isUpToDate() {
        return isReady();
    }
}
