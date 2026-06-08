package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.JsonNode;
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

    // SF-JU-06-FIX2 — le lien de fallback doit pointer vers courdecassation.fr/decision/
    // (l'id JUDILIBRE n'est pas un id Légifrance → legifrance/juri/id = 403).
    @Test
    void parseArret_fallsBackToCourDeCassation_whenNoSolutionUrl() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JudilibreApiClient client = new JudilibreApiClient("id", "secret", om);
        JsonNode node = om.readTree(
                "{\"id\":\"616676d0a1c75d6f42603ee6\",\"number\":\"18-18.022\",\"jurisdiction\":\"cc\"}");

        JudilibreArret arret = client.parseArret(node);

        assertThat(arret.lienLegifrance())
                .isEqualTo("https://www.courdecassation.fr/decision/616676d0a1c75d6f42603ee6");
    }

    @Test
    void parseArret_keepsSolutionUrl_whenProvided() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JudilibreApiClient client = new JudilibreApiClient("id", "secret", om);
        JsonNode node = om.readTree(
                "{\"id\":\"abc\",\"solution_url\":\"https://www.legifrance.gouv.fr/juri/id/JURITEXT000045728912\"}");

        JudilibreArret arret = client.parseArret(node);

        assertThat(arret.lienLegifrance())
                .isEqualTo("https://www.legifrance.gouv.fr/juri/id/JURITEXT000045728912");
    }
}
