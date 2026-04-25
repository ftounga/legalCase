package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des jalons procéduraux pour {@code DROIT_FAMILLE} — F-137 SF-F-137-01.
 *
 * <p>Périmètre actuel : Belgique uniquement. Couvre les 3 procédures clés famille BE :
 * <ul>
 *   <li>{@link #TRIBUNAL_FAMILLE_BE} — Tribunal de la famille (1ʳᵉ instance, art. 572bis CJ)</li>
 *   <li>{@link #COUR_APPEL_FAMILLE_BE} — Cour d'appel chambre famille (art. 1051 CJ)</li>
 *   <li>{@link #CASSATION_FAMILLE_BE} — Pourvoi en cassation (art. 1073 CJ)</li>
 * </ul>
 *
 * <p>Pattern symétrique strict à {@link TravailProcedureReferentiel} (F-136). Ce référentiel
 * est un fallback — la source de vérité est la table {@code legal_referentials} de type
 * {@code FAMILLE_PROCEDURE_JALONS} (seedée par la migration 162).
 *
 * <p>Délais procéduraux : les délais stricts (recours 1 mois art. 1051 CJ, pourvoi 3 mois
 * art. 1073 CJ) renvoient à un article précis du Code judiciaire belge. Les délais d'audience
 * et de délibéré sont indicatifs (pratiques greffe) — documentés comme tels dans la description
 * avocat de chaque entry DB.
 *
 * <p>La SF jumelle SF-F-137-02 (à venir) couvrira la France (JAF, audience MEC, ONC, appel CA,
 * Cassation civile). Cette classe accepte déjà un paramètre {@code country} prêt à recevoir
 * les types FR sans renommage.
 */
public final class FamilleProcedureReferentiel {

    public record ProcedureJalon(String label, int offsetDays, String articleRef) {}

    // ---- Codes de procédure (entry_key DB) ----
    public static final String TRIBUNAL_FAMILLE_BE = "TRIBUNAL_FAMILLE_BE";
    public static final String COUR_APPEL_FAMILLE_BE = "COUR_APPEL_FAMILLE_BE";
    public static final String CASSATION_FAMILLE_BE = "CASSATION_FAMILLE_BE";

    public static final String COUNTRY_FR = "FR";
    public static final String COUNTRY_BE = "BE";

    private static final Map<String, List<ProcedureJalon>> REFERENTIEL_BE = Map.ofEntries(
            // ---- TRIBUNAL_FAMILLE_BE (Tribunal de la famille — 1ʳᵉ instance) ----
            // Compétence générale art. 572bis CJ. Audience MP en référé familial art. 1253ter/2 CJ.
            // Mise en état art. 747 CJ. Jugement art. 770 CJ.
            Map.entry(TRIBUNAL_FAMILLE_BE, List.of(
                    new ProcedureJalon("Dépôt de la requête au greffe du tribunal de la famille", 0, "CJ Art. 572bis"),
                    new ProcedureJalon("Audience des mesures provisoires (MP) — ordonnance", 30, "CJ Art. 1253ter/2"),
                    new ProcedureJalon("Mise en état — calendrier de procédure (échange de conclusions)", 120, "CJ Art. 747"),
                    new ProcedureJalon("Audience de plaidoiries au fond", 180, "CJ Art. 755"),
                    new ProcedureJalon("Prononcé du jugement et notification", 240, "CJ Art. 770")
            )),
            // ---- COUR_APPEL_FAMILLE_BE (Cour d'appel — chambre famille) ----
            // Délai d'appel = 1 mois (art. 1051 CJ) — converti en 30j.
            Map.entry(COUR_APPEL_FAMILLE_BE, List.of(
                    new ProcedureJalon("Délai d'appel — Cour d'appel chambre famille", 30, "CJ Art. 1051"),
                    new ProcedureJalon("Mise en état appel — conclusions", 150, "CJ Art. 747"),
                    new ProcedureJalon("Audience Cour d'appel chambre famille", 300, "CJ Art. 1056"),
                    new ProcedureJalon("Arrêt et notification", 360, "CJ Art. 770")
            )),
            // ---- CASSATION_FAMILLE_BE (Cour de cassation BE) ----
            // Délai pourvoi = 3 mois (art. 1073 CJ) — converti en 90j.
            Map.entry(CASSATION_FAMILLE_BE, List.of(
                    new ProcedureJalon("Délai de pourvoi — Cour de cassation BE", 90, "CJ Art. 1073"),
                    new ProcedureJalon("Mémoire en cassation", 180, "CJ Art. 1080"),
                    new ProcedureJalon("Audience Cour de cassation BE", 450, "CJ Art. 1095"),
                    new ProcedureJalon("Arrêt et notification", 540, "CJ Art. 1109")
            ))
    );

    private FamilleProcedureReferentiel() {}

    /**
     * Résout les jalons procéduraux pour un type de procédure et un pays.
     *
     * @param typeProcedure code (ex. {@code TRIBUNAL_FAMILLE_BE})
     * @param country pays ({@code BE} actuellement ; {@code FR} en SF-F-137-02 jumelle)
     * @return liste immuable des jalons ; vide si {@code typeProcedure} ou {@code country} null /
     *         inconnu / mal apparié (ex. {@code TRIBUNAL_FAMILLE_BE} avec country {@code FR}).
     */
    public static List<ProcedureJalon> resolve(String typeProcedure, String country) {
        if (typeProcedure == null || country == null) return List.of();
        String upperCountry = country.trim().toUpperCase();
        if (COUNTRY_BE.equals(upperCountry)) {
            return REFERENTIEL_BE.getOrDefault(typeProcedure, List.of());
        }
        // FR à venir en SF-F-137-02 jumelle — pour l'instant retour vide.
        return List.of();
    }
}
