package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import org.springframework.stereotype.Component;

@Component
public class OldCatalogExcelSource extends ExcelSource {

    public OldCatalogExcelSource() throws Exception {
        setSourceKind("excel.oldcatalog");
        setSourceName("カタログ（旧）のファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }
}
