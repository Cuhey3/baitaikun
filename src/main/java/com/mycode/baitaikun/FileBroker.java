package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
    @Autowired
    BeanFactory factory;

    @Override
    public void configure() throws Exception {
        fromF("file:%s?noop=true&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&include=%s&recursive=true", Settings.get("媒体くん用フォルダの場所"), Settings.get("媒体くん詳細設定のファイル名"))
                .routeId("settingRoute").autoStartup(false)
                .to("direct:excel.settings?block=true")
                .to("direct:broker.poll");

        fromF("file:%s?noop=true&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&exclude=%s&recursive=true", Settings.get("媒体くん用フォルダの場所"), Settings.get("媒体くん詳細設定のファイル名"))
                .routeId("fileBrokerRoute").autoStartup(false)
                .bean(this, "checkFileName")
                .routingSlip().simple("header.slipUri")
                .setBody().constant(null)
                .to("direct:broker.poll");
    }

    public void checkFileName(@Headers Map headers) {
        String name = (String) headers.get(Exchange.FILE_NAME_ONLY);
        Optional<String> slipUri
                = patternToSlipUri.entrySet().stream()
                .filter((entry)
                        -> entry.getKey().matcher(name).find())
                .map((entry)
                        -> entry.getValue())
                .findFirst();
        if (slipUri.isPresent() && !name.startsWith("~$")) {
            headers.put("slipUri", slipUri.get());
            if(factory.getBean(CreateJsonComputableSource.class).applicationIsReady){
                System.out.println("[MESSAGE] ファイルの変更を検出しました。 " + name);
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
