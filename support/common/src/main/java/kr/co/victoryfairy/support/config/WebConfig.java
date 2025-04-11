package kr.co.victoryfairy.support.config;

import kr.co.victoryfairy.support.interceptor.CurlCommandErrorInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web에 사용되는 Configuration을 여기에 위치 한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurlCommandErrorInterceptor curlCommandErrorInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(curlCommandErrorInterceptor)
                .addPathPatterns("/**"); // 모든 요청에 적용
    }

}
