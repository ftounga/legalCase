package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-215-19 — calculateur de l'outil "Protection temporaire Ukraine BE" (F-IM-34).
 *
 * <p>Fondement : directive 2001/55/CE (protection temporaire en cas d'afflux massif
 * de personnes déplacées) activée par la décision d'exécution UE 2022/382 du Conseil
 * du 04/03/2022 à l'égard des personnes déplacées d'Ukraine, prolongée annuellement
 * (toujours active en 2026) ; transposition belge : Loi du 15/12/1980 art. 57/29 et
 * suivants. Particularité clé : <b>droit au travail immédiat</b> dès l'enregistrement
 * à l'Office des étrangers (art. 57/29), sans permis unique (single permit) requis.
 *
 * <p>Outil de type <b>information + checklist + calcul de durée</b> — il n'y a PAS de
 * délai de recours à calculer (≠ F-IM-31 / F-IM-32 / F-IM-33).
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> (droit des étrangers belge) — distinct de la
 * protection subsidiaire art. 48/4 (F-221) et de la demande d'asile CGRA.
 *
 * <p>La date de fin de la protection temporaire est <b>paramétrable</b>
 * ({@code protection.temporaire.ukraine.date-fin}) et doit être mise à jour
 * annuellement au gré des prolongations successives de la directive.
 */
public final class ProtectionTemporaireUkraineBeCalculator {

    /** Date d'activation de la protection temporaire Ukraine — décision UE 2022/382. */
    public static final LocalDate DATE_ACTIVATION_PT = LocalDate.of(2022, 2, 24);

    /** Seuil (strict) en jours restants en deçà duquel le renouvellement est imminent. */
    static final long SEUIL_RENOUVELLEMENT_JOURS = 90L;

    private static final String BASE_JURIDIQUE =
            "Directive 2001/55/CE (protection temporaire) ; décision d'exécution UE 2022/382 "
                    + "du Conseil du 04/03/2022 (personnes déplacées d'Ukraine), prolongée "
                    + "annuellement ; Loi du 15/12/1980 art. 57/29 et suivants (transposition belge — "
                    + "droit au travail immédiat sans single permit)";

    private static final String DROITS_TRAVAIL =
            "Droit au travail immédiat dès enregistrement OE (art. 57/29 Loi 15/12/1980) — "
                    + "pas de single permit requis";

    private ProtectionTemporaireUkraineBeCalculator() {
    }

    /**
     * Calcule l'éligibilité, la durée restante et les droits associés à la protection
     * temporaire Ukraine.
     *
     * @param today   date de référence injectée (testabilité) — typiquement {@code LocalDate.now()}.
     * @param dateFin date de fin de la protection temporaire (paramètre
     *                {@code protection.temporaire.ukraine.date-fin}).
     */
    public static ProtectionTemporaireUkraineBeResult compute(
            LocalDate dateArrivee,
            Boolean nationaliteUkrainienne,
            Boolean residenceUkraineAvant24Fev2022,
            Boolean apatridesUkraine,
            Boolean membreFamilleProtege,
            ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE,
            LocalDate today,
            LocalDate dateFin) {

        validate(dateArrivee, nationaliteUkrainienne, residenceUkraineAvant24Fev2022,
                titreSejourBE, today, dateFin);

        boolean nationalite = nationaliteUkrainienne;
        boolean residenceAvant = residenceUkraineAvant24Fev2022;
        boolean apatride = Boolean.TRUE.equals(apatridesUkraine);
        boolean membreFamille = Boolean.TRUE.equals(membreFamilleProtege);

        boolean eligible = (nationalite || apatride || membreFamille) && residenceAvant;

        long dureeProtectionRestante = ChronoUnit.DAYS.between(today, dateFin);
        boolean prochainRenouvellement =
                dureeProtectionRestante < SEUIL_RENOUVELLEMENT_JOURS;

        List<String> droitsAides = List.of(
                "Aide sociale du CPAS (équivalent au revenu d'intégration) pendant la durée de la protection.",
                "Accès à un logement / hébergement temporaire via le réseau d'accueil et les communes.",
                "Accès à l'enseignement obligatoire pour les enfants et à la formation pour adultes.",
                "Accès aux soins de santé (couverture médicale via le CPAS / mutuelle)."
        );

        List<String> cheminProcedure = List.of(
                "Présentation à l'Office des étrangers (OE) pour enregistrement de la protection temporaire.",
                "Délivrance de l'attestation d'immatriculation (annexe 35) — séjour provisoire.",
                "Inscription au registre des étrangers de la commune de résidence.",
                "Activation des droits (travail immédiat, CPAS, logement, enseignement, soins) et délivrance du titre A."
        );

        String recommandation = buildRecommandation(eligible, prochainRenouvellement,
                dureeProtectionRestante, titreSejourBE);

        return new ProtectionTemporaireUkraineBeResult(
                dateArrivee,
                nationalite,
                residenceAvant,
                apatride,
                membreFamille,
                titreSejourBE,
                eligible,
                dateFin,
                dureeProtectionRestante,
                prochainRenouvellement,
                DROITS_TRAVAIL,
                droitsAides,
                cheminProcedure,
                recommandation,
                BASE_JURIDIQUE
        );
    }

    private static String buildRecommandation(boolean eligible,
                                              boolean prochainRenouvellement,
                                              long dureeProtectionRestante,
                                              ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE) {
        if (!eligible) {
            return "Conditions de la protection temporaire Ukraine NON réunies (nationalité "
                    + "ukrainienne / apatride / membre de famille protégé + résidence habituelle "
                    + "en Ukraine avant le 24/02/2022). Examiner une autre voie : protection "
                    + "subsidiaire art. 48/4 (F-221) ou demande de protection internationale (CGRA).";
        }
        StringBuilder sb = new StringBuilder(
                "Bénéficiaire éligible à la protection temporaire Ukraine — droit au travail "
                        + "immédiat sans single permit (art. 57/29). ");
        if (titreSejourBE == ProtectionTemporaireUkraineBeTitreSejourEnum.AUCUN) {
            sb.append("Aucun titre encore délivré : se présenter à l'Office des étrangers pour "
                    + "enregistrement et obtention de l'attestation d'immatriculation (annexe 35). ");
        }
        if (prochainRenouvellement) {
            sb.append("ALERTE — renouvellement imminent : ");
            if (dureeProtectionRestante <= 0) {
                sb.append("la protection enregistrée arrive à échéance ou est échue ; ");
            } else {
                sb.append("il reste ").append(dureeProtectionRestante)
                        .append(" jour(s) ; ");
            }
            sb.append("anticiper la prolongation du titre A et vérifier la dernière décision "
                    + "de prolongation de la directive.");
        } else {
            sb.append("Protection en cours — ").append(dureeProtectionRestante)
                    .append(" jour(s) restant(s) avant l'échéance paramétrée.");
        }
        return sb.toString();
    }

    private static void validate(LocalDate dateArrivee,
                                 Boolean nationaliteUkrainienne,
                                 Boolean residenceUkraineAvant24Fev2022,
                                 ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE,
                                 LocalDate today,
                                 LocalDate dateFin) {
        if (dateArrivee == null) {
            throw new IllegalArgumentException("dateArrivee est requise");
        }
        if (nationaliteUkrainienne == null) {
            throw new IllegalArgumentException("nationaliteUkrainienne est requise");
        }
        if (residenceUkraineAvant24Fev2022 == null) {
            throw new IllegalArgumentException("residenceUkraineAvant24Fev2022 est requise");
        }
        if (titreSejourBE == null) {
            throw new IllegalArgumentException("titreSejourBE est requis");
        }
        if (today == null) {
            throw new IllegalArgumentException("today est requis");
        }
        if (dateFin == null) {
            throw new IllegalArgumentException("dateFin (protection.temporaire.ukraine.date-fin) est requise");
        }
        // La protection temporaire Ukraine a été activée le 24/02/2022 (date de l'afflux).
        // Une arrivée antérieure ne relève pas de ce dispositif.
        if (dateArrivee.isBefore(DATE_ACTIVATION_PT)) {
            throw new IllegalArgumentException(
                    "dateArrivee antérieure au 24/02/2022 — arrivée avant l'activation de la "
                            + "protection temporaire Ukraine (décision UE 2022/382)");
        }
    }
}
