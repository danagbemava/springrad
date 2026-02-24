package {{packageName}}.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RateLimitConfig {

    @Bean
    Supplier<Bucket> bucketSupplier() {
        return () -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(120)
                        .refillGreedy(120, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    @Bean
    BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(120)
                        .refillGreedy(120, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
