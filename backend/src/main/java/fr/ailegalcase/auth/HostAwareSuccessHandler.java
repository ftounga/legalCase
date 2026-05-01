package fr.ailegalcase.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.List;

/**
 * Redirige l'utilisateur vers `<origine de la requête>/dashboard` après login OAuth,
 * à condition que l'origine fasse partie de la liste blanche `app.allowed-frontend-urls`.
 *
 * Permet à plusieurs hosts (legalcase.fr, legalcase.ng-itconsulting.com, leurs équivalents staging)
 * de servir l'app en parallèle pendant la migration de domaine F-143.
 *
 * Sans cette logique, `defaultSuccessUrl` rebascule tous les utilisateurs vers un host fixe,
 * ce qui casse la session OAuth (cookie lié au domaine) sur l'autre host.
 */
public class HostAwareSuccessHandler implements AuthenticationSuccessHandler {

    private static final String DASHBOARD_PATH = "/dashboard";

    private final List<String> allowedFrontendUrls;
    private final String fallbackFrontendUrl;

    public HostAwareSuccessHandler(List<String> allowedFrontendUrls, String fallbackFrontendUrl) {
        this.allowedFrontendUrls = List.copyOf(allowedFrontendUrls);
        this.fallbackFrontendUrl = fallbackFrontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String origin = computeRequestOrigin(request);
        String target = allowedFrontendUrls.contains(origin)
                ? origin + DASHBOARD_PATH
                : fallbackFrontendUrl + DASHBOARD_PATH;
        response.sendRedirect(target);
    }

    private String computeRequestOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("https".equals(scheme) && port == 443)
                || ("http".equals(scheme) && port == 80);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}
