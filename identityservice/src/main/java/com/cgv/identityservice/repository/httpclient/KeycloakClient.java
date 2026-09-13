package com.cgv.identityservice.repository.httpclient;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "keycloak-client", url = "${keycloak.auth-server-url:http://localhost:8180}")
public interface KeycloakClient {

    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    JsonNode exchangeToken(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );

    @PostMapping(value = "/realms/{realm}/protocol/openid-connect/logout",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void logout(
            @PathVariable("realm") String realm,
            Map<String, ?> formParams
    );
}