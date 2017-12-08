package edu.berkeley.grails.hazelcast.flash;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class HazelcastClientFactory {
    private static final Log log = LogFactory.getLog(HazelcastClientFactory.class);

    private String configFileLocation;

    @SuppressWarnings("WeakerAccess")
    public String getConfigFileLocation() {
        return configFileLocation;
    }

    public void setConfigFileLocation(String configFileLocation) {
        this.configFileLocation = configFileLocation;
    }

    public HazelcastInstance newHazelcastClientInstance() throws IOException {
        if (getConfigFileLocation() != null) {
            log.info("Using client config file from " + getConfigFileLocation());
            return newHazelcastClientInstance(getConfigFileLocation());
        } else {
            return HazelcastClient.newHazelcastClient();
        }
    }

    @SuppressWarnings("WeakerAccess")
    public HazelcastInstance newHazelcastClientInstance(String configFileLocation) throws IOException {
        ClientConfig config = new XmlClientConfigBuilder(configFileLocation).build();
        return HazelcastClient.newHazelcastClient(config);
    }
}
