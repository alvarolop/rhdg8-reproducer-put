package com.alopezme.hotrodtester;

import com.alopezme.hotrodtester.model.Book;
import com.alopezme.hotrodtester.utils.BookSchema;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;

@Configuration
public class Reproducer {

    @Value("${alvaro.queries.host}")
    private String host;
    @Value("${alvaro.queries.port}")
    private int port;
    @Value("${alvaro.queries.cache-name}")
    private String cacheName;

    private RemoteCacheManager remoteCacheManager;

    Logger logger = LoggerFactory.getLogger(Reproducer.class);

//    @EventListener(ApplicationReadyEvent.class)
    @PostConstruct
    public void tester() {

        String xml = String.format(
                "<infinispan>" +
                        "   <cache-container>" +
                        "       <distributed-cache name=\"%s\" mode=\"SYNC\" owners=\"1\" statistics=\"true\">" +
                        "           <encoding>" +
                        "               <key media-type=\"application/x-protostream\"/>" +
                        "               <value media-type=\"application/x-protostream\"/>" +
                        "           </encoding>" +
//                    "           <transaction mode=\"NONE\"/>" +
//                    "           <expiration lifespan=\"-1\" max-idle=\"-1\" interval=\"60000\"/>" +
//                    "           <memory storage=\"HEAP\"/>" +
//                    "           <indexing enabled=\"true\">" +
//                    "               <key-transformers/>" +
//                    "               <indexed-entities/>" +
//                    "           </indexing>" +
//                    "           <state-transfer enabled=\"false\" await-initial-transfer=\"false\"/>" +
//                    "           <partition-handling when-split=\"ALLOW_READ_WRITES\" merge-policy=\"REMOVE_ALL\"/>" +
                        "       </distributed-cache>" +
                        "   </cache-container>" +
                        "</infinispan>", cacheName);
        logger.info("Hello world, I have just started up");

        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .statistics()
                    .enable()
                .addServer()
                    .host(host)
                    .port(port)
                .security()
                    .authentication()
                        .saslMechanism("DIGEST-MD5")
                        .username("developer")
                        .password("developer")
                .marshaller(new ProtoStreamMarshaller())
                .addContextInitializers(new BookSchemaImpl());

        this.remoteCacheManager = new RemoteCacheManager(configuration.build());
        logger.info("Available caches: " + remoteCacheManager.getCacheNames());

        RemoteCache<Integer, Book> cache = remoteCacheManager.administration()
                .getOrCreateCache(cacheName, new XMLStringConfiguration(xml));

        logger.info("\n--> Test begins <--\n");
        logger.info("Content of entry #100: " + cache.get(100) + "\n");
        logger.info("Put entry #100");
        cache.put (100, new Book(100, "Alvaro", "Lopez", 1993));
        logger.info("Content of entry #100: " + cache.get(100) + "\n");

    }
}
