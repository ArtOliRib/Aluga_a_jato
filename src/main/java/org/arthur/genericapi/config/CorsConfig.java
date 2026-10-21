package org.arthur.genericapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Libera o acesso do front-end.
 *
 * <p>Sem isto, o navegador bloqueia qualquer chamada de outra origem: a API
 * responde, mas o JavaScript nunca ve a resposta. O sintoma e confuso para quem
 * depura, porque o erro aparece no console do browser como falha de rede, e nao
 * como erro HTTP.
 *
 * <p>Aplica-se apenas ao navegador. {@code curl}, Postman e testes de
 * integracao nao passam por CORS e por isso continuam funcionando sem isto.
 *
 * <p>As origens ficam em {@code locadora.cors.origens-permitidas} (veja
 * {@code application.properties}) para que dev e producao usem listas
 * diferentes sem alterar codigo. O padrao cobre as portas usuais de dev
 * (Vite usa 5173).
 *
 * <p>O valor padrao esta declarado aqui, e nao so no {@code application.properties},
 * porque o arquivo de propriedades de teste <b>substitui</b> o de producao em vez
 * de complementa-lo: sem o default no codigo, toda a suite falharia ao subir o
 * contexto por falta do placeholder.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> origensPermitidas;

    public CorsConfig(@Value("${locadora.cors.origens-permitidas:http://localhost:5173,http://localhost:3000}") List<String> origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origensPermitidas.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // O POST de carro e de aluguel devolve o header Location, que e o
                // que permite ao front saber a URL do recurso recem-criado. Sem
                // expor, o JavaScript le esse header como null.
                .exposedHeaders("Location")
                .maxAge(3600);
    }
}
