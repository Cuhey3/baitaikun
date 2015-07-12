package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileBroker extends RouteBuilder {

    private final Map<Pattern, String> patternToSlipUri = new LinkedHashMap<>();
    private final String fileEndpointTemplate = "file:%s?noop=true&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&%s=%s&recursive=true";
    @Autowired
    BeanFactory factory;

    @Override
    public void configure() throws Exception {
        fromF(fileEndpointTemplate, Settings.get("媒体くん用フォルダの場所"), "include", Settings.get("媒体くん詳細設定のファイル名"))
                .routeId("settingRoute").autoStartup(false)
                .to("direct:excel.settings?block=true")
                .to("direct:broker.notate");

        fromF(fileEndpointTemplate, Settings.get("媒体くん用フォルダの場所"), "exclude", Settings.get("媒体くん詳細設定のファイル名"))
                .routeId("fileBrokerRoute").autoStartup(false)
                .bean(this, "fileNameToSlipUri")
                .routingSlip().simple("header.slipUri")
                .to("direct:broker.notate");
    }

    public void fileNameToSlipUri(@Headers Map headers) {
        String name = (String) headers.get(Exchange.FILE_NAME_ONLY);
        if (!name.startsWith("~$")) {
            String slipUri
                    = patternToSlipUri.entrySet().stream()
                    .filter((entry)
                            -> entry.getKey().matcher(name).find())
                    .map((entry)
                            -> entry.getValue())
                    .findFirst().orElse(null);
            if (slipUri != null) {
                headers.put("slipUri", slipUri);
                if (factory.getBean(CreateJsonComputableSource.class).applicationIsReady) {
                    System.out.println("[MESSAGE] ファイルの変更を検出しました。 " + name);
                }
            }
        }
    }

    public void createPatternToSlipUri() {
        Map<String, String> collect = Stream.of("媒体カレンダーのファイル名/direct:excel.baitai",
                "納期案内一覧のファイル名/direct:excel.nouki",
                "カタログ（最新）のファイル名/direct:excel.newcatalog",
                "カタログ（旧）のファイル名/direct:excel.oldcatalog",
                "アイテムキー置換リストのファイル名/direct:excel.itemKeyReplace",
                "検索画面表示設定のファイル名/direct:excel.browserSetting",
                "レコード追加リストのファイル名/direct:excel.recordAppender")
                .collect(Collectors.toMap(
                                s -> s.split("/")[0],
                                s -> s.split("/")[1]));

        patternToSlipUri.clear();
        patternToSlipUri.putAll(
                factory.getBean(BaitaikunSettingsExcelSource.class).getSettings().get("各ファイル名").entrySet().stream()
                .collect(Collectors.toMap(
                                (entry) -> Pattern.compile(entry.getValue()),
                                (entry) -> collect.get(entry.getKey()))));
    }
}
