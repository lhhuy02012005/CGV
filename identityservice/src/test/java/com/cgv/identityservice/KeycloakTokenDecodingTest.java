package com.cgv.identityservice;

import com.cgv.identityservice.dto.request.UserCreationParam;
import com.cgv.identityservice.repository.httpclient.KeycloakClient;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.cloud.openfeign.support.FeignHttpMessageConverters;
import org.springframework.cloud.openfeign.support.HttpMessageConverterCustomizer;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class KeycloakTokenDecodingTest {
    @Test void springFeignDecoderReadsTheActualKeycloakTokenReturnType() throws Exception {
        try (var context = decoderContext()) {
            var decoder = new SpringDecoder(context.getBeanProvider(FeignHttpMessageConverters.class));
            var type = KeycloakClient.class.getMethod("exchangeToken", String.class, Map.class).getGenericReturnType();
            try (var response = response(200, Map.of("Content-Type", List.of("application/json")),
                    "{\"access_token\":\"test-access\",\"refresh_token\":\"test-refresh\",\"expires_in\":300}")) {
                JsonNode token = assertInstanceOf(JsonNode.class, decoder.decode(response, type));
                assertEquals("test-access", token.get("access_token").asText());
                assertEquals("test-refresh", token.get("refresh_token").asText());
                assertEquals(300, token.get("expires_in").asLong());
            }
        }
    }

    @Test void userCreationKeepsLocationForAnEmptyCreatedResponse() throws Exception {
        try (var context = decoderContext()) {
            var decoder = new ResponseEntityDecoder(new SpringDecoder(context.getBeanProvider(FeignHttpMessageConverters.class)));
            var type = KeycloakClient.class.getMethod("createUser", String.class, String.class, UserCreationParam.class).getGenericReturnType();
            try (var response = response(201, Map.of("Location", List.of("http://keycloak/admin/realms/test/users/test-id")), "")) {
                var result = assertInstanceOf(ResponseEntity.class, decoder.decode(response, type));
                assertEquals(201, result.getStatusCode().value());
                assertTrue(result.getHeaders().getFirst("Location").endsWith("/test-id"));
            }
        }
    }

    private GenericApplicationContext decoderContext() {
        var context = new GenericApplicationContext();
        context.registerBean(FeignHttpMessageConverters.class, () -> new FeignHttpMessageConverters(
                context.getBeanProvider(ClientHttpMessageConvertersCustomizer.class),
                context.getBeanProvider(HttpMessageConverterCustomizer.class)));
        context.refresh();
        return context;
    }

    private Response response(int status, Map<String, java.util.Collection<String>> headers, String body) {
        return Response.builder().status(status).reason("test").headers(headers)
                .request(Request.create(Request.HttpMethod.POST, "http://localhost/test", Map.of(), new byte[0], StandardCharsets.UTF_8, null))
                .body(body, StandardCharsets.UTF_8).build();
    }
}
