package com.socialpersona.message.websocket;

import com.socialpersona.persona.service.PersonaService;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class QQWebSocketConfigurator extends ServerEndpointConfig.Configurator
        implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (endpointClass == QQWebSocketHandler.class && applicationContext != null) {
            PersonaService personaService = applicationContext.getBean(PersonaService.class);
            return (T) new QQWebSocketHandler(personaService);
        }
        return super.getEndpointInstance(endpointClass);
    }
}
