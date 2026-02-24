package {{packageName}}.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;

public abstract class BaseConsumer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String deadLetterTopic;

    protected BaseConsumer(KafkaTemplate<String, String> kafkaTemplate, String deadLetterTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.deadLetterTopic = deadLetterTopic;
    }

    @KafkaListener(topics = "${app.messaging.topic:events.main}", groupId = "${spring.application.name}")
    public void onMessage(@Payload String payload) {
        try {
            handle(payload);
        } catch (Exception ex) {
            kafkaTemplate.send(deadLetterTopic, payload);
        }
    }

    protected abstract void handle(String payload);
}
