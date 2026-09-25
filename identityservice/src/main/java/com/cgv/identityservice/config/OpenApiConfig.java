package com.cgv.identityservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("CGV Identity Service API")
                        .version("1.0.0")
                        .description("Tài liệu API xác thực, phân quyền và quản lý người dùng hệ thống CGV Cinemas.")
                        .contact(new Contact()
                                .name("CGV Tech Team")
                                .email("dev@cgv.vn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8000" + contextPath)
                                .description("Chế độ 1: Qua Kong API Gateway (Cổng 8000)"),
                        new Server()
                                .url("http://localhost:8081" + contextPath)
                                .description("Chế độ 2: Trực tiếp Port chính Service (Local 8081)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Chỉ cần dán JWT Token vào đây (Swagger sẽ tự động gắn tiền tố Bearer)")));
    }
}
