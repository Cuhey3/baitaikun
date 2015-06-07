package com.mycode.baitaikun.sources.excel.impl;

import com.mycode.baitaikun.Settings;
import com.mycode.baitaikun.sources.excel.ExcelSource;
import org.springframework.stereotype.Component;

@Component
public class NoukiExcelSource extends ExcelSource {

    public NoukiExcelSource() throws Exception {
        setSourceKind("excel.nouki");
        setSourceNamePattern(Settings.get("納期案内一覧のファイル名"));
        buildEndpoint();
    }
}
