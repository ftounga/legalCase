package fr.ailegalcase.blog.image;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetryableImageGeneratorTest {

    private final OpenAiImageClient client = mock(OpenAiImageClient.class);
    private final RetryableImageGenerator generator = new RetryableImageGenerator(client, 2, 1, 1);

    @Test
    void successOnFirstAttempt_oneCall() {
        OpenAiImageClient.ImageGenerationResponse resp = new OpenAiImageClient.ImageGenerationResponse(
                new byte[]{1, 2}, "gpt-image-1", "prompt");
        when(client.generate(anyString())).thenReturn(resp);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Success.class);
        verify(client, times(1)).generate(anyString());
    }

    @Test
    void successOnSecondAttempt_after429_twoCallsTotal() {
        OpenAiImageClient.ImageGenerationResponse resp = new OpenAiImageClient.ImageGenerationResponse(
                new byte[]{1, 2}, "gpt-image-1", "prompt");
        HttpClientErrorException rateLimited = HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "rate", null,
                new byte[0], StandardCharsets.UTF_8);
        when(client.generate(anyString())).thenThrow(rateLimited).thenReturn(resp);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Success.class);
        verify(client, times(2)).generate(anyString());
    }

    @Test
    void successOnThirdAttempt_after2x500_threeCallsTotal() {
        OpenAiImageClient.ImageGenerationResponse resp = new OpenAiImageClient.ImageGenerationResponse(
                new byte[]{1, 2}, "gpt-image-1", "prompt");
        HttpServerErrorException down = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "down", null,
                new byte[0], StandardCharsets.UTF_8);
        when(client.generate(anyString())).thenThrow(down).thenThrow(down).thenReturn(resp);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Success.class);
        verify(client, times(3)).generate(anyString());
    }

    @Test
    void threeFailures_returnsFailedTransient() {
        HttpServerErrorException down = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "down", null,
                new byte[0], StandardCharsets.UTF_8);
        when(client.generate(anyString())).thenThrow(down);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Failed.class);
        RetryableImageGenerator.Outcome.Failed failed = (RetryableImageGenerator.Outcome.Failed) outcome;
        assertThat(failed.wasDefinitive()).isFalse();
        verify(client, times(3)).generate(anyString());
    }

    @Test
    void status400_atFirstAttempt_noRetry_failedDefinitive() {
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", null,
                new byte[0], StandardCharsets.UTF_8);
        when(client.generate(anyString())).thenThrow(badRequest);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Failed.class);
        RetryableImageGenerator.Outcome.Failed failed = (RetryableImageGenerator.Outcome.Failed) outcome;
        assertThat(failed.wasDefinitive()).isTrue();
        verify(client, times(1)).generate(anyString());
    }

    @Test
    void contentPolicyViolation_isDefinitive_noRetry() {
        // Simulé via 400 + body indiquant content_policy_violation (OpenAI renvoie 400)
        HttpClientErrorException violation = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "policy", null,
                "{\"error\":{\"code\":\"content_policy_violation\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(client.generate(anyString())).thenThrow(violation);

        RetryableImageGenerator.Outcome outcome = generator.generate("p");

        assertThat(outcome).isInstanceOf(RetryableImageGenerator.Outcome.Failed.class);
        RetryableImageGenerator.Outcome.Failed failed = (RetryableImageGenerator.Outcome.Failed) outcome;
        assertThat(failed.wasDefinitive()).isTrue();
        verify(client, times(1)).generate(anyString());
    }
}
