package br.org.edu.ifrn.LojaCarro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
                .addPathPatterns("/carro/**", "/usuario/**", "/log/**", "/uploads/**")
                // Rotas liberadas: entrar, sair, verificar sessão e criar conta
                .excludePathPatterns("/usuario/login", "/usuario/logout", "/usuario/logado", "/usuario/salvar");
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
