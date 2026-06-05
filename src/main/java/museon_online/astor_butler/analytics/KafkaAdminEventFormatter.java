package museon_online.astor_butler.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaAdminEventFormatter {

    private static final int MAX_RAW_PAYLOAD_LENGTH = 1600;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public String format(ConsumerRecord<String, ?> record, String eventId) {
        Map<String, Object> event = parse(record.value());
        if (event.isEmpty()) {
            return technicalFallback(record, eventId);
        }

        String eventType = text(event.get("eventType"));
        return switch (eventType) {
            case "USER_MESSAGE_RECEIVED" -> userMessage(record, eventId, event);
            case "LLM_RESPONSE_GENERATED" -> llmResponse(record, eventId, event);
            default -> generic(record, eventId, event);
        };
    }

    private String userMessage(ConsumerRecord<String, ?> record, String eventId, Map<String, Object> event) {
        String displayName = displayName(event);
        String previousState = text(event.get("previousState"));
        String nextState = text(event.get("nextState"));
        String stateTransition = stateTransition(previousState, nextState);
        String text = text(event.get("text"));
        String transcript = text(event.get("transcript"));
        String mediaKind = text(event.get("mediaKind"));
        String transcriptionStatus = text(event.get("transcriptionStatus"));
        String storageObjectKey = text(event.get("storageObjectKey"));
        boolean contactPhonePresent = bool(event.get("contactPhonePresent"));
        boolean fallback = bool(event.get("fallback"));

        return """
                <b>Astor Butler / user message</b>
                %s

                <b>%s</b>
                %s

                <b>Сообщение</b>
                %s
                %s

                <b>Контекст</b>
                Type: %s
                State: %s
                Media: %s
                Transcription: %s
                Contact: %s
                Actions: %s

                <b>Техника</b>
                Kafka: %s[%d] offset=%d
                Key: %s
                Event: %s
                """.formatted(
                fallback ? "Нужна проверка администратора" : "Событие обработано штатно",
                html(displayName),
                userMeta(event),
                quote(text),
                mediaDetails(transcript, storageObjectKey),
                html(text(event.get("eventType"))),
                html(stateTransition),
                html(blankAsEmptyLabel(mediaKind)),
                html(blankAsEmptyLabel(transcriptionStatus)),
                contactPhonePresent ? "shared" : "not shared",
                html(actions(event.get("actions"))),
                html(record.topic()),
                record.partition(),
                record.offset(),
                html(text(record.key())),
                html(eventId)
        );
    }

    private String mediaDetails(String transcript, String storageObjectKey) {
        if (transcript.isBlank() && storageObjectKey.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        if (!transcript.isBlank()) {
            builder.append("\n<b>Расшифровка</b>\n")
                    .append(quote(transcript));
        }
        if (!storageObjectKey.isBlank()) {
            builder.append("\n<b>Voice object</b>\n")
                    .append("<code>")
                    .append(html(storageObjectKey))
                    .append("</code>");
        }
        return builder.toString();
    }

    private String llmResponse(ConsumerRecord<String, ?> record, String eventId, Map<String, Object> event) {
        return """
                <b>Astor Butler / AI response</b>
                %s

                <b>%s</b>
                %s

                <b>Вход</b>
                %s

                <b>Ответ AI</b>
                %s

                <b>Контекст</b>
                Type: %s
                State: %s
                Stage: %s
                Fallback: %s

                <b>Техника</b>
                Kafka: %s[%d] offset=%d
                Key: %s
                Event: %s
                """.formatted(
                bool(event.get("fallbackUsed")) ? "AI fallback был использован" : "AI ответил штатно",
                html(displayName(event)),
                userMeta(event),
                quote(text(event.get("inputText"))),
                quote(text(event.get("responseText"))),
                html(text(event.get("eventType"))),
                html(blankAsEmptyLabel(text(event.get("state")))),
                html(blankAsEmptyLabel(text(event.get("stage")))),
                bool(event.get("fallbackUsed")) ? "yes" : "no",
                html(record.topic()),
                record.partition(),
                record.offset(),
                html(text(record.key())),
                html(eventId)
        );
    }

    private String generic(ConsumerRecord<String, ?> record, String eventId, Map<String, Object> event) {
        return """
                <b>Astor Butler / event</b>
                %s

                <b>%s</b>
                %s

                <b>Техника</b>
                Kafka: %s[%d] offset=%d
                Key: %s
                Event: %s

                <pre>%s</pre>
                """.formatted(
                html(text(event.get("eventType"))),
                html(displayName(event)),
                userMeta(event),
                html(record.topic()),
                record.partition(),
                record.offset(),
                html(text(record.key())),
                html(eventId),
                html(truncate(raw(event)))
        );
    }

    private String technicalFallback(ConsumerRecord<String, ?> record, String eventId) {
        return """
                <b>Astor Butler / event</b>
                UNPARSEABLE_KAFKA_PAYLOAD

                Kafka: %s[%d] offset=%d
                Key: %s
                Event: %s

                <pre>%s</pre>
                """.formatted(
                html(record.topic()),
                record.partition(),
                record.offset(),
                html(text(record.key())),
                html(eventId),
                html(truncate(String.valueOf(record.value())))
        );
    }

    private Map<String, Object> parse(Object value) {
        if (value instanceof GenericRecord record) {
            return avroRecord(record);
        }
        if (!(value instanceof String text) || text.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(text, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> avroRecord(GenericRecord record) {
        Map<String, Object> event = new LinkedHashMap<>();
        for (Schema.Field field : record.getSchema().getFields()) {
            Object value = record.get(field.name());
            if (value instanceof GenericRecord nested && "actor".equals(field.name())) {
                putNested(event, nested);
            } else if (value instanceof GenericRecord nested && "payload".equals(field.name())) {
                putNested(event, nested);
            } else {
                event.put(field.name(), value);
            }
        }
        return event;
    }

    private void putNested(Map<String, Object> target, GenericRecord record) {
        for (Schema.Field field : record.getSchema().getFields()) {
            target.put(field.name(), record.get(field.name()));
        }
    }

    private String displayName(Map<String, Object> event) {
        String firstName = text(event.get("firstName"));
        String lastName = text(event.get("lastName"));
        String username = text(event.get("username"));
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank() && !username.isBlank()) {
            return fullName + " (@" + username + ")";
        }
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private String userMeta(Map<String, Object> event) {
        String chatId = text(event.get("chatId"));
        String telegramUserId = text(event.get("telegramUserId"));
        String username = text(event.get("username"));
        return "chat " + html(blankAsEmptyLabel(chatId))
                + " / user " + html(blankAsEmptyLabel(telegramUserId))
                + (username.isBlank() ? "" : " / @" + html(username));
    }

    private String stateTransition(String previousState, String nextState) {
        if (previousState.isBlank() && nextState.isBlank()) {
            return "(unknown)";
        }
        if (previousState.isBlank()) {
            return nextState;
        }
        if (nextState.isBlank()) {
            return previousState;
        }
        return previousState + " -> " + nextState;
    }

    private String actions(Object value) {
        if (value instanceof List<?> items) {
            return items.isEmpty() ? "[]" : String.join(", ", items.stream().map(this::text).toList());
        }
        return text(value);
    }

    private String raw(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            return event.toString();
        }
    }

    private String blankAsEmptyLabel(String value) {
        return value.isBlank() ? "(empty)" : value;
    }

    private boolean bool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String quote(String value) {
        String normalized = blankAsEmptyLabel(value == null ? "" : value.trim());
        return "<blockquote>" + html(truncate(normalized)) + "</blockquote>";
    }

    private String html(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_RAW_PAYLOAD_LENGTH ? value : value.substring(0, MAX_RAW_PAYLOAD_LENGTH) + "\n...";
    }
}
