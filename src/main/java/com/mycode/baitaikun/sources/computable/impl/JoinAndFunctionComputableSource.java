package com.mycode.baitaikun.sources.computable.impl;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.excel.impl.BaitaikunSettingsExcelSource;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JoinAndFunctionComputableSource extends ComputableSource {

    @Getter
    final List<Map<String, String>> mapList = new ArrayList<>();
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
        mapList.clear();
        Map<String, Map<String, String>> itemKeyToMap = itemKeyToMapComputableSource.getItemKeyToMap();
        baitai.getMapList().stream()
                .map((map) -> {
                    Map<String, String> record = new LinkedHashMap<>();
                    String itemKey = map.get("ITEM_KEY");
                    map.entrySet().stream()
                    .forEach((entry) -> {
                        record.put("媒体一覧." + entry.getKey(), entry.getValue());
                        record.put("演算.ITEM_KEY", itemKey);
                    });
                    if (itemKeyToMap.containsKey(itemKey)) {
                        record.putAll(itemKeyToMap.get(itemKey));
                    }
                    return record;
                })
                .forEach(mapList::add);

        TaxInPrice taxInPrice = new TaxInPrice();
        Furikomi furikomi = new Furikomi();
        Bunkatsu bunkatsu = new Bunkatsu();
        mapList.stream()
                .forEach(map -> {
                    taxInPrice.setTaxInPrice(map);
                    furikomi.setFurikomi(map);
                    bunkatsu.setBunkatsu(map);
                });
        return null;
    }

    private class TaxInPrice {

        final double taxRate;
        final String[] taxOutFields;
        final Pattern noDigit = Pattern.compile("[^\\d]");

        public TaxInPrice() {
            Map<String, Map<String, String>> setting = settings.getSettings();
            taxRate = Double.parseDouble(setting.get("共通設定").get("消費税率"));
            taxOutFields = Stream.of("媒体一覧", "納期案内", "カタログ（最新）", "カタログ（旧）")
                    .map((sheetName)
                            -> sheetName + "." + settings.getSettings().get(sheetName).get("税抜価格の列名"))
                    .toArray(size
                            -> new String[size]);
        }

        public void setTaxInPrice(Map<String, String> map) {
            Optional<Integer> findFirst
                    = Stream.of(taxOutFields)
                    .filter((taxOutField)
                            -> map.containsKey(taxOutField))
                    .map((taxOutField)
                            -> noDigit.matcher(map.get(taxOutField)).replaceAll(""))
                    .filter((taxOutPrice)
                            -> !taxOutPrice.isEmpty())
                    .map((taxOutPrice) -> {
                        try {
                            int parseInt = Integer.parseInt(taxOutPrice);
                            return (int) (parseInt * (1 + taxRate));
                        } catch (NumberFormatException t) {
                            return -1;
                        }
                    })
                    .filter((taxOutPrice)
                            -> taxOutPrice > 0)
                    .findFirst();
            if (findFirst.isPresent()) {
                map.put("演算.税込価格", findFirst.get() + "");
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
            } else if (map.containsKey("媒体一覧.媒体コード")) { // ここ、決め打ちになってる　設定化しないと…
                String baitaiCode = map.get("媒体一覧.媒体コード");
                if (baitaiCode.length() > 2) {
                    baitaiCode = baitaiCode.substring(0, 3);
                    if (fukaBaitai.containsKey(baitaiCode)) {
                        fuka = fukaBaitai.get(baitaiCode);
                    }
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
            String bunkatsu = map.get(bunkatsuField);
            bunkatsu = bunkatsu == null ? "" : Normalizer.normalize(bunkatsu, Normalizer.Form.NFKC).replaceAll("[^\\d]", "");
            String bunkatsu2 = bunkatsuNum.get(map.get("媒体一覧.ITEM_KEY"));
            bunkatsu2 = bunkatsu2 == null ? "" : Normalizer.normalize(bunkatsu2, Normalizer.Form.NFKC).replaceAll("[^\\d]", "");
            String kakaku = map.get(priceField);
            kakaku = kakaku == null ? "" : Normalizer.normalize(kakaku, Normalizer.Form.NFKC).replaceAll("[^\\d]", "");

            if (!bunkatsu.isEmpty() && !bunkatsu.equals("0")) {
                map.put("演算.分割回数", "最大 " + bunkatsu + " 回可");
            } else if (!bunkatsu2.isEmpty() && !bunkatsu2.equals("0")) {
                map.put("演算.分割回数", "最大 " + bunkatsu2 + " 回可");
            } else if (bunkatsu.equals("0") || bunkatsu2.equals("0")) {
                map.put("演算.分割回数", "分割不可");
            } else if (!kakaku.isEmpty()) {
                if (Integer.parseInt(kakaku) > 30000) {
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
