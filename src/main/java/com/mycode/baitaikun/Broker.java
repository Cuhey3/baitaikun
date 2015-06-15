package com.mycode.baitaikun;

import com.mycode.baitaikun.sources.computable.ComputableSource;
import com.mycode.baitaikun.sources.Source;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;

@Component
public class Broker extends RouteBuilder {

    @Autowired
    DefaultListableBeanFactory factory;
    private final String timerEndpoint = "timer:broker.poll?period=1s&delay=3s";
    private final String initEndpoint = "timer:broker.init?repeatCount=1";
    private final Set<ComputableSource> computableSources = new HashSet<>();

    @Override
    public void configure() throws Exception {
        from(initEndpoint)
                .bean(this, "setComputableSources()");

        from(timerEndpoint)
                .bean(this, "getShoudUpdateOneSource()")
                .filter().simple("${body} != null")
                .routingSlip().simple("body.computeEndpoint");
    }

    public void setComputableSources() {
        String[] beanNames = factory.getBeanNamesForType(ComputableSource.class);
        for (String beanName : beanNames) {
            computableSources.add((ComputableSource) factory.getBean(beanName));
        }
    }

    public ComputableSource getShoudUpdateOneSource() {
        Iterator<ComputableSource> itr = computableSources.iterator();
        while (itr.hasNext()) {
            ComputableSource one = itr.next();
            if (!one.isUpToDate() && one.isReady()) {
                boolean superiorSourcesReady = true;
                Iterator<Source> iterator = one.getSuperiorSources().iterator();
                while (superiorSourcesReady && iterator.hasNext()) {
                    Source next = iterator.next();
                    superiorSourcesReady = next.isUpToDate() && next.isReady();
                }
                if (superiorSourcesReady) {
                    return one;
                }
            }
        }
        return null;
    }
}
