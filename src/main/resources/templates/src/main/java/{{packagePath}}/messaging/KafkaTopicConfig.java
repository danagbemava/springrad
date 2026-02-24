package {{packageName}}.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic eventsTopic() {
        return TopicBuilder.name("events.main")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic deadLetterTopic() {
        return TopicBuilder.name("events.main.dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
