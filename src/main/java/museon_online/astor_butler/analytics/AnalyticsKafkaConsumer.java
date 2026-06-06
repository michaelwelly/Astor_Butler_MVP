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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsKafkaConsumer {

    private static final String CONSUMER_NAME = "analytics-admin-chat";

    private final AnalyticsProcessedEventRepository processedEventRepository;
    private final TelegramAdminNotifier telegramAdminNotifier;
    private final KafkaAdminEventFormatter kafkaAdminEventFormatter;

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String topic;

    @Value("${astor.kafka.analytics-group-id:astor-admin-events}")
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
        CompletableFuture.runAsync(this::consumeLoop, executor)
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null && running) {
                        log.error("Analytics Kafka consumer thread stopped unexpectedly", throwable);
                    }
                });
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
                log.warn("Analytics Kafka consumer failed, retrying in 3s: {}", e.getMessage(), e);
                sleepBeforeRetry();
            } finally {
                consumer = null;
            }
        }
    }

    private void process(ConsumerRecord<String, String> record) {
        String eventId = extractEventId(record);

        if (processedEventRepository.existsByConsumerNameAndEventId(CONSUMER_NAME, eventId)) {
            log.debug("Analytics Kafka event already processed: eventId={}", eventId);
            return;
        }

        String text = kafkaAdminEventFormatter.format(record, eventId);

        if (adminChatEnabled) {
            boolean delivered = telegramAdminNotifier.sendAnalytics(text);
            if (!delivered) {
                throw new IllegalStateException("Telegram analytics notification was not delivered");
            }
            log.info("Analytics Kafka event delivered to admin chat: eventId={}, key={}", eventId, record.key());
        } else {
            log.info("Analytics admin chat disabled. Kafka event consumed: eventId={}, key={}", eventId, record.key());
        }

        processedEventRepository.markProcessed(CONSUMER_NAME, eventId);
    }

    private String extractEventId(ConsumerRecord<String, String> record) {
        return record.key() == null || record.key().isBlank()
                ? topic + ":" + record.partition() + ":" + record.offset()
                : record.key();
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return properties;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
