package fr.ailegalcase.blog.image;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiErrorClassifierTest {

    @Test
    void status429_isTransient() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "rate limited", null,
                "{\"error\":{\"code\":\"rate_limit_exceeded\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void status429_insufficientQuota_isDefinitive() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "no credit", null,
                "{\"error\":{\"code\":\"insufficient_quota\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.DEFINITIVE);
    }

    @Test
    void status500_isTransient() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "server error", null,
                new byte[0], StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void status503_isTransient() {
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "down", null,
                new byte[0], StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void status400_isDefinitive() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad input", null,
                new byte[0], StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.DEFINITIVE);
    }

    @Test
    void status401_isDefinitive() {
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "no auth", null,
                new byte[0], StandardCharsets.UTF_8);
        assertThat(OpenAiErrorClassifier.classify(ex))
                .isEqualTo(OpenAiErrorClassifier.Classification.DEFINITIVE);
    }

    @Test
    void ioException_isTransient() {
        assertThat(OpenAiErrorClassifier.classify(new IOException("network")))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void socketTimeout_isTransient() {
        assertThat(OpenAiErrorClassifier.classify(new SocketTimeoutException("timeout")))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void wrappedIOException_isTransient() {
        RuntimeException wrapper = new RuntimeException(new IOException("wrapped"));
        assertThat(OpenAiErrorClassifier.classify(wrapper))
                .isEqualTo(OpenAiErrorClassifier.Classification.TRANSIENT);
    }

    @Test
    void unknownException_isDefinitive() {
        assertThat(OpenAiErrorClassifier.classify(new RuntimeException("???")))
                .isEqualTo(OpenAiErrorClassifier.Classification.DEFINITIVE);
    }
}
