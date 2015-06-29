package com.mycode.baitaikun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class Settings {

    private static final Properties properties = new Properties();

    public static String get(String key) throws FileNotFoundException, IOException {
        if (properties.isEmpty()) {
            init();
        }
        return properties.getProperty(key);
    }

    private static void init() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        InputStream inputStream = new FileInputStream(new File("program\\媒体くん基本設定ファイル（削除不可）.txt"));
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        properties.load(reader);
    }
}
