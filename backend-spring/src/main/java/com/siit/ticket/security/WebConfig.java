package com.siit.ticket.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Removido mapeo estático de /uploads/** por motivos de seguridad.
        // Los archivos ahora se sirven de forma segura y autenticada.
    }
}
