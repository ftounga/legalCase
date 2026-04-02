package fr.ailegalcase.superadmin;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
public class RabbitMQManagementClient {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQManagementClient.class);

    private final RestClient restClient;
    private final String vhost;

    public RabbitMQManagementClient(
            @Value("${rabbitmq.management.url:http://localhost:15672}") String managementUrl,
            @Value("${rabbitmq.management.vhost:%2F}") String vhost,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password,
            RestClient.Builder builder) {
        this.vhost = vhost;
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        this.restClient = builder
                .baseUrl(managementUrl)
                .defaultHeader("Authorization", "Basic " + credentials)
                .build();
    }

    /** Fetch queue stats. Returns empty Optional on any error (fail-open). */
    public Optional<QueueStats> getQueue(String queueName) {
        try {
            QueueStats stats = restClient.get()
                    .uri("/api/queues/{vhost}/{queue}", vhost, queueName)
                    .retrieve()
                    .body(QueueStats.class);
            return Optional.ofNullable(stats);
        } catch (Exception e) {
            log.warn("RabbitMQ management API unavailable for queue {} — {}", queueName, e.getMessage());
            return Optional.empty();
        }
    }

    public record QueueStats(
            @JsonProperty("messages_ready") long messagesReady,
            @JsonProperty("messages_unacknowledged") long messagesUnacknowledged,
            @JsonProperty("consumers") long consumers) {}
}
