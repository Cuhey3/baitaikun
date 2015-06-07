package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import org.springframework.stereotype.Component;

@Component
public class NewCatalogExcelSource extends ExcelSource {

    public NewCatalogExcelSource() throws Exception {
        setSourceKind("excel.newcatalog");
        setSourceNamePattern(Settings.get("カタログ（最新）のファイル名"));
        buildEndpoint();
    }
}
