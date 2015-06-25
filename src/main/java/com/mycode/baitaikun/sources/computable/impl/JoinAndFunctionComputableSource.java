package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JoinAndFunctionComputableSource extends ComputableSource {

    @Getter
    List<Map<String, String>> mapList;
    @Autowired
    BaitaikunSettingsExcelSource settings;
    @Autowired
    BaitaiComputableSource baitai;
    @Autowired
    ItemKeyToMapComputableSource itemKeyToMapComputableSource;

    public JoinAndFunctionComputableSource() throws Exception {
        setSourceKind("computable.join");
        getSuperiorSourceClasses().add(BaitaikunSettingsExcelSource.class);
        getSuperiorSourceClasses().add(BaitaiComputableSource.class);
        getSuperiorSourceClasses().add(ItemKeyToMapComputableSource.class);
        setCheckForUpdateTime(-1L);
        buildEndpoint();
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        from(computeImplEndpoint)
                .bean(this, "compute()")
                .bean(this, "updated()");

        from(initImplEndpoint)
                .to("mock:initImpl");
    }

    @Override
    public Object compute() {
        TaxInPrice taxInPrice = new TaxInPrice();
        Furikomi furikomi = new Furikomi();
        Bunkatsu bunkatsu = new Bunkatsu();
        mapList = new ArrayList<>();
        List<Map<String, String>> baitaiMapList = baitai.getMapList();
        Map<String, Map<String, String>> itemKeyToMap = itemKeyToMapComputableSource.getItemKeyToMap();
        baitaiMapList.stream().forEach((map) -> {
            Map<String, String> record = new LinkedHashMap<>();
            String itemKey = map.get("ITEM_KEY");
            map.entrySet().stream().forEach((entry) -> {
                record.put("媒体一覧." + entry.getKey(), entry.getValue());
                record.put("演算.ITEM_KEY", itemKey);
            });
            Map<String, String> itemInfo = itemKeyToMap.get(itemKey);
            if (itemInfo != null) {
                itemInfo.entrySet().stream().forEach((entry) -> {
                    record.put(entry.getKey(), entry.getValue());
                });
            }
            mapList.add(record);
        });
        mapList.stream().forEach(map -> {
            taxInPrice.setTaxInPrice(map);
            furikomi.setFurikomi(map);
            bunkatsu.setBunkatsu(map);
        });
        //mapList.stream().forEach(System.out::println);
        return null;
    }

    private class TaxInPrice {

        final double taxRate;
        final String[] taxOutFields;
        final Pattern noDigit = Pattern.compile("[^\\d]");

        public TaxInPrice() {
            Map<String, Map<String, String>> setting = settings.getSettings();
            taxRate = Double.parseDouble(setting.get("共通設定").get("消費税率"));
            taxOutFields = new String[4];
            String[] sheetNames = new String[]{"媒体一覧", "納期案内", "カタログ（最新）", "カタログ（旧）"};
            for (int i = 0; i < 4; i++) {
                taxOutFields[i] = sheetNames[i] + "." + settings.getSettings().get(sheetNames[i]).get("税抜価格の列名");
            }
        }

        public void setTaxInPrice(Map<String, String> map) {

            for (String taxOutField : taxOutFields) {
                String taxOutPrice = map.get(taxOutField);
                if (taxOutPrice != null) {
                    taxOutPrice = noDigit.matcher(taxOutPrice).replaceAll("");
                    if (!taxOutPrice.isEmpty()) {
                        try {
                            int parseInt = Integer.parseInt(taxOutPrice);
                            map.put("演算.税込価格", (int) (parseInt * (1 + taxRate)) + "");
                        } catch (Throwable t) {
                            System.out.println("価格を読み取れません…アイテムキー: " + map.get("演算.ITEM_KEY") + " 価格: " + map.get(taxOutField));
                            continue;
                        }
                        break;
                    }
                }
            }
        }
    }

    private class Furikomi {

        Map<String, String> fukaItem;
        Map<String, String> fukaBaitai;

        public Furikomi() {
            fukaItem = settings.getSettings().get("振込不可商品");
            fukaBaitai = settings.getSettings().get("振込不可媒体");
        }

        public void setFurikomi(Map<String, String> map) {
            String itemKey = map.get("媒体一覧.ITEM_KEY");
            String fuka = "";
            if (fukaItem.containsKey(itemKey)) {
                fuka = fukaItem.get(itemKey);
            }
            String baitaiCode = map.get("媒体一覧.媒体コード");
            if (baitaiCode != null && baitaiCode.length() > 2) {
                baitaiCode = baitaiCode.substring(0, 3);
                if (fuka.isEmpty() && fukaBaitai.containsKey(baitaiCode)) {
                    fuka = fukaBaitai.get(baitaiCode);
                }
            }
            if (fuka.isEmpty()) {
                fuka = "可";
            }
            map.put("演算.振込一括", fuka);
        }
    }

    private class Bunkatsu {

        Map<String, String> bunkatsuNum;
        String bunkatsuField;
        String priceField;
        Pattern p = Pattern.compile("^\\d+$");

        public Bunkatsu() {
            this.bunkatsuNum = settings.getSettings().get("分割回数");
            this.bunkatsuField = "納期案内." + settings.getSettings().get("納期案内").get("分割回数の列名");
            this.priceField = "納期案内." + settings.getSettings().get("納期案内").get("税抜価格の列名");
        }

        public void setBunkatsu(Map<String, String> map) {
            String itemKey = map.get("媒体一覧.ITEM_KEY");
            String bunkatsu = map.get(bunkatsuField);
            String kakaku = map.get(priceField);
            if (bunkatsu != null && p.matcher(bunkatsu).find() && !bunkatsu.equals("0")) {
                map.put("演算.分割回数", "最大 " + bunkatsu + " 回可");
            } else if (bunkatsuNum.containsKey(itemKey) && !bunkatsuNum.get(itemKey).equals("0") && !bunkatsuNum.get(itemKey).equals("-")) {
                map.put("演算.分割回数", "最大 " + bunkatsuNum.get(itemKey) + " 回可");
            } else if (kakaku != null && p.matcher(kakaku).find()) {
                if (Integer.parseInt(kakaku) < 30000) {
                    map.put("演算.分割回数", "分割回数要確認");
                } else {
                    map.put("演算.分割回数", "分割不可");
                }
            } else {
                map.put("演算.分割回数", "分割回数要確認");
            }
        }
    }
}
