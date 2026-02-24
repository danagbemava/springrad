package {{packageName}}.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class DeadLetterQueueConfig {

    @Bean
    DeadLetterPublisher deadLetterPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeadLetterPublisher(kafkaTemplate, "events.main.dlt");
    }

    public static final class DeadLetterPublisher {
        private final KafkaTemplate<String, String> kafkaTemplate;
        private final String deadLetterTopic;

        DeadLetterPublisher(KafkaTemplate<String, String> kafkaTemplate, String deadLetterTopic) {
            this.kafkaTemplate = kafkaTemplate;
            this.deadLetterTopic = deadLetterTopic;
        }

        public void publish(String key, String payload) {
            kafkaTemplate.send(deadLetterTopic, key, payload);
        }
    }
}
