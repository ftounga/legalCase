package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-210-03 : analyseur du choix d'option successorale (Cciv art. 768 et s.).
 *
 * <p>3 options ouvertes à tout héritier (art. 768) :
 * <ul>
 *   <li>{@code ACCEPTATION_PURE_SIMPLE} — recueil intégral, responsabilité
 *       indéfinie aux dettes de la succession (art. 785).</li>
 *   <li>{@code ACCEPTATION_CONCURRENCE_ACTIF} — recueil + limitation de
 *       responsabilité au montant de l'actif net (art. 791-3) ; nécessite
 *       inventaire et déclaration au greffe.</li>
 *   <li>{@code RENONCIATION} — abandon de la qualité d'héritier
 *       (art. 804) ; nécessite déclaration au greffe.</li>
 * </ul>
 *
 * <p>Délai pour exercer l'option (art. 771) : <b>4 mois</b> à compter de
 * l'ouverture de la succession (date du décès). Au delà, l'héritier peut
 * être sommé d'opter et dispose alors d'un délai supplémentaire (2 mois,
 * art. 772). Pour les héritiers de second rang appelés par la défaillance
 * de l'héritier de premier rang, on retient ici le délai cumulé indicatif
 * de <b>6 mois</b>.
 *
 * <p>Acceptation tacite (art. 783) : si l'héritier accomplit, avant l'option,
 * un acte qui suppose nécessairement l'intention d'accepter (vente d'un
 * bien, paiement d'une dette en qualité d'héritier…), il est <b>réputé</b>
 * avoir accepté purement et simplement — la renonciation n'est plus
 * possible.
 *
 * <p>Recommandation prudente (best-practice notariale) :
 * <ul>
 *   <li>Acceptation pure et simple si actif clairement positif et dettes
 *       documentées exhaustivement.</li>
 *   <li>Acceptation à concurrence de l'actif net si dettes incertaines ou
 *       actif/passif proches (sécurise la responsabilité).</li>
 *   <li>Renonciation si passif > actif et pas d'acte équivalent posé.</li>
 * </ul>
 */
public final class AcceptationRenonciationSuccessionCalculator {

    public static final int DELAI_PREMIER_RANG_JOURS = 120; // ~ 4 mois
    public static final int DELAI_SECOND_RANG_JOURS = 180;  // 4 + 2 mois

    public static final String OPTION_PURE_SIMPLE = "ACCEPTATION_PURE_SIMPLE";
    public static final String OPTION_CONCURRENCE_ACTIF = "ACCEPTATION_CONCURRENCE_ACTIF";
    public static final String OPTION_RENONCIATION = "RENONCIATION";

    static final String BASE_JURIDIQUE =
            "Cciv art. 768+ + 771-772 (délais) + 783 (acceptation tacite) + 791 (effet)";

    private AcceptationRenonciationSuccessionCalculator() {
    }

    public static AcceptationRenonciationSuccessionResult compute(
            LocalDate dateOuvertureSuccession,
            String qualiteHeritier,
            double actifBrutEur,
            double passifEur,
            boolean actesEquivalentAcceptationDejaPosesDetected,
            boolean inventaireRealise,
            boolean dettesIncertainesDetected,
            String intentionExprimee,
            LocalDate today) {

        if (dateOuvertureSuccession == null) {
            throw new IllegalArgumentException("dateOuvertureSuccession est requis");
        }
        if (today == null) today = LocalDate.now();
        if (dateOuvertureSuccession.isAfter(today)) {
            throw new IllegalArgumentException("dateOuvertureSuccession ne peut être dans le futur");
        }
        if (actifBrutEur < 0) {
            throw new IllegalArgumentException("actifBrutEur doit être >= 0");
        }
        if (passifEur < 0) {
            throw new IllegalArgumentException("passifEur doit être >= 0");
        }
        boolean secondRang = "SECOND_RANG".equals(qualiteHeritier);
        int delaiTotalJours = secondRang ? DELAI_SECOND_RANG_JOURS : DELAI_PREMIER_RANG_JOURS;
        long elapsed = ChronoUnit.DAYS.between(dateOuvertureSuccession, today);
        int delaiRestantJours = (int) (delaiTotalJours - elapsed);

        List<String> options = new ArrayList<>();
        // Si actes équivalents posés → seulement PURE_SIMPLE (réputée acceptée)
        if (actesEquivalentAcceptationDejaPosesDetected) {
            options.add(OPTION_PURE_SIMPLE);
        } else {
            options.add(OPTION_PURE_SIMPLE);
            options.add(OPTION_CONCURRENCE_ACTIF);
            options.add(OPTION_RENONCIATION);
        }

        String reco = recommandation(actifBrutEur, passifEur, dettesIncertainesDetected,
                actesEquivalentAcceptationDejaPosesDetected);

        String formule = buildFormule(actifBrutEur, passifEur, reco, delaiRestantJours,
                actesEquivalentAcceptationDejaPosesDetected);

        List<String> messages = buildMessages(dateOuvertureSuccession, secondRang,
                delaiRestantJours, actifBrutEur, passifEur, dettesIncertainesDetected,
                inventaireRealise, actesEquivalentAcceptationDejaPosesDetected,
                intentionExprimee, reco);

        return new AcceptationRenonciationSuccessionResult(
                dateOuvertureSuccession,
                qualiteHeritier != null ? qualiteHeritier : "PREMIER_RANG",
                actifBrutEur,
                passifEur,
                actesEquivalentAcceptationDejaPosesDetected,
                inventaireRealise,
                dettesIncertainesDetected,
                intentionExprimee,
                options,
                reco,
                delaiRestantJours,
                delaiTotalJours,
                BASE_JURIDIQUE,
                formule,
                messages);
    }

    // -----------------------------------------------------------------------
    // Recommandation
    // -----------------------------------------------------------------------

    private static String recommandation(double actif, double passif,
                                         boolean dettesIncertaines,
                                         boolean actesPoses) {
        if (actesPoses) {
            // L'acceptation pure et simple est réputée acquise — pas de choix
            return OPTION_PURE_SIMPLE;
        }
        if (dettesIncertaines) {
            return OPTION_CONCURRENCE_ACTIF;
        }
        if (passif <= 0 && actif > 0) {
            return OPTION_PURE_SIMPLE;
        }
        if (passif > actif) {
            return OPTION_RENONCIATION;
        }
        // Actif/passif proches (ratio passif/actif > 0.6) → prudence
        if (actif > 0 && (passif / actif) > 0.6) {
            return OPTION_CONCURRENCE_ACTIF;
        }
        return OPTION_PURE_SIMPLE;
    }

    private static String buildFormule(double actif, double passif, String reco,
                                       int delaiRestantJours, boolean actesPoses) {
        StringBuilder sb = new StringBuilder();
        sb.append("Actif ").append((long) actif).append(" € — passif ").append((long) passif).append(" €. ");
        if (actesPoses) {
            sb.append("Acceptation pure et simple réputée acquise (actes équivalents — art. 783). ");
        } else {
            sb.append("Recommandation : ").append(libelleOption(reco)).append(". ");
        }
        if (delaiRestantJours <= 0) {
            sb.append("Délai légal dépassé — régulariser sans délai par déclaration au greffe.");
        } else {
            sb.append("Délai restant : ").append(delaiRestantJours).append(" jours.");
        }
        return sb.toString();
    }

    private static List<String> buildMessages(LocalDate dateOuverture,
                                               boolean secondRang,
                                               int delaiRestantJours,
                                               double actif,
                                               double passif,
                                               boolean dettesIncertaines,
                                               boolean inventaireRealise,
                                               boolean actesPoses,
                                               String intentionExprimee,
                                               String reco) {
        List<String> m = new ArrayList<>();
        m.add("Succession ouverte le " + dateOuverture
                + (secondRang ? " (héritier de second rang — délai 6 mois)"
                              : " (héritier de premier rang — délai 4 mois)")
                + " — art. 771-772 Cciv.");
        if (delaiRestantJours <= 0) {
            m.add("Délai légal dépassé — l'héritier peut être sommé d'opter (art. 771 al. 2). À régulariser.");
        } else if (delaiRestantJours < 30) {
            m.add("Délai restant < 30 jours — décider rapidement.");
        }
        if (actesPoses) {
            m.add("Actes équivalents à acceptation détectés (art. 783) : la renonciation N'EST PLUS POSSIBLE. Acceptation pure et simple réputée acquise.");
        }
        if (dettesIncertaines) {
            m.add("Dettes incertaines — recommander acceptation à concurrence de l'actif net (art. 791-3) avec inventaire notarié.");
        }
        if (!inventaireRealise && OPTION_CONCURRENCE_ACTIF.equals(reco)) {
            m.add("Inventaire à faire dresser par notaire — délai 2 mois après la déclaration au greffe (art. 789).");
        }
        if (passif > actif && !actesPoses) {
            m.add("Passif supérieur à l'actif — la renonciation protège l'héritier de toute responsabilité aux dettes (art. 805).");
        }
        if (intentionExprimee != null && !intentionExprimee.isBlank()
                && !"INCERTAIN".equals(intentionExprimee)
                && !intentionExprimee.equals(reco)) {
            m.add("L'intention exprimée par le client ('" + intentionExprimee
                    + "') diverge de la recommandation prudente ('" + reco
                    + "'). À discuter en RDV avant déclaration au greffe.");
        }
        m.add("Toute renonciation ou acceptation à concurrence de l'actif net se déclare au greffe du Tribunal Judiciaire (art. 804 / 788 Cciv).");
        return m;
    }

    private static String libelleOption(String reco) {
        return switch (reco) {
            case OPTION_PURE_SIMPLE -> "acceptation pure et simple";
            case OPTION_CONCURRENCE_ACTIF -> "acceptation à concurrence de l'actif net";
            case OPTION_RENONCIATION -> "renonciation";
            default -> reco;
        };
    }
}
