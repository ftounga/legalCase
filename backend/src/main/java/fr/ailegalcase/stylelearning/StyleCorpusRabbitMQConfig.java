package fr.ailegalcase.stylelearning;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * F-98 / SF-98-46 — déclaration de la queue RabbitMQ {@code style.corpus}.
 *
 * <p>Profil {@code local}/{@code prod} uniquement, aligné sur
 * {@code CaseConclusionRabbitMQConfig} : en profil de test, RabbitMQ n'est pas
 * démarré et le worker {@code StyleCorpusExtractionService} n'est pas chargé.</p>
 */
@Configuration
@Profile({"local", "prod"})
public class StyleCorpusRabbitMQConfig {

    public static final String STYLE_CORPUS_QUEUE = "style.corpus";
    public static final String STYLE_CORPUS_EXCHANGE = "style.corpus.exchange";
    public static final String STYLE_CORPUS_ROUTING_KEY = "style.corpus";

    @Bean
    public Queue styleCorpusQueue() {
        return new Queue(STYLE_CORPUS_QUEUE, true);
    }

    @Bean
    public DirectExchange styleCorpusExchange() {
        return new DirectExchange(STYLE_CORPUS_EXCHANGE);
    }

    @Bean
    public Binding styleCorpusBinding(Queue styleCorpusQueue, DirectExchange styleCorpusExchange) {
        return BindingBuilder
                .bind(styleCorpusQueue)
                .to(styleCorpusExchange)
                .with(STYLE_CORPUS_ROUTING_KEY);
    }
}
