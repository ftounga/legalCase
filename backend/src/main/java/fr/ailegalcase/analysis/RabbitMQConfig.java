package fr.ailegalcase.analysis;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class RabbitMQConfig {

    public static final String CHUNK_ANALYSIS_QUEUE = "chunk.analysis";
    public static final String CHUNK_ANALYSIS_EXCHANGE = "chunk.analysis.exchange";
    public static final String CHUNK_ANALYSIS_ROUTING_KEY = "chunk.analysis";

    public static final String DOCUMENT_ANALYSIS_QUEUE = "document.analysis";
    public static final String DOCUMENT_ANALYSIS_EXCHANGE = "document.analysis.exchange";
    public static final String DOCUMENT_ANALYSIS_ROUTING_KEY = "document.analysis";

    public static final String CASE_ANALYSIS_QUEUE = "case.analysis";
    public static final String CASE_ANALYSIS_EXCHANGE = "case.analysis.exchange";
    public static final String CASE_ANALYSIS_ROUTING_KEY = "case.analysis";

    public static final String AI_QUESTION_GENERATION_QUEUE = "ai.question.generation";
    public static final String AI_QUESTION_GENERATION_EXCHANGE = "ai.question.generation.exchange";
    public static final String AI_QUESTION_GENERATION_ROUTING_KEY = "ai.question.generation";

    @Bean
    public Queue chunkAnalysisQueue() {
        return new Queue(CHUNK_ANALYSIS_QUEUE, true);
    }

    @Bean
    public DirectExchange chunkAnalysisExchange() {
        return new DirectExchange(CHUNK_ANALYSIS_EXCHANGE);
    }

    @Bean
    public Binding chunkAnalysisBinding(Queue chunkAnalysisQueue, DirectExchange chunkAnalysisExchange) {
        return BindingBuilder
                .bind(chunkAnalysisQueue)
                .to(chunkAnalysisExchange)
                .with(CHUNK_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public Queue documentAnalysisQueue() {
        return new Queue(DOCUMENT_ANALYSIS_QUEUE, true);
    }

    @Bean
    public DirectExchange documentAnalysisExchange() {
        return new DirectExchange(DOCUMENT_ANALYSIS_EXCHANGE);
    }

    @Bean
    public Binding documentAnalysisBinding(Queue documentAnalysisQueue, DirectExchange documentAnalysisExchange) {
        return BindingBuilder
                .bind(documentAnalysisQueue)
                .to(documentAnalysisExchange)
                .with(DOCUMENT_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public Queue caseAnalysisQueue() {
        return new Queue(CASE_ANALYSIS_QUEUE, true);
    }

    @Bean
    public DirectExchange caseAnalysisExchange() {
        return new DirectExchange(CASE_ANALYSIS_EXCHANGE);
    }

    @Bean
    public Binding caseAnalysisBinding(Queue caseAnalysisQueue, DirectExchange caseAnalysisExchange) {
        return BindingBuilder
                .bind(caseAnalysisQueue)
                .to(caseAnalysisExchange)
                .with(CASE_ANALYSIS_ROUTING_KEY);
    }

    @Bean
    public Queue aiQuestionGenerationQueue() {
        return new Queue(AI_QUESTION_GENERATION_QUEUE, true);
    }

    @Bean
    public DirectExchange aiQuestionGenerationExchange() {
        return new DirectExchange(AI_QUESTION_GENERATION_EXCHANGE);
    }

    @Bean
    public Binding aiQuestionGenerationBinding(Queue aiQuestionGenerationQueue,
                                               DirectExchange aiQuestionGenerationExchange) {
        return BindingBuilder
                .bind(aiQuestionGenerationQueue)
                .to(aiQuestionGenerationExchange)
                .with(AI_QUESTION_GENERATION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
