package com.mycode.baitaikun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("[MESSAGE] 媒体くんを起動しました。準備が終わるまでお待ちください。結構かかります。");
        new SpringApplication(App.class).run();
        Thread.sleep(Long.MAX_VALUE);
    }
}
