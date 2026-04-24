package fr.ailegalcase.casefile;

import java.util.List;
import java.util.Map;

/**
 * Référentiel statique des jalons procéduraux pour {@code DROIT_DU_TRAVAIL} — F-136 SF-136-01.
 *
 * <p>Couvre les 6 procédures clés :
 * <ul>
 *   <li>{@link #PRUDHOMMES_FR} — Conseil de prud'hommes FR (BCO + bureau de jugement)</li>
 *   <li>{@link #APPEL_CA_SOCIALE_FR} — Appel chambre sociale Cour d'appel FR</li>
 *   <li>{@link #CASSATION_SOCIALE_FR} — Pourvoi en cassation chambre sociale FR</li>
 *   <li>{@link #TRIBUNAL_TRAVAIL_BE} — Tribunal du travail BE (1ʳᵉ instance)</li>
 *   <li>{@link #COUR_TRAVAIL_BE} — Cour du travail BE (appel)</li>
 *   <li>{@link #CASSATION_BE} — Cassation BE</li>
 * </ul>
 *
 * <p>Pattern symétrique à {@link ImmigrationProcedureReferentiel} (immigration). Ce référentiel
 * est un fallback — la source de vérité est la table {@code legal_referentials} de type
 * {@code TRAVAIL_PROCEDURE_JALONS} (seedée par la migration 130).
 *
 * <p>Délais procéduraux : les délais stricts (recours, pourvoi) renvoient à un article précis
 * (Code travail / CPC / CJ belge). Les délais d'audience et de délibéré sont indicatifs (pratiques
 * greffe) — documentés comme tels dans la description avocat de chaque entry DB.
 */
public final class TravailProcedureReferentiel {

    public record ProcedureJalon(String label, int offsetDays, String articleRef) {}

    // ---- Codes de procédure (entry_key DB) ----
    public static final String PRUDHOMMES_FR = "PRUDHOMMES_FR";
    public static final String APPEL_CA_SOCIALE_FR = "APPEL_CA_SOCIALE_FR";
    public static final String CASSATION_SOCIALE_FR = "CASSATION_SOCIALE_FR";
    public static final String TRIBUNAL_TRAVAIL_BE = "TRIBUNAL_TRAVAIL_BE";
    public static final String COUR_TRAVAIL_BE = "COUR_TRAVAIL_BE";
    public static final String CASSATION_BE = "CASSATION_BE";

    public static final String COUNTRY_FR = "FR";
    public static final String COUNTRY_BE = "BE";

    private static final Map<String, List<ProcedureJalon>> REFERENTIEL_FR = Map.ofEntries(
            // ---- PRUDHOMMES_FR (Conseil de prud'hommes — bureau de conciliation et orientation + bureau de jugement) ----
            // Délais d'audience et délibéré indicatifs (durée moyenne nationale CSM 2023). Délai de
            // convocation BCO = 15 jours minimum (R.1452-3) — on retient 45j pour intégrer le délai
            // matériel d'envoi par le greffe.
            Map.entry(PRUDHOMMES_FR, List.of(
                    new ProcedureJalon("Convocation au bureau de conciliation et d'orientation (BCO)", 45, "Code travail Art. R.1452-3"),
                    new ProcedureJalon("Audience du bureau de conciliation et d'orientation (BCO)", 90, "Code travail Art. L.1454-1"),
                    new ProcedureJalon("Renvoi au bureau de jugement (échec conciliation)", 180, "Code travail Art. L.1454-1-2"),
                    new ProcedureJalon("Audience du bureau de jugement", 270, "Code travail Art. R.1454-25"),
                    new ProcedureJalon("Délibéré et notification du jugement", 330, "Code travail Art. R.1454-25")
            )),
            // ---- APPEL_CA_SOCIALE_FR (Cour d'appel chambre sociale) ----
            // Délai d'appel = 1 mois (R.1461-1 Code travail) — converti en 30j.
            Map.entry(APPEL_CA_SOCIALE_FR, List.of(
                    new ProcedureJalon("Délai d'appel — chambre sociale Cour d'appel", 30, "Code travail Art. R.1461-1"),
                    new ProcedureJalon("Conclusions appelant (mise en état)", 90, "CPC Art. 908"),
                    new ProcedureJalon("Audience chambre sociale Cour d'appel", 270, "CPC Art. 779"),
                    new ProcedureJalon("Arrêt et notification", 330, "CPC Art. 538")
            )),
            // ---- CASSATION_SOCIALE_FR (Cour de cassation chambre sociale) ----
            // Délai pourvoi en matière sociale = 2 mois (art. 612 CPC).
            Map.entry(CASSATION_SOCIALE_FR, List.of(
                    new ProcedureJalon("Délai de pourvoi — chambre sociale Cour de cassation", 60, "CPC Art. 612"),
                    new ProcedureJalon("Mémoire ampliatif (demandeur)", 150, "CPC Art. 978"),
                    new ProcedureJalon("Audience chambre sociale Cour de cassation", 540, "CPC Art. 1009"),
                    new ProcedureJalon("Arrêt et notification", 630, "CPC Art. 1023")
            ))
    );

    private static final Map<String, List<ProcedureJalon>> REFERENTIEL_BE = Map.ofEntries(
            // ---- TRIBUNAL_TRAVAIL_BE (Tribunal du travail — 1ʳᵉ instance) ----
            // Compétence générale art. 578 CJ. Citation et 1ʳᵉ audience d'introduction art. 700 CJ.
            Map.entry(TRIBUNAL_TRAVAIL_BE, List.of(
                    new ProcedureJalon("Citation et audience d'introduction", 30, "CJ Art. 700"),
                    new ProcedureJalon("Mise en état — échange de conclusions", 120, "CJ Art. 747"),
                    new ProcedureJalon("Audience de plaidoiries", 240, "CJ Art. 755"),
                    new ProcedureJalon("Jugement et notification", 300, "CJ Art. 770")
            )),
            // ---- COUR_TRAVAIL_BE (Cour du travail — appel) ----
            // Délai d'appel = 1 mois (art. 1051 CJ) — converti en 30j.
            Map.entry(COUR_TRAVAIL_BE, List.of(
                    new ProcedureJalon("Délai d'appel — Cour du travail", 30, "CJ Art. 1051"),
                    new ProcedureJalon("Mise en état appel — conclusions", 150, "CJ Art. 747"),
                    new ProcedureJalon("Audience Cour du travail", 300, "CJ Art. 1056"),
                    new ProcedureJalon("Arrêt et notification", 360, "CJ Art. 770")
            )),
            // ---- CASSATION_BE (Cour de cassation BE) ----
            // Délai pourvoi = 3 mois (art. 1073 CJ) — converti en 90j.
            Map.entry(CASSATION_BE, List.of(
                    new ProcedureJalon("Délai de pourvoi — Cour de cassation BE", 90, "CJ Art. 1073"),
                    new ProcedureJalon("Mémoire en cassation", 180, "CJ Art. 1080"),
                    new ProcedureJalon("Audience Cour de cassation BE", 450, "CJ Art. 1095"),
                    new ProcedureJalon("Arrêt et notification", 540, "CJ Art. 1109")
            ))
    );

    private TravailProcedureReferentiel() {}

    /**
     * Résout les jalons procéduraux pour un type de procédure et un pays.
     *
     * @param typeProcedure code (ex. {@code PRUDHOMMES_FR}, {@code TRIBUNAL_TRAVAIL_BE})
     * @param country pays ({@code FR} ou {@code BE})
     * @return liste immuable des jalons ; vide si {@code typeProcedure} ou {@code country} null /
     *         inconnu / mal apparié (ex. {@code PRUDHOMMES_FR} avec country {@code BE}).
     */
    public static List<ProcedureJalon> resolve(String typeProcedure, String country) {
        if (typeProcedure == null || country == null) return List.of();
        String upperCountry = country.trim().toUpperCase();
        if (COUNTRY_FR.equals(upperCountry)) {
            return REFERENTIEL_FR.getOrDefault(typeProcedure, List.of());
        }
        if (COUNTRY_BE.equals(upperCountry)) {
            return REFERENTIEL_BE.getOrDefault(typeProcedure, List.of());
        }
        return List.of();
    }
}
