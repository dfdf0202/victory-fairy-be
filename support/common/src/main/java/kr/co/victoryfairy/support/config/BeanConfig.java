package kr.co.victoryfairy.support.config;

import jakarta.servlet.Filter;
import kr.co.victoryfairy.support.webfilter.RequestResponseCachingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * 일반적인 Bean으로 등록될 Configuration을 여기에 위치 한다.
 */
@Configuration
public class BeanConfig {

    @Bean
    ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }


    @Bean
    public Filter requestResponseCachingFilter() {
        return new RequestResponseCachingFilter();
    }
}
