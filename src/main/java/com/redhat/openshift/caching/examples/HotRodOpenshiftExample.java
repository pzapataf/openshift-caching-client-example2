package com.redhat.openshift.caching.examples;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Example of how to configure a Hot Rod client to access a JDG cluster in Openshift Online,
 */
public class HotRodOpenshiftExample {
    static int HR_SERVER_PORT = 443;
    static String USER = "test";
    static String PASSWORD = "test";
    static String TRUST_STORE_NAME = "caching-service-trust-store.jks";
    static String TRUST_PASSWORD = "secret";

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        // We will use system variables
        if (!new File(TRUST_STORE_NAME).exists()) {
            System.out.println("You need to generate a trust store with the service certificate first");
            return;
        }


        String HR_SERVER_ENDPOINT = args[0];

        ConfigurationBuilder builder = new ConfigurationBuilder();

        // Hot Rod client setup
        builder
            .addServer().host(HR_SERVER_ENDPOINT).port(HR_SERVER_PORT)
            .clientIntelligence(ClientIntelligence.BASIC)
            .security()
                .authentication()
                    .username(USER)
                    .password(PASSWORD)
                    .realm("ApplicationRealm")
                    .saslMechanism("DIGEST-MD5")
                    .saslQop(SaslQop.AUTH)
                    .serverName("caching-service")
                    .enable()
                .ssl()
                    .enable()
                    .sniHostName(HR_SERVER_ENDPOINT)
                    .trustStoreFileName(TRUST_STORE_NAME)
                    .trustStorePassword(TRUST_PASSWORD.toCharArray());

        // Connect to the server
        System.out.println("Connecting to " + HR_SERVER_ENDPOINT + ":" + HR_SERVER_PORT);

        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());

        // Obtain the remote cache
        RemoteCache<String, String> cache = cacheManager.getCache();

        // Start using Hot Rod
        try {
            System.out.println("[Ctrl+C] to stop");
            while (true) {
                cache.put("test", Instant.now().toString());
                System.out.println("Value from Cache: " + cache.get("test"));
                TimeUnit.SECONDS.sleep(2);
            }
        } finally {
            // Stop the cache manager and release all resources
            cacheManager.stop();
        }
    }
}
