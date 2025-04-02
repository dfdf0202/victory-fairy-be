package kr.co.victoryfairy.core.craw.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
public class BeanConfig {

    @Bean(destroyMethod = "close") // 종료 시 자동으로 리소스 정리
    public Browser browser() {
        Playwright playwright = Playwright.create(); // Playwright 자체는 여기서 닫아줘야 함
        return playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(false) // 디버깅 : false
                        .setSlowMo(50)
                        .setExecutablePath(Paths.get("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")) // 디버깅
        );
    }

}
