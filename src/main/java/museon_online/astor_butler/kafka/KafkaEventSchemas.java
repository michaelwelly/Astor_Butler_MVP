package museon_online.astor_butler.kafka;

import org.apache.avro.Schema;

public final class KafkaEventSchemas {

    private KafkaEventSchemas() {
    }

    public static final Schema USER_MESSAGE_RECEIVED = new Schema.Parser().parse("""
            {
              "type": "record",
              "name": "UserMessageReceivedEvent",
              "namespace": "museon_online.astor_butler.kafka.avro",
              "fields": [
                {"name": "eventId", "type": "string"},
                {"name": "eventType", "type": "string"},
                {"name": "eventVersion", "type": "string"},
                {"name": "occurredAt", "type": "string"},
                {"name": "source", "type": "string"},
                {"name": "channel", "type": ["null", "string"], "default": null},
                {"name": "idempotencyKey", "type": "string"},
                {
                  "name": "actor",
                  "type": {
                    "type": "record",
                    "name": "UserMessageActor",
                    "fields": [
                      {"name": "telegramUserId", "type": ["null", "long"], "default": null},
                      {"name": "chatId", "type": ["null", "long"], "default": null},
                      {"name": "username", "type": ["null", "string"], "default": null},
                      {"name": "firstName", "type": ["null", "string"], "default": null},
                      {"name": "lastName", "type": ["null", "string"], "default": null}
                    ]
                  }
                },
                {
                  "name": "payload",
                  "type": {
                    "type": "record",
                    "name": "UserMessageReceivedPayload",
                    "fields": [
                      {"name": "chatId", "type": ["null", "long"], "default": null},
                      {"name": "telegramUserId", "type": ["null", "long"], "default": null},
                      {"name": "telegramMessageId", "type": ["null", "int"], "default": null},
                      {"name": "telegramUpdateId", "type": ["null", "int"], "default": null},
                      {"name": "text", "type": ["null", "string"], "default": null},
                      {"name": "mediaKind", "type": ["null", "string"], "default": null},
                      {"name": "transcriptionStatus", "type": ["null", "string"], "default": null},
                      {"name": "transcript", "type": ["null", "string"], "default": null},
                      {"name": "storageObjectKey", "type": ["null", "string"], "default": null},
                      {"name": "contactPhonePresent", "type": "boolean", "default": false},
                      {"name": "previousState", "type": ["null", "string"], "default": null},
                      {"name": "nextState", "type": ["null", "string"], "default": null},
                      {"name": "actions", "type": {"type": "array", "items": "string"}, "default": []},
                      {"name": "fallback", "type": "boolean", "default": false}
                    ]
                  }
                }
              ]
            }
            """);

    public static final Schema LLM_RESPONSE_GENERATED = new Schema.Parser().parse("""
            {
              "type": "record",
              "name": "LlmResponseGeneratedEvent",
              "namespace": "museon_online.astor_butler.kafka.avro",
              "fields": [
                {"name": "eventId", "type": "string"},
                {"name": "eventType", "type": "string"},
                {"name": "eventVersion", "type": "string"},
                {"name": "occurredAt", "type": "string"},
                {"name": "source", "type": "string"},
                {"name": "channel", "type": ["null", "string"], "default": null},
                {"name": "idempotencyKey", "type": "string"},
                {"name": "sourceMessageEventId", "type": "string"},
                {
                  "name": "actor",
                  "type": {
                    "type": "record",
                    "name": "LlmResponseActor",
                    "fields": [
                      {"name": "telegramUserId", "type": ["null", "long"], "default": null},
                      {"name": "chatId", "type": ["null", "long"], "default": null},
                      {"name": "username", "type": ["null", "string"], "default": null},
                      {"name": "firstName", "type": ["null", "string"], "default": null},
                      {"name": "lastName", "type": ["null", "string"], "default": null}
                    ]
                  }
                },
                {
                  "name": "payload",
                  "type": {
                    "type": "record",
                    "name": "LlmResponseGeneratedPayload",
                    "fields": [
                      {"name": "chatId", "type": ["null", "long"], "default": null},
                      {"name": "telegramUserId", "type": ["null", "long"], "default": null},
                      {"name": "telegramMessageId", "type": ["null", "int"], "default": null},
                      {"name": "telegramUpdateId", "type": ["null", "int"], "default": null},
                      {"name": "state", "type": ["null", "string"], "default": null},
                      {"name": "stage", "type": ["null", "string"], "default": null},
                      {"name": "inputText", "type": ["null", "string"], "default": null},
                      {"name": "responseText", "type": ["null", "string"], "default": null},
                      {"name": "fallbackUsed", "type": "boolean", "default": false},
                      {"name": "prompt", "type": ["null", "string"], "default": null}
                    ]
                  }
                }
              ]
            }
            """);
}
