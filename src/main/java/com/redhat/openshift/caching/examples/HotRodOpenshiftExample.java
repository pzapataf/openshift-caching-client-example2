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

    private static String getEnv(String var) {
        String value = System.getenv(var);
        System.out.println(" -> " + var + " =" + value);
        return value;
    }

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {

        System.out.println("======================================================");
        System.out.println("Supplied environment variables:\n\n");
        System.getenv().forEach((k,v)-> System.out.println(k + "=" + v));
        System.out.println("======================================================");
        System.out.println("Required variables:\n");
        String HR_SERVICE_ENDPOINT = getEnv("HOT_ROD_SERVICE_ENDPOINT");
        String HR_SERVICE_USER     = getEnv("HOT_ROD_SERVICE_USER");
        String HR_SERVICE_PASSWORD = getEnv("HOT_ROD_SERVICE_PASSWORD");
        String HR_SERVICE_TRUST_STORE_PATH = getEnv("HR_SERVICE_TRUST_STORE_PATH");
        String HR_SERVICE_TRUST_STORE_PASSWORD = getEnv("HR_SERVICE_TRUST_STORE_PASSWORD");
        int HR_SERVER_PORT=11222;

        System.out.println("======================================================");
        if( HR_SERVICE_ENDPOINT == null || HR_SERVICE_USER == null || HR_SERVICE_PASSWORD == null || HR_SERVICE_TRUST_STORE_PATH == null ||  HR_SERVICE_TRUST_STORE_PASSWORD == null) {
            System.out.println("Missing parameters!");
            return;
        }

        // We will use system variables
        File trustStoreFile = new File(HR_SERVICE_TRUST_STORE_PATH);

        if (!trustStoreFile.exists() || !trustStoreFile.canRead()) {
            System.out.println("Trust store at path " + trustStoreFile.getPath() + " must exist" );
            return;
        }

        ConfigurationBuilder builder = new ConfigurationBuilder();

        // Hot Rod client setup
        builder
            .addServer().host(HR_SERVICE_ENDPOINT).port(HR_SERVER_PORT)
            .clientIntelligence(ClientIntelligence.BASIC)
            .security()
                .authentication()
                    .username(HR_SERVICE_USER)
                    .password(HR_SERVICE_PASSWORD)
                    .realm("ApplicationRealm")
                    .saslMechanism("DIGEST-MD5")
                    .saslQop(SaslQop.AUTH)
                    .serverName("caching-service")
                    .enable()
                .ssl()
                    .enable()
                    .sniHostName(HR_SERVICE_ENDPOINT)
                    .trustStoreFileName(HR_SERVICE_TRUST_STORE_PATH)
                    .trustStorePassword(HR_SERVICE_TRUST_STORE_PASSWORD.toCharArray());

        // Connect to the server
        System.out.println("Connecting to " + HR_SERVICE_ENDPOINT + ":" + HR_SERVER_PORT);

        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build());

        // Obtain the remote cache
        RemoteCache<String, String> cache = cacheManager.getCache();

        // Start using Hot Rod
        try {
            System.out.println("Kill pod to stop");
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
