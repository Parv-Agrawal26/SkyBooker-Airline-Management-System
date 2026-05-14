package com.skybooker.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "skybooker.exchange";

    public static final String USER_REGISTERED_QUEUE   = "notification.user.registered";
    public static final String BOOKING_CREATED_QUEUE   = "notification.booking.created";
    public static final String PAYMENT_CONFIRMED_QUEUE = "notification.payment.confirmed";
    public static final String PAYMENT_REFUNDED_QUEUE  = "notification.payment.refunded";

    public static final String USER_REGISTERED_KEY   = "user.registered";
    public static final String BOOKING_CREATED_KEY   = "booking.created";
    public static final String PAYMENT_CONFIRMED_KEY = "payment.confirmed";
    public static final String PAYMENT_REFUNDED_KEY  = "payment.refunded";

    @Bean public TopicExchange skybookerExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean public Queue userRegisteredQueue()   { return QueueBuilder.durable(USER_REGISTERED_QUEUE).build(); }
    @Bean public Queue bookingCreatedQueue()   { return QueueBuilder.durable(BOOKING_CREATED_QUEUE).build(); }
    @Bean public Queue paymentConfirmedQueue() { return QueueBuilder.durable(PAYMENT_CONFIRMED_QUEUE).build(); }
    @Bean public Queue paymentRefundedQueue()  { return QueueBuilder.durable(PAYMENT_REFUNDED_QUEUE).build(); }

    @Bean public Binding userRegisteredBinding()   { return BindingBuilder.bind(userRegisteredQueue()).to(skybookerExchange()).with(USER_REGISTERED_KEY); }
    @Bean public Binding bookingCreatedBinding()   { return BindingBuilder.bind(bookingCreatedQueue()).to(skybookerExchange()).with(BOOKING_CREATED_KEY); }
    @Bean public Binding paymentConfirmedBinding() { return BindingBuilder.bind(paymentConfirmedQueue()).to(skybookerExchange()).with(PAYMENT_CONFIRMED_KEY); }
    @Bean public Binding paymentRefundedBinding()  { return BindingBuilder.bind(paymentRefundedQueue()).to(skybookerExchange()).with(PAYMENT_REFUNDED_KEY); }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonMessageConverter());
        return t;
    }
}
