package fr.ailegalcase.casefile.conclusion;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * F-98 / SF-98-01 — déclaration de la queue RabbitMQ {@code case.conclusion}.
 *
 * <p>Profil {@code local}/{@code prod} uniquement, aligné sur
 * {@code RabbitMQConfig} : en profil de test, RabbitMQ n'est pas démarré et le
 * worker {@code CaseConclusionService} n'est pas chargé. {@code RabbitTemplate}
 * et le {@code MessageConverter} restent fournis par {@code RabbitMQConfig}.</p>
 */
@Configuration
@Profile({"local", "prod"})
public class CaseConclusionRabbitMQConfig {

    public static final String CASE_CONCLUSION_QUEUE = "case.conclusion";
    public static final String CASE_CONCLUSION_EXCHANGE = "case.conclusion.exchange";
    public static final String CASE_CONCLUSION_ROUTING_KEY = "case.conclusion";

    @Bean
    public Queue caseConclusionQueue() {
        return new Queue(CASE_CONCLUSION_QUEUE, true);
    }

    @Bean
    public DirectExchange caseConclusionExchange() {
        return new DirectExchange(CASE_CONCLUSION_EXCHANGE);
    }

    @Bean
    public Binding caseConclusionBinding(Queue caseConclusionQueue, DirectExchange caseConclusionExchange) {
        return BindingBuilder
                .bind(caseConclusionQueue)
                .to(caseConclusionExchange)
                .with(CASE_CONCLUSION_ROUTING_KEY);
    }
}
