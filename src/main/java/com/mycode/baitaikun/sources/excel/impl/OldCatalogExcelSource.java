package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import org.springframework.stereotype.Component;

@Component
public class OldCatalogExcelSource extends ExcelSource {

    public OldCatalogExcelSource() throws Exception {
        setSourceKind("excel.oldcatalog");
        setSourceNamePattern(Settings.get("カタログ（旧）のファイル名"));
        buildEndpoint();
    }
}
