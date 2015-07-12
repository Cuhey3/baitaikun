package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class Broker extends RouteBuilder {

    @Autowired
    DefaultListableBeanFactory factory;
    private final String initEndpoint = "timer:broker.init?repeatCount=1";
    private final Set<ComputableSource> computableSources = new HashSet<>();

    @Override
    public void configure() throws Exception {
        from(initEndpoint)
                .bean(this, "setComputableSources()");

        from("direct:broker.notate").routeId("broker.notate").autoStartup(false)
                .bean(this, "getShoudUpdateOneSource")
                .filter().simple("${body} is 'com.mycode.baitaikun.sources.computable.ComputableSource'")
                .routingSlip().simple("body.computeEndpoint");
    }

    public void setComputableSources() throws Exception {
        computableSources.clear();
        Stream.of(factory.getBeanNamesForType(ComputableSource.class))
                .map((beanName)
                        -> (ComputableSource) factory.getBean(beanName))
                .forEach(computableSources::add);
        if (this.getContext().getRouteStatus("settingRoute").isStopped()) {
            this.getContext().startRoute("settingRoute");
        }
    }

    public ComputableSource getShoudUpdateOneSource() {
        ComputableSource result = computableSources.stream()
                .filter((source)
                        -> source.isReady() && !source.isUpToDate())
                .filter((source)
                        -> source.getSuperiorSources().stream()
                        .allMatch((superiorSource)
                                -> superiorSource.isUpToDate() && superiorSource.isReady()))
                .findFirst().orElse(null);
        if (result != null) {
            result.setReady(false);
        }
        return result;
    }
}
