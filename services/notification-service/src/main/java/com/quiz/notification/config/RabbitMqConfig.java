package com.quiz.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange examEventsExchange(@Value("${app.rabbitmq.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue examSubmittedQueue(@Value("${app.rabbitmq.exam-submitted-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding examSubmittedBinding(
            Queue examSubmittedQueue,
            DirectExchange examEventsExchange,
            @Value("${app.rabbitmq.exam-submitted-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(examSubmittedQueue)
                .to(examEventsExchange)
                .with(routingKey);
    }

    @Bean
    public Queue classUserAddedQueue(@Value("${app.rabbitmq.class-user-added-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding classUserAddedBinding(
            Queue classUserAddedQueue,
            DirectExchange examEventsExchange,
            @Value("${app.rabbitmq.class-user-added-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(classUserAddedQueue)
                .to(examEventsExchange)
                .with(routingKey);
    }

    @Bean
    public Queue examCreatedQueue(@Value("${app.rabbitmq.exam-created-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding examCreatedBinding(
            Queue examCreatedQueue,
            DirectExchange examEventsExchange,
            @Value("${app.rabbitmq.exam-created-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(examCreatedQueue)
                .to(examEventsExchange)
                .with(routingKey);
    }

    @Bean
    public Queue authUserRegisteredQueue(@Value("${app.rabbitmq.auth-user-registered-queue}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding authUserRegisteredBinding(
            Queue authUserRegisteredQueue,
            DirectExchange examEventsExchange,
            @Value("${app.rabbitmq.auth-user-registered-routing-key}") String routingKey
    ) {
        return BindingBuilder.bind(authUserRegisteredQueue)
                .to(examEventsExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
