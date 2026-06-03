package museon_online.astor_butler.analytics;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.telegram.adapter.TelegramAdminNotifier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsKafkaConsumer {

    private static final String CONSUMER_NAME = "analytics-admin-chat";

    private final AnalyticsProcessedEventRepository processedEventRepository;
    private final TelegramAdminNotifier telegramAdminNotifier;

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String topic;

    @Value("${astor.kafka.analytics-group-id:astor-analytics-admin}")
    private String groupId;

    @Value("${astor.kafka.analytics-admin-chat-enabled:true}")
    private boolean adminChatEnabled;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "astor-analytics-kafka-consumer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;
    private volatile KafkaConsumer<String, String> consumer;

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.info("Kafka analytics consumer disabled");
            return;
        }

        running = true;
        executor.submit(this::consumeLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        KafkaConsumer<String, String> currentConsumer = consumer;
        if (currentConsumer != null) {
            currentConsumer.wakeup();
        }
        executor.shutdownNow();
    }

    private void consumeLoop() {
        while (running) {
            try (KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(consumerProperties())) {
                consumer = kafkaConsumer;
                kafkaConsumer.subscribe(List.of(topic));
                log.info("Analytics Kafka consumer subscribed: topic={}, groupId={}", topic, groupId);

                while (running) {
                    for (ConsumerRecord<String, String> record : kafkaConsumer.poll(Duration.ofSeconds(1))) {
                        process(record);
                    }
                    kafkaConsumer.commitSync();
                }
            } catch (WakeupException e) {
                if (running) {
                    log.warn("Analytics Kafka consumer wakeup while running", e);
                }
            } catch (Exception e) {
                log.warn("Analytics Kafka consumer failed, retrying in 3s: {}", e.getMessage());
                sleepBeforeRetry();
            } finally {
                consumer = null;
            }
        }
    }

    private void process(ConsumerRecord<String, String> record) {
        String eventId = record.key() == null || record.key().isBlank()
                ? topic + ":" + record.partition() + ":" + record.offset()
                : record.key();

        if (!processedEventRepository.markProcessed(CONSUMER_NAME, eventId)) {
            log.debug("Analytics Kafka event already processed: eventId={}", eventId);
            return;
        }

        String text = """
                Astor Butler user event
                topic=%s partition=%d offset=%d
                key=%s

                %s
                """.formatted(
                record.topic(),
                record.partition(),
                record.offset(),
                eventId,
                truncate(record.value())
        );

        if (adminChatEnabled) {
            telegramAdminNotifier.sendAnalytics(text);
        } else {
            log.info("Analytics admin chat disabled. Kafka event consumed: key={}", eventId);
        }
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return properties;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 3200 ? value : value.substring(0, 3200) + "\n...";
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
