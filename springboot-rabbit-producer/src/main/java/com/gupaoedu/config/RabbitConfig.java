package com.gupaoedu.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * rabbitmq服务端确认消息的两种方式
 * 1.事务模式(不推荐)
 * 2.确认模式(推荐异步)
 *
 * channel是为了减少tcp连接资源的消耗
 * vhost是为了资源隔离
 */
@Configuration
public class RabbitConfig {
    /**
     * 所有的消息发送都会转换成JSON格式发到交换机
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());


        //如果消息没有路由成功(消息被mq退回)

        //当mandatory标志位设置为true时
        //如果exchange根据自身类型和消息routingKey无法找到一个合适的queue存储消息
        //那么broker会调用basic.return方法将消息返还给生产者
        //当mandatory设置为false时，出现上述情况broker会直接将消息丢弃
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            System.out.println("被退回的消息:" + new String(message.getBody()));
            System.out.println("replyCode:" + replyCode);
            System.out.println("replyText:" + replyText);
            System.out.println("exchange:" + exchange);
            System.out.println("routingKey:" + routingKey);
        });

        //确认消息是否被mq接收(异步确认模式:一边发送一边确认)
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                //如果消息发送失败,可以将消息保存下来,也可以重发
                System.out.println("发送消息失败" + cause);
                throw new RuntimeException("发送异常" + cause);
            }
        });
        return rabbitTemplate;
    }
}
