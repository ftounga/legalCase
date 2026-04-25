package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-23-01 : requête d'analyse de requalification intérim → CDI
 * (art. L.1251-40, L.1251-41, L.1251-32, L.1251-36 Code du travail).
 *
 * @param motifInterimInvoque         motif officiellement invoqué (enum)
 * @param motifInterdit               vrai si l'avocat a identifié un motif interdit (L.1251-6/L.1251-9)
 * @param motifInterditType           précision du motif interdit (requise quand motifInterdit=true)
 * @param successionMissions          liste chronologique des missions considérées (≥ 1)
 * @param delaiCarenceRespecte        vrai si la carence L.1251-36 a été respectée
 * @param dureeMissionsTotaleMois     durée totale cumulée des missions (en mois, > 0)
 * @param salaireMensuelBrutEur       salaire mensuel brut de référence (> 0)
 * @param dateFinDerniereMission      date de fin de la dernière mission (sert au rappel L.1471-1)
 * @param memeEntrepriseUtilisatrice  vrai si toutes les missions ont eu lieu chez la même entreprise utilisatrice
 */
public record RequalificationInterimCdiRequest(
        String motifInterimInvoque,
        Boolean motifInterdit,
        String motifInterditType,
        List<MissionInterim> successionMissions,
        Boolean delaiCarenceRespecte,
        Integer dureeMissionsTotaleMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDerniereMission,
        Boolean memeEntrepriseUtilisatrice
) {
    /**
     * Une entrée de la succession de missions intérim.
     *
     * @param dateDebut             début de la mission
     * @param dateFin               fin de la mission (≥ dateDebut)
     * @param motif                 motif libre tel qu'il apparaît au contrat (texte court)
     * @param entrepriseUtilisatrice identifiant ou nom de l'entreprise utilisatrice
     */
    public record MissionInterim(
            LocalDate dateDebut,
            LocalDate dateFin,
            String motif,
            String entrepriseUtilisatrice
    ) {}
}
