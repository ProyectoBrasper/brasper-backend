package com.brasper.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class ProdProfileConfigTest {

    @Test
    void theClientAddressComesFromTheProxyHeaderInProduction() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));

        assertThat(yaml.getObject().getProperty("server.forward-headers-strategy")).isEqualTo("native");
    }

    @Test
    void theApiDocsAreOffInProductionUnlessTheSwitchIsSet() {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        Properties prod = yaml.getObject();

        assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo("${SWAGGER_ENABLED:false}");
        assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("${SWAGGER_ENABLED:false}");
    }
}
