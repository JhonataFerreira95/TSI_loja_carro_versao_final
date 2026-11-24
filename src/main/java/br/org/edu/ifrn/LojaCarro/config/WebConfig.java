package br.org.edu.ifrn.LojaCarro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.uploads.directory:uploads}")
    private String diretorioUploads;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/carro/**", "/usuario/**", "/agendamento/**", "/agendamento", "/log/**", "/uploads/**")
                // Login, logout e consulta da própria sessão não exigem autorização prévia.
                .excludePathPatterns("/usuario/login", "/usuario/logout", "/usuario/logado");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/veiculos").setViewName("forward:/index.html");
        registry.addViewController("/veiculos/{id}").setViewName("forward:/index.html");
        registry.addViewController("/cadastro").setViewName("forward:/index.html");
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/test-drive/{id}").setViewName("forward:/index.html");
        registry.addViewController("/meus-agendamentos").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/index.html");
        registry.addViewController("/admin/veiculos").setViewName("forward:/index.html");
        registry.addViewController("/admin/agendamentos").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String localizacao = Path.of(diretorioUploads).toAbsolutePath().normalize().toUri().toString();
        if (!localizacao.endsWith("/")) {
            localizacao += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(localizacao);
    }
}
