package museon_online.astor_butler.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Component
@Slf4j
public class KafkaTopicInitializer {

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String userEventsTopic;

    @Value("${astor.kafka.outbox-user-events-topic:astor.outbox.user.events}")
    private String outboxUserEventsTopic;

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
            for (String topicName : new LinkedHashSet<>(List.of(userEventsTopic, outboxUserEventsTopic))) {
                ensureTopic(adminClient, topicName);
            }
        } catch (Exception e) {
            log.info("Kafka topics cannot be verified or created yet: reason={}", e.getMessage());
        }
    }

    private void ensureTopic(AdminClient adminClient, String topicName) throws Exception {
        Set<String> topicNames = adminClient.listTopics().names().get();
        if (!topicNames.contains(topicName)) {
            NewTopic topic = new NewTopic(topicName, partitions, replicationFactor);
            adminClient.createTopics(List.of(topic)).all().get();
            log.info("Kafka topic created: topic={}, partitions={}", topicName, partitions);
            return;
        }

        int currentPartitions = adminClient.describeTopics(List.of(topicName))
                .allTopicNames()
                .get()
                    .get(topicName)
                .partitions()
                .size();
        if (currentPartitions < partitions) {
            adminClient.createPartitions(Map.of(
                    topicName,
                    NewPartitions.increaseTo(partitions)
            )).all().get();
            log.info(
                    "Kafka topic partitions increased: topic={}, from={}, to={}",
                    topicName,
                    currentPartitions,
                    partitions
            );
            return;
        }

        log.info(
                "Kafka topic ready: topic={}, partitions={}, configuredPartitions={}",
                topicName,
                currentPartitions,
                partitions
        );
    }
}
