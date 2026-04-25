package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-22-01 : requête d'analyse de requalification CDD → CDI
 * (art. L.1245-1, L.1245-2, L.1242-12, L.1244-3 Code du travail).
 *
 * @param motifCddInvoque       motif officiellement invoqué par l'employeur (enum)
 * @param motifInterdit         vrai si l'avocat a identifié un motif interdit (L.1242-1/3)
 * @param motifInterditType     précision du motif interdit (requise quand motifInterdit=true)
 * @param successionCdd         liste chronologique des CDD considérés (≥ 1)
 * @param delaiCarenceRespecte  vrai si la carence L.1244-3 a été respectée
 * @param dureeContratMois      durée totale cumulée des CDD (en mois, > 0)
 * @param salaireMensuelBrutEur salaire mensuel brut de référence (> 0)
 * @param dateFinDernierContrat date de fin du dernier CDD considéré (sert au rappel L.1471-1)
 */
public record RequalificationCddCdiRequest(
        String motifCddInvoque,
        Boolean motifInterdit,
        String motifInterditType,
        List<CddSuccessionEntry> successionCdd,
        Boolean delaiCarenceRespecte,
        Integer dureeContratMois,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateFinDernierContrat
) {
    /**
     * Une entrée de la succession de CDD.
     *
     * @param dateDebut début du CDD
     * @param dateFin   fin du CDD (≥ dateDebut)
     * @param motif     motif libre tel qu'il apparaît au contrat (texte court)
     */
    public record CddSuccessionEntry(LocalDate dateDebut, LocalDate dateFin, String motif) {}
}
