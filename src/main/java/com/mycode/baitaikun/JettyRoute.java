package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.impl.CreateJsonComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunBrowserSettingExcelSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    String port;
    String templateFileName;
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
                .otherwise().bean(this, "getTime");
        fromF("jetty:http://0.0.0.0:%s/", port).bean(this, "getCompleteHtml()");
        from("direct:waitJson").choice().when().method(this, "jsonIsReady()")
                .bean(this, "getJson()")
                .otherwise().delay(3000).to("direct:waitJson");
        fromF("file:%s?noop=true&delay=5000&idempotent=true&idempotentKey=${file:name}-${file:modified}&readLock=none&include=%s&recursive=true", Settings.get("媒体くん用フォルダの場所"), templateFileName).to("direct:waitSetting");
        from("direct:waitSetting").choice().when().method(this, "settingIsReady()")
                .bean(this, "createHtml").toF("file:%s/../",Settings.get("媒体くん用フォルダの場所"))
                //.bean(this, "createHtml").to("file:./")
                .otherwise().delay(3000).to("direct:waitSetting");
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

    public String createHtml(@Body String body, @Headers Map header) throws UnknownHostException {
        String[] argsSetting = baitaikunBrowserSettingExcelSource.getArgsSetting();
        for (int i = 0; i < argsSetting.length; i++) {
            switch (argsSetting[i]) {
                case "自動取得:IPアドレス":
                    argsSetting[i] = InetAddress.getLocalHost().getHostAddress();
                    break;
                case "自動取得:ポート番号":
                    argsSetting[i] = port;
                    break;
            }
        }
        Pattern p = Pattern.compile("(<< ?引数)(\\d+)( ?>>)");
        Matcher m;
        while ((m = p.matcher(body)).find()) {
            int argNum = Integer.parseInt(m.group(2));
            body = m.replaceFirst(argsSetting[argNum - 1]);
        }
        header.put(Exchange.FILE_NAME, "媒体くんX.html");
        completeHtml = body;
        System.out.println("[MESSAGE] 媒体くんのURLを登録しました: http://" + InetAddress.getLocalHost().getHostAddress() + ":" + port + "/");
        return body;
    }
}
