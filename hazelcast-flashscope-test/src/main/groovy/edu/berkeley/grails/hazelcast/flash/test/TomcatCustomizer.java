package edu.berkeley.grails.hazelcast.flash.test;

import com.hazelcast.session.HazelcastSessionManager;
import com.hazelcast.session.P2PLifecycleListener;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.springframework.boot.autoconfigure.websocket.TomcatWebSocketContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TomcatCustomizer extends TomcatWebSocketContainerCustomizer {
    @Override
    public void doCustomize(TomcatEmbeddedServletContainerFactory container) {
        P2PLifecycleListener p2PLifecycleListener = new P2PLifecycleListener();
        //p2PLifecycleListener.setConfigLocation("classpath:hazelcast.xml");
        container.addContextLifecycleListeners(p2PLifecycleListener);

        final Map<String, Context> contextHolder = new HashMap<>();

        container.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                contextHolder.put("context", context);
            }
        });

        final HazelcastSessionManager hazelcastSessionManager = new HazelcastSessionManager();
        hazelcastSessionManager.setSticky(false);
        hazelcastSessionManager.setDeferredWrite(false);

        container.addContextLifecycleListeners(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                // type: before_init, after_init, before_start, configure_start, start, after_start
                if (event.getType().equals("start")) {
                    Context context = contextHolder.get("context");
                    hazelcastSessionManager.setContext(context);
                    context.setManager(hazelcastSessionManager);
                }
            }
        });
    }
}
