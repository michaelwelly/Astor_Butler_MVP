package museon_online.astor_butler.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

@Component
@Slf4j
public class KafkaTopicInitializer {

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String userEventsTopic;

    @Value("${astor.kafka.user-events-partitions:3}")
    private int partitions;

    @Value("${astor.kafka.replication-factor:1}")
    private short replicationFactor;

    @PostConstruct
    public void ensureTopics() {
        if (!enabled) {
            return;
        }

        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");

        try (AdminClient adminClient = AdminClient.create(properties)) {
            NewTopic topic = new NewTopic(userEventsTopic, partitions, replicationFactor);
            adminClient.createTopics(List.of(topic)).all().get();
            log.info("Kafka topic created: topic={}, partitions={}", userEventsTopic, partitions);
        } catch (Exception e) {
            log.info("Kafka topic is already available or cannot be created yet: topic={}, reason={}", userEventsTopic, e.getMessage());
        }
    }
}
