package com.cgv.mediaservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudinaryUploadServiceTest {
    @Test
    void signsOnlyTheFieldsSentToCloudinary() {
        var service = new CloudinaryUploadService("demo", "1234", "abcd");
        assertEquals("5c9aa02ec00dcc865f9a1de21195f81a80e3dbfd", service.sign(1315060510L));

        var upload = service.createUploadSignature();
        assertEquals("demo", upload.cloudName());
        assertEquals("1234", upload.apiKey());
        assertEquals("cgv/movies", upload.folder());
        assertEquals(service.sign(upload.timestamp()), upload.signature());
        assertTrue(upload.timestamp() > 0);
    }

    @Test
    void rejectsMissingConfiguration() {
        var service = new CloudinaryUploadService("", "1234", "abcd");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, service::createUploadSignature).getStatusCode());
    }
}
