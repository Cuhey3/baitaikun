package com.mycode.baitaikun.sources;

import static java.lang.String.format;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class Source extends RouteBuilder {

    @Autowired
    @Getter
    public BeanFactory factory;
    @Setter
    protected String sourceKind;
    @Getter
    @Setter
    protected long updateTime = 0L, checkForUpdateTime = 0L;
    @Setter
    protected boolean ready = false;
    protected String initEndpoint, initImplEndpoint;

    public void buildEndpoint() throws Exception {
        initEndpoint = format("timer:%s.init?repeatCount=1", sourceKind);
        initImplEndpoint = format("direct:%s.initImpl", sourceKind);
    }

    public abstract boolean isUpToDate();

    public void updated() {
        System.out.println("[UPDATE] " + this.getClass().getSimpleName());
        updateTime = System.currentTimeMillis();
        checkForUpdateTime = System.currentTimeMillis();
        ready = true;
    }

    public void checkedForUpdate() {
        checkForUpdateTime = System.currentTimeMillis();
    }

    public void ready() {
        ready = true;
    }

    public boolean isReady() {
        return ready;
    }
}
