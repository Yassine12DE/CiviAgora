package tn.esprit.tic.civiAgora.config;

import org.springframework.web.socket.config.annotation.*;
/*
@Configuration
@EnableWebSocketMessageBroker*/
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

   /* @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/secured/user/queue/specific-user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/secured/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*");
        registry.addEndpoint("/ws").withSockJS();
    }*/

}
