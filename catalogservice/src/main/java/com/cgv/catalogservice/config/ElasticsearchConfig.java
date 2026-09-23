package com.cgv.catalogservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String elasticsearchUri;

    @Override
    public ClientConfiguration clientConfiguration() {
        String hostAndPort = elasticsearchUri.replace("http://", "").replace("https://", "");
        return ClientConfiguration.builder()
                .connectedTo(hostAndPort)
                .withHeaders(() -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Accept", "application/vnd.elasticsearch+json;compatible-with=8");
                    headers.add("Content-Type", "application/vnd.elasticsearch+json;compatible-with=8");
                    return headers;
                })
                .build();
    }
}
