package fr.ailegalcase.casefile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * SF-215-01 — calculateur d'éligibilité et de délai de renouvellement du permit unique belge.
 *
 * <p>Sources :
 * <ul>
 *   <li>Loi du 30 avril 1999 relative à l'emploi des travailleurs étrangers.</li>
 *   <li>AR du 02 septembre 2018 portant exécution de la Loi du 30/04/1999 — art. 23
 *       (délai de dépôt du renouvellement : 60 jours calendaires avant expiration).
 *       <i>(à vérifier par avocat BE — délai et durées de renouvellement variables selon profil)</i></li>
 * </ul>
 *
 * <p>Le permit unique combine autorisation de travail (compétence régionale —
 * ACTIRIS Bruxelles / VDAB Flandre / FOREM Wallonie) et autorisation de séjour
 * (compétence fédérale — Office des Étrangers). Outil <b>single-country BE</b> —
 * les titres de séjour pour activité salariée français (CESEDA L.421-1 et seq.)
 * relèvent d'une procédure juridiquement distincte.
 */
public final class SinglePermitBeCalculator {

    /** Délai réglementaire de dépôt d'un renouvellement avant expiration (AR 02/09/2018 art. 23). */
    // (à vérifier par avocat BE)
    public static final int DELAI_DEPOT_RENOUVELLEMENT_JOURS = 60;

    /** Seuil de bascule en URGENT (≤ 30 jours avant expiration). */
    public static final int SEUIL_URGENT_JOURS = 30;

    /** Limite d'ancienneté d'un permit expiré encore éligible au traitement (2 ans). */
    public static final int LIMITE_PERMIT_ANCIEN_ANNEES = 2;

    private static final String BASE_JURIDIQUE =
            "Loi du 30/04/1999 relative à l'emploi des travailleurs étrangers ; "
                    + "AR du 02/09/2018 relatif à l'occupation des travailleurs étrangers — "
                    + "art. 23 délai renouvellement 60j";

    private SinglePermitBeCalculator() {
    }

    public static SinglePermitBeResult compute(LocalDate dateDebutPermit,
                                               LocalDate dateFinPermit,
                                               SinglePermitBeRegionEnum regionInstruction,
                                               SinglePermitBeTypeActiviteEnum typeActivite,
                                               SinglePermitBeMotifEnum motifDemande) {
        return compute(dateDebutPermit, dateFinPermit, regionInstruction, typeActivite,
                motifDemande, Clock.system(ZoneId.of("Europe/Brussels")));
    }

    public static SinglePermitBeResult compute(LocalDate dateDebutPermit,
                                               LocalDate dateFinPermit,
                                               SinglePermitBeRegionEnum regionInstruction,
                                               SinglePermitBeTypeActiviteEnum typeActivite,
                                               SinglePermitBeMotifEnum motifDemande,
                                               Clock clock) {
        validateInputs(dateDebutPermit, dateFinPermit, regionInstruction, typeActivite,
                motifDemande, clock);

        LocalDate today = LocalDate.now(clock);
        // (à vérifier par avocat BE) — délai réglementaire AR 02/09/2018 art. 23
        LocalDate dateLimiteDemande = dateFinPermit.minusDays(DELAI_DEPOT_RENOUVELLEMENT_JOURS);
        long joursAvantExpiration = ChronoUnit.DAYS.between(today, dateFinPermit);

        SinglePermitBeStatutRenouvellement statut;
        if (today.isAfter(dateFinPermit)) {
            statut = SinglePermitBeStatutRenouvellement.EXPIRE;
        } else if (joursAvantExpiration <= SEUIL_URGENT_JOURS) {
            statut = SinglePermitBeStatutRenouvellement.URGENT;
        } else if (joursAvantExpiration <= DELAI_DEPOT_RENOUVELLEMENT_JOURS) {
            statut = SinglePermitBeStatutRenouvellement.DANS_DELAI;
        } else {
            statut = SinglePermitBeStatutRenouvellement.DEPOSE_EN_TEMPS;
        }

        String regionCompetente = buildRegionCompetente(regionInstruction);
        List<String> etapesProchaines = buildEtapesProchaines(motifDemande);

        return new SinglePermitBeResult(
                dateDebutPermit,
                dateFinPermit,
                regionInstruction,
                typeActivite,
                motifDemande,
                dateLimiteDemande,
                joursAvantExpiration,
                statut,
                regionCompetente,
                etapesProchaines,
                BASE_JURIDIQUE);
    }

    static String buildRegionCompetente(SinglePermitBeRegionEnum region) {
        return switch (region) {
            case BRUXELLES -> "ACTIRIS (Bruxelles) — travail + Office des Étrangers — séjour";
            case FLANDRE -> "VDAB (Flandre) — travail + Office des Étrangers — séjour";
            case WALLONIE -> "FOREM (Wallonie) — travail + Office des Étrangers — séjour";
        };
    }

    static List<String> buildEtapesProchaines(SinglePermitBeMotifEnum motif) {
        return switch (motif) {
            case NOUVEAU -> List.of(
                    "Constituer dossier complet (contrat, diplômes, antécédents...)",
                    "Dépôt employeur auprès du Conseil régional compétent (ACTIRIS/VDAB/FOREM)",
                    "Décision Conseil régional 30 jours après dépôt complet",
                    "Si favorable : transmission Office des Étrangers pour visa D",
                    "Délivrance permit unique à l'arrivée en Belgique"
            );
            case RENOUVELLEMENT -> List.of(
                    "Vérifier conditions toujours réunies (contrat en cours, employeur autorisé)",
                    "Dépôt 60 jours avant expiration (AR 02/09/2018 art. 23)",
                    "Décision Conseil régional + OE — instruction parallèle",
                    "Si retard > 60j : traitement urgence avec attestation employeur",
                    "Renouvellement délivré pour 1, 2 ou 3 ans selon profil"
            );
        };
    }

    private static void validateInputs(LocalDate dateDebutPermit,
                                       LocalDate dateFinPermit,
                                       SinglePermitBeRegionEnum regionInstruction,
                                       SinglePermitBeTypeActiviteEnum typeActivite,
                                       SinglePermitBeMotifEnum motifDemande,
                                       Clock clock) {
        if (dateDebutPermit == null) {
            throw new IllegalArgumentException("dateDebutPermit est requise");
        }
        if (dateFinPermit == null) {
            throw new IllegalArgumentException("dateFinPermit est requise");
        }
        if (regionInstruction == null) {
            throw new IllegalArgumentException("regionInstruction est requise");
        }
        if (typeActivite == null) {
            throw new IllegalArgumentException("typeActivite est requis");
        }
        if (motifDemande == null) {
            throw new IllegalArgumentException("motifDemande est requis");
        }
        if (dateDebutPermit.isAfter(dateFinPermit)) {
            throw new IllegalArgumentException("Dates incohérentes");
        }
        LocalDate today = LocalDate.now(clock);
        if (dateFinPermit.isBefore(today.minusYears(LIMITE_PERMIT_ANCIEN_ANNEES))) {
            throw new IllegalArgumentException("Permit trop ancien");
        }
    }
}
