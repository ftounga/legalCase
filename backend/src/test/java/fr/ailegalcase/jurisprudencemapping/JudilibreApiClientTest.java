package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-01 / SF-JU-01-02 — tests UT minimaux du
 * {@link JudilibreApiClient}.
 *
 * <p>Comportement principal vérifié : sans credentials configurés, le client
 * fait un no-op (retourne liste vide + log WARN) — propriété cruciale pour la
 * CI et le dev local qui n'ont pas de compte OAuth2 PISTE.</p>
 *
 * <p>Les chemins HTTP (token refresh, retry exponentiel, parsing pagination)
 * ne sont pas testés avec un vrai mock HTTP — décision pragmatique : la
 * validation passe par staging post-déploiement avec vrai compte PISTE.</p>
 */
class JudilibreApiClientTest {

    @Test
    void fetchArretsForPeriod_returnsEmpty_whenCredentialsBlank() {
        JudilibreApiClient client = new JudilibreApiClient("", "", new ObjectMapper());

        List<JudilibreArret> result = client.fetchArretsForPeriod(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchArretsForPeriod_returnsEmpty_whenClientIdNull() {
        JudilibreApiClient client = new JudilibreApiClient(null, "secret", new ObjectMapper());

        List<JudilibreArret> result = client.fetchArretsForPeriod(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchArretsForPeriod_returnsEmpty_whenSecretBlank() {
        JudilibreApiClient client = new JudilibreApiClient("id", "  ", new ObjectMapper());

        List<JudilibreArret> result = client.fetchArretsForPeriod(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));

        assertThat(result).isEmpty();
    }
}
