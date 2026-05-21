package fr.ailegalcase.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientErrorLogServiceRateLimitTest {

    private final ClientErrorLogService service = new ClientErrorLogService();

    private ClientErrorPayload payload() {
        return new ClientErrorPayload("boom", null, null, null, null, null);
    }

    // R-01 + R-02 : 10 PASS pour un user puis 11ᵉ rate-limited
    @Test
    void user_tenAccepted_eleventhRejected() {
        for (int i = 0; i < 10; i++) {
            assertThat(service.tryLog(payload(), "user-A", null))
                    .as("call %d for user-A should be accepted", i + 1)
                    .isTrue();
        }
        assertThat(service.tryLog(payload(), "user-A", null))
                .as("11th call for user-A should be rejected")
                .isFalse();
    }

    // R-03 + R-04 : 5 PASS pour IP anonyme puis 6ᵉ rate-limited
    @Test
    void ip_fiveAccepted_sixthRejected() {
        for (int i = 0; i < 5; i++) {
            assertThat(service.tryLog(payload(), null, "1.2.3.4"))
                    .as("call %d for IP 1.2.3.4 should be accepted", i + 1)
                    .isTrue();
        }
        assertThat(service.tryLog(payload(), null, "1.2.3.4"))
                .as("6th call for IP 1.2.3.4 should be rejected")
                .isFalse();
    }

    // R-05 : compteurs isolés par user/IP
    @Test
    void differentUsers_separateCounters() {
        for (int i = 0; i < 10; i++) {
            service.tryLog(payload(), "user-A", null);
        }
        // user-B doit toujours pouvoir logger
        assertThat(service.tryLog(payload(), "user-B", null)).isTrue();
    }

    // R-06 : si user est non null, on ignore l'IP (et inversement)
    @Test
    void userTakesPrecedenceOverIp() {
        // 10 calls user-A — devraient saturer la limite user, pas la limite IP
        for (int i = 0; i < 10; i++) {
            assertThat(service.tryLog(payload(), "user-A", "1.2.3.4")).isTrue();
        }
        // 11ᵉ pour user-A bloqué
        assertThat(service.tryLog(payload(), "user-A", "1.2.3.4")).isFalse();
        // Mais user-B passe (compteur IP non incrémenté quand user présent)
        assertThat(service.tryLog(payload(), "user-B", "1.2.3.4")).isTrue();
    }

    // R-07 : tryLog avec userKey et ipKey null (cas dégénéré) → accepté sans rate limit
    @Test
    void noUserNoIp_alwaysAccepted() {
        for (int i = 0; i < 20; i++) {
            assertThat(service.tryLog(payload(), null, null)).isTrue();
        }
    }
}
