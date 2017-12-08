package edu.berkeley.grails.hazelcast.flash.test;

import com.hazelcast.session.P2PLifecycleListener;
import com.hazelcast.session.HazelcastSessionManager;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleException;
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
        System.out.println("!!! running customizer");

        P2PLifecycleListener p2PLifecycleListener = new P2PLifecycleListener();
        //p2PLifecycleListener.setConfigLocation("classpath:hazelcast.xml");
        container.addContextLifecycleListeners(p2PLifecycleListener);

        final Map<String,Context> contextHolder = new HashMap<>();


        container.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                System.out.println("Got context");
                contextHolder.put("context", context);
            }
        });

        final HazelcastSessionManager hazelcastSessionManager = new HazelcastSessionManager();
        hazelcastSessionManager.setSticky(false);
        hazelcastSessionManager.setDeferredWrite(false);

        container.addContextLifecycleListeners(new LifecycleListener() {
            @Override
            public void lifecycleEvent(LifecycleEvent event) {
                /**
                 * type: before_init, after_init, before_start, configure_start, start, after_start
                 */
                System.out.println("LIFECYCLE EVENT: type=" + event.getType() + ", source super class=" + event.getSource().getClass().getSuperclass().getName());
                for(Class interf : event.getSource().getClass().getInterfaces()) {
                    System.out.println("  INTERFACE " + interf.getName());
                }

                if(event.getType().equals("start")) {
                    Context context = contextHolder.get("context");
                    //try {
                        hazelcastSessionManager.setContext(context);
                        //hazelcastSessionManager.init();
                        //hazelcastSessionManager.start();
                    //}
                    //catch(LifecycleException e) {
                    //    throw new RuntimeException(e);
                    //}
                    context.setManager(hazelcastSessionManager);

                    System.out.println("Added hazelcastSessionManager");

                }
            }
        });



        /*
        container.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                System.out.println("STATE = " + context.getStateName() + ", baseName=" + context.getBaseName() + ", displayName=" + context.getDisplayName() + ", publicId=" + context.getPublicId() + ", name=" + context.getName() + ", parent=" + context.getParent());
                HazelcastSessionManager hazelcastSessionManager = new HazelcastSessionManager();
                try {
                    hazelcastSessionManager.setContext(context);
                    //hazelcastSessionManager.init();
                    hazelcastSessionManager.start();
                }
                catch(LifecycleException e) {
                    throw new RuntimeException(e);
                }
                context.setManager(hazelcastSessionManager);
            }
        });
        */
    }
}
