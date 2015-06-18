package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import org.springframework.stereotype.Component;

@Component
public class BaitaiExcelSource extends ExcelSource {

    public BaitaiExcelSource() throws Exception {
        setSourceKind("excel.baitai");
        setSourceName("媒体カレンダーのファイル名");
        setSourceNamePattern(Settings.get(getSourceName()));
        buildEndpoint();
    }
}
