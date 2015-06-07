package com.mycode.baitaikun;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class Utility {

    public List<Map<String, String>> createMapList(List<String[]> stringArrayList, int skip) {
        Iterator<String[]> iterator = stringArrayList.iterator();
        List<Map<String, String>> mapList = new ArrayList<>();
        for (int i = 0; i < skip; i++) {
            iterator.next();
        }
        String[] header = iterator.next();
        while (iterator.hasNext()) {
            String[] values = iterator.next();
            Map<String, String> map = new LinkedHashMap<>();
            int valueLengthMax = 0;
            for (int i = 0; i < header.length && i < values.length; i++) {
                map.put(header[i], values[i]);
                valueLengthMax = Math.max(values[i].length(), valueLengthMax);
            }
            if (valueLengthMax != 0) {
                mapList.add(map);
            }
        }
        System.out.println(mapList);
        return mapList;
    }

    public boolean hasNull(String[] array) {
        for (String s : array) {
            if (s == null) {
                return true;
            }
        }
        return false;
    }

    public boolean isNotEmpty(String[] array) {
        for (String s : array) {
            if (s != null && !s.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
