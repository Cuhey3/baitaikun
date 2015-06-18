package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Headers;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class FileBroker extends RouteBuilder {

    Map<Pattern, String> patternToSlipUri = null;
    @Autowired
    DefaultListableBeanFactory factory;

    @Override
    public void configure() throws Exception {
        fromF("file:%s?noop=true&delay=3000&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&include=%s&recursive=true", Settings.get("媒体くん用フォルダの場所"), Settings.get("媒体くん詳細設定のファイル名"))
                .routeId("settingRoute").to("direct:excel.settings");
        fromF("file:%s?noop=true&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&recursive=true", Settings.get("媒体くん用フォルダの場所"))
                .autoStartup(false)
                .routeId("fileBrokerRoute")
                .bean(this, "checkFileName")
                .routingSlip(simple("header.slipUri"));
    }

    public void checkFileName(@Headers Map headers) {
        String name = (String) headers.get(Exchange.FILE_NAME_ONLY);
        if (patternToSlipUri == null) {
            createPatternToSlipUri();
        }
        Iterator<Map.Entry<Pattern, String>> iterator = patternToSlipUri.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Pattern, String> next = iterator.next();
            if (next.getKey().matcher(name).find()) {
                headers.put("slipUri", next.getValue());
                break;
            }
        }
    }

    public void createPatternToSlipUri() {
        patternToSlipUri = new LinkedHashMap<>();
        BaitaikunSettingsExcelSource source = factory.getBean(BaitaikunSettingsExcelSource.class);
        Map<String, String> get = source.getSettings().get("各ファイル名");
        Iterator<Map.Entry<String, String>> iterator = get.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> next = iterator.next();
            switch (next.getKey()) {
                case "媒体カレンダーのファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.baitai");
                    break;
                case "納期案内一覧のファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.nouki");
                    break;
                case "カタログ（最新）のファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.newcatalog");
                    break;
                case "カタログ（旧）のファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.oldcatalog");
                    break;
                case "アイテムキー置換リストのファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.itemKeyReplace");
                    break;
                case "検索画面表示設定のファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.browserSetting");
                    break;
                case "レコード追加リストのファイル名":
                    patternToSlipUri.put(Pattern.compile(next.getValue()), "direct:excel.recordAppender");
                    break;
            }
        }
    }
}
