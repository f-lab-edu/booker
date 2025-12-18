package com.bookerapp.core.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Booker API")
                .version("1.0")
                .description("Booker REST API Documentation"))
            .tags(Arrays.asList(
                new Tag().name("1. Book").description("도서 관리 API"),
                new Tag().name("2. Book Loan").description("도서 대출 관련 API"),
                new Tag().name("3. BookOrder").description("도서 주문 요청 관리 API"),
                new Tag().name("4. Event").description("이벤트 관리 API"),
                new Tag().name("5. Event Participation").description("이벤트 참여 신청 API - 동시성 제어 학습용"),
                new Tag().name("6. WorkLog").description("작업 로그 관리 API"),
                new Tag().name("7. LoadTest").description("부하 테스트 API"),
                new Tag().name("Authentication").description("인증 API")
            ));
    }
} 
