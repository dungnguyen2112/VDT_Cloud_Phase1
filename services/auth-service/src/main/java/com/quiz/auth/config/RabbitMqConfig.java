package com.quiz.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.QueueBuilder;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange examEventsExchange(
            @Value("${app.rabbitmq.exchange:exam.events}") String exchangeName
    ) {
        // Durable direct exchange, shared with notification-service.
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue authUserRegisteredQueue(
            @Value("${app.rabbitmq.auth-user-registered-queue:notification.auth.user.registered}") String queueName
    ) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding authUserRegisteredBinding(
            Queue authUserRegisteredQueue,
            DirectExchange examEventsExchange,
            @Value("${app.rabbitmq.auth-user-registered-routing-key:auth.user.registered}") String routingKey
    ) {
        return BindingBuilder.bind(authUserRegisteredQueue)
                .to(examEventsExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}

