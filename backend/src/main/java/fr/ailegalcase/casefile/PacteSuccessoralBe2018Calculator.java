package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-211-04 : calculateur du pacte successoral global belge.
 * Loi 31/07/2017 modifiant CC art. 1100/1+ — entrée en vigueur 01/09/2018.
 *
 * <p>Vérifie :
 * <ul>
 *   <li>Date de signature ≥ 01/09/2018 (concept inexistant avant)</li>
 *   <li>Acte authentique notarié OBLIGATOIRE (art. 1100/5 CC) — sinon NUL</li>
 *   <li>Présence ET accord de tous les héritiers réservataires (art. 1100/2 CC)</li>
 *   <li>Équilibre des donations rapportables (art. 1100/7 CC) — sinon CONTESTABLE</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b>.
 */
public final class PacteSuccessoralBe2018Calculator {

    /** Date d'entrée en vigueur de la Loi 31/07/2017 (réforme successions). */
    public static final LocalDate ENTREE_EN_VIGUEUR = LocalDate.of(2018, 9, 1);

    public static final String VERDICT_VALIDE = "VALIDE";
    public static final String VERDICT_CONTESTABLE = "CONTESTABLE";
    public static final String VERDICT_NUL = "NUL";

    private static final String BASE_JURIDIQUE =
            "Loi du 31/07/2017 ; CC art. 1100/1 à 1100/7 (entrée en vigueur 01/09/2018).";

    private PacteSuccessoralBe2018Calculator() {
    }

    public static PacteSuccessoralBe2018Result compute(LocalDate dateSignaturePacte,
                                                       boolean acteAuthentique,
                                                       boolean accordTousHeritiersReservataires,
                                                       boolean equilibreDonationsRapportables,
                                                       boolean presenceTousHeritiersReservataires) {
        return compute(dateSignaturePacte, acteAuthentique, accordTousHeritiersReservataires,
                equilibreDonationsRapportables, presenceTousHeritiersReservataires,
                Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static PacteSuccessoralBe2018Result compute(LocalDate dateSignaturePacte,
                                                       boolean acteAuthentique,
                                                       boolean accordTousHeritiersReservataires,
                                                       boolean equilibreDonationsRapportables,
                                                       boolean presenceTousHeritiersReservataires,
                                                       Clock clock) {
        validateInputs(dateSignaturePacte, clock);

        List<String> motifsAnnulation = new ArrayList<>();
        List<String> motifsContestabilite = new ArrayList<>();

        if (!acteAuthentique) {
            motifsAnnulation.add("Pacte successoral non passé en la forme authentique notariée — "
                    + "art. 1100/5 CC (nullité absolue).");
        }
        if (!presenceTousHeritiersReservataires) {
            motifsAnnulation.add("Tous les héritiers réservataires n'ont pas été appelés au pacte — "
                    + "art. 1100/2 CC (nullité).");
        }
        if (!accordTousHeritiersReservataires) {
            // si présents mais pas d'accord → contestabilité, sinon déjà couvert ci-dessus
            if (presenceTousHeritiersReservataires) {
                motifsContestabilite.add("Un ou plusieurs héritiers réservataires présents n'ont pas "
                        + "donné leur accord — risque de contestation art. 1100/2 CC.");
            }
        }
        if (!equilibreDonationsRapportables) {
            motifsContestabilite.add("Déséquilibre constaté dans les donations rapportables — "
                    + "risque de remise en cause art. 1100/7 CC.");
        }

        String verdict;
        if (!motifsAnnulation.isEmpty()) {
            verdict = VERDICT_NUL;
        } else if (!motifsContestabilite.isEmpty()) {
            verdict = VERDICT_CONTESTABLE;
        } else {
            verdict = VERDICT_VALIDE;
        }

        List<String> messages = buildMessages(verdict);
        String formule = buildFormule(dateSignaturePacte, verdict);

        return new PacteSuccessoralBe2018Result(
                dateSignaturePacte,
                acteAuthentique,
                accordTousHeritiersReservataires,
                equilibreDonationsRapportables,
                presenceTousHeritiersReservataires,
                verdict,
                motifsAnnulation,
                motifsContestabilite,
                formule,
                BASE_JURIDIQUE,
                messages);
    }

    private static void validateInputs(LocalDate dateSignaturePacte, Clock clock) {
        if (dateSignaturePacte == null) {
            throw new IllegalArgumentException("dateSignaturePacte est requise");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateSignaturePacte.isAfter(today)) {
            throw new IllegalArgumentException(
                    "dateSignaturePacte ne peut pas être dans le futur");
        }
        if (dateSignaturePacte.isBefore(ENTREE_EN_VIGUEUR)) {
            throw new IllegalArgumentException(
                    "Le pacte successoral global n'existe en droit belge que depuis le "
                            + ENTREE_EN_VIGUEUR + " (Loi 31/07/2017).");
        }
    }

    private static List<String> buildMessages(String verdict) {
        List<String> messages = new ArrayList<>();
        messages.add("Pacte successoral global : acte authentique notarié OBLIGATOIRE (art. 1100/5 CC).");
        messages.add("Présence ET accord de TOUS les héritiers réservataires requis (art. 1100/2 CC).");
        messages.add("Le pacte fige les donations rapportables et leur équilibre (art. 1100/7 CC).");
        messages.add("Effet : opposabilité aux héritiers présents — interdiction d'action en réduction "
                + "ultérieure pour ce qui est rapportable.");

        switch (verdict) {
            case VERDICT_VALIDE -> messages.add(
                    "Pacte valide — toutes les conditions formelles et de fond sont remplies.");
            case VERDICT_CONTESTABLE -> messages.add(
                    "Pacte contestable — risque de remise en cause partielle, à régulariser.");
            case VERDICT_NUL -> messages.add(
                    "Pacte nul (nullité absolue) — refaire l'acte selon les conditions légales.");
            default -> { /* unreachable */ }
        }
        return messages;
    }

    private static String buildFormule(LocalDate dateSignature, String verdict) {
        return String.format(
                "Pacte successoral global signé le %s — verdict %s (Loi 31/07/2017 ; CC art. 1100/1-7).",
                dateSignature, verdict);
    }
}
