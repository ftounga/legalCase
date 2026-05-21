package fr.ailegalcase.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de logging des erreurs frontend.
 *
 * <p>Logge en SLF4J ERROR pour que Fluent Bit relaie vers CloudWatch et que le
 * metric filter {@code "ERROR"} matche. Applique un rate limit in-memory à
 * fenêtre glissante 60s pour éviter de saturer les logs en cas de bug
 * récurrent côté client.</p>
 *
 * <p>SF-INFRA-09.</p>
 */
@Service
public class ClientErrorLogService {

    private static final Logger log = LoggerFactory.getLogger(ClientErrorLogService.class);

    static final int MAX_EVENTS_PER_USER_PER_MINUTE = 10;
    static final int MAX_EVENTS_PER_IP_PER_MINUTE = 5;
    static final long WINDOW_NANOS = 60_000_000_000L; // 60s

    private final Map<String, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> ipTimestamps = new ConcurrentHashMap<>();

    /**
     * Tente de logger l'erreur. Retourne {@code false} si rate-limited.
     *
     * @param payload le payload validé
     * @param userKey identifiant utilisateur authentifié (peut être null)
     * @param ipKey   adresse IP du caller (peut être null)
     * @return true si le log a été émis, false si rate-limited
     */
    public boolean tryLog(ClientErrorPayload payload, String userKey, String ipKey) {
        if (userKey != null) {
            if (!allow(userTimestamps, userKey, MAX_EVENTS_PER_USER_PER_MINUTE)) {
                return false;
            }
        } else if (ipKey != null) {
            if (!allow(ipTimestamps, ipKey, MAX_EVENTS_PER_IP_PER_MINUTE)) {
                return false;
            }
        }

        log.error("[FRONTEND] {} | url={} | ua={} | stack={}",
                safe(payload.message()),
                safe(payload.url()),
                safe(payload.userAgent()),
                safe(payload.stack()));
        return true;
    }

    private static boolean allow(Map<String, Deque<Long>> bucket, String key, int max) {
        long now = System.nanoTime();
        Deque<Long> queue = bucket.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            Iterator<Long> it = queue.iterator();
            while (it.hasNext()) {
                long ts = it.next();
                if (now - ts > WINDOW_NANOS) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (queue.size() >= max) {
                return false;
            }
            queue.addLast(now);
            return true;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
