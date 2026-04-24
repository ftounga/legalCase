package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-IM-14-02 : calculateur pour la régularisation 9ter médical belge
 * (art. 9ter Loi du 15 décembre 1980 + AR 17/05/2007 art. 7-8).
 *
 * <p>Procédure de régularisation ouverte à l'étranger souffrant d'une <b>maladie
 * grave</b> nécessitant des soins impossibles ou inaccessibles dans son pays
 * d'origine.
 *
 * <p>4 conditions cumulatives (chacune = 25 pts) :
 * <ul>
 *   <li>Maladie grave certifiée (certificat médical type CM 1 et 2)</li>
 *   <li>Soins nécessaires disponibles en Belgique</li>
 *   <li>Soins inaccessibles dans le pays d'origine (preuve documentée)</li>
 *   <li>Pas de menace à l'ordre public</li>
 * </ul>
 *
 * <p>Instruction par l'Office des étrangers (OE) + avis du médecin-fonctionnaire
 * délégué. Délai d'instruction moyen : 6-24 mois. Projection à 12 mois.
 *
 * <p>En cas de refus, recours en annulation devant le Conseil du contentieux
 * des étrangers (CCE) dans les 30 jours.
 *
 * <p>Outil <b>single-country BELGIQUE</b> — le pendant français (étranger malade
 * L.425-9 CESEDA) est juridiquement distinct.
 */
public final class Belgian9terCalculator {

    /** Délai d'instruction prévisionnel (mois). */
    public static final int DELAI_INSTRUCTION_MOIS = 12;

    private static final String BASE_JURIDIQUE =
            "Loi 15/12/1980 art. 9ter + AR 17/05/2007 art. 7-8";

    private Belgian9terCalculator() {
    }

    public static Belgian9terResult compute(LocalDate dateDebutSymptomes,
                                            boolean maladieGraveCertifiee,
                                            boolean soinsNecessairesDisponiblesBe,
                                            boolean soinsInaccessiblesPaysOrigine,
                                            boolean menaceOrdrePublic,
                                            LocalDate dateDepotDemande) {
        return compute(dateDebutSymptomes, maladieGraveCertifiee,
                soinsNecessairesDisponiblesBe, soinsInaccessiblesPaysOrigine,
                menaceOrdrePublic, dateDepotDemande,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static Belgian9terResult compute(LocalDate dateDebutSymptomes,
                                            boolean maladieGraveCertifiee,
                                            boolean soinsNecessairesDisponiblesBe,
                                            boolean soinsInaccessiblesPaysOrigine,
                                            boolean menaceOrdrePublic,
                                            LocalDate dateDepotDemande,
                                            Clock clock) {
        validateInputs(dateDebutSymptomes, dateDepotDemande, clock);

        boolean certificatMedicalType1Ok = maladieGraveCertifiee;
        boolean soinsRequisOk = soinsNecessairesDisponiblesBe;
        boolean inaccessibiliteOk = soinsInaccessiblesPaysOrigine;
        boolean pasMenace = !menaceOrdrePublic;

        int scoreGlobal = computeScore(certificatMedicalType1Ok, soinsRequisOk,
                inaccessibiliteOk, pasMenace);

        String verdict = computeVerdict(scoreGlobal);

        LocalDate dateExpirationInstruction = dateDepotDemande != null
                ? dateDepotDemande.plusMonths(DELAI_INSTRUCTION_MOIS)
                : null;

        List<String> criteresNonRemplis = buildCriteresNonRemplis(
                certificatMedicalType1Ok, soinsRequisOk,
                inaccessibiliteOk, pasMenace);

        List<String> messages = buildMessages(certificatMedicalType1Ok,
                soinsRequisOk, inaccessibiliteOk, pasMenace);

        String formule = buildFormule(scoreGlobal, verdict, pasMenace,
                dateExpirationInstruction);

        return new Belgian9terResult(
                dateDebutSymptomes,
                maladieGraveCertifiee,
                soinsNecessairesDisponiblesBe,
                soinsInaccessiblesPaysOrigine,
                menaceOrdrePublic,
                dateDepotDemande,
                certificatMedicalType1Ok,
                soinsRequisOk,
                inaccessibiliteOk,
                pasMenace,
                scoreGlobal,
                verdict,
                criteresNonRemplis,
                dateExpirationInstruction,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateDebutSymptomes,
                                       LocalDate dateDepotDemande,
                                       Clock clock) {
        LocalDate today = LocalDate.now(clock);
        if (dateDebutSymptomes != null && dateDebutSymptomes.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDebutSymptomes ne peut pas être dans le futur");
        }
        if (dateDepotDemande != null && dateDepotDemande.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être dans le futur");
        }
        if (dateDebutSymptomes != null && dateDepotDemande != null
                && dateDepotDemande.isBefore(dateDebutSymptomes)) {
            throw new IllegalArgumentException(
                    "dateDepotDemande ne peut pas être antérieure à dateDebutSymptomes");
        }
    }

    private static int computeScore(boolean certificatOk,
                                    boolean soinsRequisOk,
                                    boolean inaccessibiliteOk,
                                    boolean pasMenace) {
        int score = 0;
        if (certificatOk) score += 25;
        if (soinsRequisOk) score += 25;
        if (inaccessibiliteOk) score += 25;
        if (pasMenace) score += 25;
        return score;
    }

    private static String computeVerdict(int score) {
        if (score >= 75) return "ELEVEE";
        if (score >= 50) return "MOYENNE";
        return "FAIBLE";
    }

    private static List<String> buildCriteresNonRemplis(boolean certificatOk,
                                                        boolean soinsRequisOk,
                                                        boolean inaccessibiliteOk,
                                                        boolean pasMenace) {
        List<String> criteres = new ArrayList<>();
        if (!certificatOk) {
            criteres.add("Certificat médical type (CM 1 et 2) non produit");
        }
        if (!soinsRequisOk) {
            criteres.add("Soins nécessaires non disponibles en Belgique");
        }
        if (!inaccessibiliteOk) {
            criteres.add("Inaccessibilité des soins au pays d'origine non prouvée");
        }
        if (!pasMenace) {
            criteres.add("Menace ordre public présumée");
        }
        return criteres;
    }

    private static List<String> buildMessages(boolean certificatOk,
                                              boolean soinsRequisOk,
                                              boolean inaccessibiliteOk,
                                              boolean pasMenace) {
        List<String> messages = new ArrayList<>();
        messages.add("Procédure 9ter (Loi 15/12/1980) : régularisation pour "
                + "maladie grave — instruction OE + avis médecin-fonctionnaire "
                + "délégué, délai 6-24 mois.");
        messages.add("Certificat médical type à actualiser tous les 6 mois.");
        messages.add("Examen médecin-fonctionnaire délégué obligatoire.");

        if (!certificatOk) {
            messages.add("Produire le certificat médical type CM 1 et 2 rempli "
                    + "par un médecin belge — pièce maîtresse de la demande.");
        }
        if (!soinsRequisOk) {
            messages.add("Documenter la disponibilité effective du traitement en "
                    + "Belgique (attestation médicale, protocole de soins).");
        }
        if (!inaccessibiliteOk) {
            messages.add("Constituer un dossier de preuve sur l'inaccessibilité "
                    + "des soins au pays d'origine : rapports OMS / MSF / ONG, "
                    + "avis médical documenté, sources pays spécifiques.");
        }
        if (!pasMenace) {
            messages.add("La menace à l'ordre public fait obstacle — examen "
                    + "approfondi des antécédents judiciaires requis.");
        }
        messages.add("En cas de refus, recours CCE annulation dans 30 jours.");
        return messages;
    }

    private static String buildFormule(int score,
                                       String verdict,
                                       boolean pasMenace,
                                       LocalDate dateExpirationInstruction) {
        String suffixDelai = dateExpirationInstruction != null
                ? " — délai d'instruction prévisionnel jusqu'au "
                        + dateExpirationInstruction
                : "";
        if (!pasMenace) {
            return "Régularisation 9ter médical (art. 9ter Loi 15/12/1980) : "
                    + "menace ordre public présumée — demande vouée au rejet"
                    + suffixDelai + ".";
        }
        return String.format(
                "Régularisation 9ter médical (art. 9ter Loi 15/12/1980) : "
                        + "score %d/100, probabilité d'acceptation %s%s.",
                score, verdict, suffixDelai);
    }
}
