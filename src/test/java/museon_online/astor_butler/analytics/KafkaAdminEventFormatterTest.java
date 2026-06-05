package museon_online.astor_butler.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.kafka.AvroUserEventFactory;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaAdminEventFormatterTest {

    private final KafkaAdminEventFormatter formatter = new KafkaAdminEventFormatter(new ObjectMapper());

    @Test
    void formatsUserMessageEventForHumans() {
        String payload = """
                {
                  "eventId": "telegram:284069874",
                  "eventType": "USER_MESSAGE_RECEIVED",
                  "channel": "TELEGRAM",
                  "chatId": 1773317437,
                  "username": "Poedinenko",
                  "firstName": "Наталья",
                  "lastName": "Поединенко",
                  "text": "",
                  "contactPhonePresent": true,
                  "previousState": "CONSENT_REQUIRED",
                  "nextState": "READY_FOR_DIALOG",
                  "actions": ["CONTACT_CAPTURED", "OPEN_MENU"],
                  "fallback": false
                }
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "astor.user.events",
                2,
                39,
                "telegram:284069874",
                payload
        );

        String text = formatter.format(record, "telegram:284069874");

        assertThat(text).contains("USER_MESSAGE_RECEIVED");
        assertThat(text).contains("Наталья Поединенко (@Poedinenko)");
        assertThat(text).contains("CONSENT_REQUIRED -&gt; READY_FOR_DIALOG");
        assertThat(text).contains("CONTACT_CAPTURED, OPEN_MENU");
        assertThat(text).contains("astor.user.events[2] offset=39");
    }

    @Test
    void fallsBackForUnparseablePayload() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "astor.user.events",
                0,
                1,
                "broken",
                "not-json"
        );

        String text = formatter.format(record, "broken");

        assertThat(text).contains("UNPARSEABLE_KAFKA_PAYLOAD");
        assertThat(text).contains("not-json");
    }

    @Test
    void formatsAvroUserMessageEventForHumans() {
        AvroUserEventFactory factory = new AvroUserEventFactory();
        IncomingMessage incoming = IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                349,
                284069874,
                "",
                "+79990000000",
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069874"
        );
        OutgoingMessage outgoing = OutgoingMessage.of(
                incoming,
                "Спасибо, контакт получил.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("CONTACT_CAPTURED", "OPEN_MENU")
        );
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                "astor.user.events",
                1,
                40,
                factory.partitionKey(incoming),
                factory.userMessageReceived(incoming, BotState.CONSENT_REQUIRED, outgoing)
        );

        String text = formatter.format(record, factory.eventId(incoming));

        assertThat(record.key()).isEqualTo("telegram:user:1773317437");
        assertThat(text).contains("USER_MESSAGE_RECEIVED");
        assertThat(text).contains("Наталья Поединенко (@Poedinenko)");
        assertThat(text).contains("CONSENT_REQUIRED -&gt; READY_FOR_DIALOG");
        assertThat(text).contains("astor.user.events[1] offset=40");
        assertThat(text).contains("Event: telegram:update:284069874");
    }
}
