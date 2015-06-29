package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunBrowserSettingExcelSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Headers;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JettyRoute extends RouteBuilder {

    @Autowired
    CreateJsonComputableSource createJsonComputableSource;
    @Autowired
    BaitaikunBrowserSettingExcelSource baitaikunBrowserSettingExcelSource;
    private final String port;
    private final String templateFileName;
    String templateHtml;
    @Getter
    String completeHtml;

    public JettyRoute() throws IOException {
        this.port = Settings.get("媒体くん検索画面のポート番号");
        this.templateFileName = Settings.get("媒体くん画面のテンプレート名");
    }

    @Override
    public void configure() throws Exception {
        fromF("jetty:http://0.0.0.0:%s/query/", port)
                .choice().when(header("method").isEqualTo("init")).to("direct:waitJson")
                .otherwise().bean(this, "getTime()");

        fromF("file:%s?noop=true&delay=5000&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&include=%s&recursive=true", Settings.get("媒体くん用フォルダの場所"), templateFileName)
                .bean(this, "saveHtml").to("direct:waitSetting");

        from("direct:waitSetting").choice().when().method(this, "settingIsReady()")
                .bean(this, "createHtml").toF("file:%s/../", Settings.get("媒体くん用フォルダの場所"))
                //.bean(this, "createHtml").to("file:./")
                .to("direct:broker.poll")
                .otherwise().delay(3000).to("direct:waitSetting");

        fromF("jetty:http://0.0.0.0:%s/", port)
                .choice().when().method(this, "settingIsReady()")
                .bean(this, "getCompleteHtml()")
                .otherwise().setBody().simple("now loading...");

        from("direct:waitJson").choice().when().method(this, "jsonIsReady()")
                .bean(this, "getJson()")
                .otherwise().delay(3000).to("direct:waitJson");

    }

    public String getJson() {
        return String.format("JSON_CALLBACK(%s);", createJsonComputableSource.getJson());
    }

    public boolean jsonIsReady() {
        return !createJsonComputableSource.getJson().equals("{}");
    }

    public String getTime() {
        return String.format("JSON_CALLBACK({method:\"timer\",time:%s});", createJsonComputableSource.getTime());
    }

    public boolean settingIsReady() {
        return baitaikunBrowserSettingExcelSource.isReady();
    }

    public void saveHtml(@Body String body) {
        templateHtml = body;
    }

    public String createHtml(@Headers Map header) throws UnknownHostException {
        String body = templateHtml;
        String[] argsSetting = Stream.of(baitaikunBrowserSettingExcelSource.getArgsSetting())
                .skip(1)
                .map(arg -> {
                    switch (arg) {
                        case "自動取得:IPアドレス":
                            try {
                                return InetAddress.getLocalHost().getHostAddress();
                            } catch (UnknownHostException ex) {
                                return "255.255.255.255";
                            }
                        case "自動取得:ポート番号":
                            return port;
                        case "自動取得:スタイル":
                            return baitaikunBrowserSettingExcelSource.getCss();
                        default:
                            return arg;
                    }
                }).toArray(size -> new String[size]);
        Pattern p = Pattern.compile("(<< ?引数)(\\d+)( ?>>)");
        Matcher m;
        while ((m = p.matcher(body)).find()) {
            int argNum = Integer.parseInt(m.group(2));
            body = m.replaceFirst(argsSetting[argNum - 1]);
        }
        header.put(Exchange.FILE_NAME, "媒体くんX.html");
        completeHtml = body;
        System.out.println("[MESSAGE] 媒体くんXのページを更新しました URL: http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "/");
        return body;
    }
}
