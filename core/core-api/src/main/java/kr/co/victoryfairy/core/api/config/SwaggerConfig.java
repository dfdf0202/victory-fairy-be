package kr.co.victoryfairy.core.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import kr.co.victoryfairy.support.utils.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.HashMap;

@Configuration
public class SwaggerConfig {

    private static final String TITLE = "VictoryFairy API Docs";
    private static final String APP_START_TIME = DateUtils.now(DateUtils.Format.DATETIME_FORMAT_HYPEN.getPattern());

    @Bean
    public OpenAPI openAPI() throws UnknownHostException {
        return new OpenAPI().info(new Info()
                        .title(TITLE)
                        .description(String.format("* Application Start Time : %s",  APP_START_TIME))
                        .version("v1.0"))
                .components(new Components().schemas(new HashMap<>()));
    }


}
