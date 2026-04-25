package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * SF-DT-20-01 : calculateur du rappel de salaire FR
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 *
 * <p>Compose 4 briques de calcul :</p>
 * <ol>
 *   <li>Différentiel mensuel × nombre de mois ⇒ rappel brut hors revalorisation.</li>
 *   <li>Revalorisation INSEE optionnelle (taux saisi par l'avocat).</li>
 *   <li>Prime d'ancienneté CCN (résolue par le service via {@code LegalReferentialService},
 *       passée ici sous forme de pourcentage déjà sélectionné).</li>
 *   <li>Congés payés sur rappel selon 3 méthodes : {@code DIX_POURCENT}, {@code MAINTIEN},
 *       {@code AUCUN}.</li>
 * </ol>
 *
 * <p>Le calculator est <strong>stateless</strong> et ne consulte pas la base : la
 * résolution de la prime CCN est confiée au service applicatif appelant.</p>
 */
public final class RappelSalaireCalculator {

    private static final BigDecimal CENT = new BigDecimal("100");
    private static final BigDecimal DOUZE = new BigDecimal("12");
    private static final BigDecimal DIX_POURCENT = new BigDecimal("0.10");

    private RappelSalaireCalculator() {}

    /**
     * Calcule le rappel de salaire dû.
     *
     * @param periodeDebut                     premier mois de la période (inclus)
     * @param periodeFin                       dernier mois de la période (inclus)
     * @param montantSalaireDuMensuelEur       salaire dû par mois (&gt; 0)
     * @param montantSalairePerVerseMensuelEur salaire effectivement perçu par mois (≥ 0, &lt; dû)
     * @param conventionCollectiveCode         code CCN normalisé (peut être null)
     * @param ancienneteAnneesPrime            ancienneté (années) pour la prime
     * @param primePourcentageCcn              pourcentage de prime ancienneté CCN résolu par le service ;
     *                                         null ou 0 si CCN inconnue / pas de tranche
     * @param indexInseeRevalorise             true si revalorisation INSEE appliquée
     * @param tauxRevalorisationPct            taux INSEE [0,100] ; ignoré si {@code indexInseeRevalorise=false}
     * @param methodeCpSurRappel               méthode CP appliquée
     * @return résultat consolidé
     * @throws IllegalArgumentException si un paramètre est invalide
     */
    public static RappelSalaireResult compute(LocalDate periodeDebut,
                                              LocalDate periodeFin,
                                              BigDecimal montantSalaireDuMensuelEur,
                                              BigDecimal montantSalairePerVerseMensuelEur,
                                              String conventionCollectiveCode,
                                              Integer ancienneteAnneesPrime,
                                              BigDecimal primePourcentageCcn,
                                              boolean indexInseeRevalorise,
                                              BigDecimal tauxRevalorisationPct,
                                              RappelSalaireMethodeCpSurRappel methodeCpSurRappel) {

        // --- Validation ---
        if (periodeDebut == null || periodeFin == null) {
            throw new IllegalArgumentException("Période début et fin requises");
        }
        if (periodeFin.isBefore(periodeDebut)) {
            throw new IllegalArgumentException("Période invalide : fin avant début");
        }
        if (montantSalaireDuMensuelEur == null || montantSalaireDuMensuelEur.signum() <= 0) {
            throw new IllegalArgumentException("Salaire dû mensuel requis et strictement positif");
        }
        if (montantSalairePerVerseMensuelEur == null || montantSalairePerVerseMensuelEur.signum() < 0) {
            throw new IllegalArgumentException("Salaire perçu mensuel requis et positif ou nul");
        }
        if (montantSalaireDuMensuelEur.compareTo(montantSalairePerVerseMensuelEur) <= 0) {
            throw new IllegalArgumentException("Aucun différentiel à réclamer (salaire dû ≤ salaire perçu)");
        }
        if (ancienneteAnneesPrime == null || ancienneteAnneesPrime < 0) {
            throw new IllegalArgumentException("Ancienneté (années) requise et ≥ 0");
        }
        if (indexInseeRevalorise) {
            if (tauxRevalorisationPct == null) {
                throw new IllegalArgumentException("Taux de revalorisation requis quand indexInseeRevalorise=true");
            }
            if (tauxRevalorisationPct.signum() < 0 || tauxRevalorisationPct.compareTo(CENT) > 0) {
                throw new IllegalArgumentException("Taux de revalorisation invalide (attendu dans [0, 100])");
            }
        }
        if (methodeCpSurRappel == null) {
            throw new IllegalArgumentException("Méthode CP sur rappel requise (DIX_POURCENT, MAINTIEN, AUCUN)");
        }

        // --- Calcul nombre de mois ---
        // periodeFin inclus : on ajoute 1 jour pour que Period.between retourne le nombre de mois
        // entiers couvert. Si periodeDebut=2023-01-01 et periodeFin=2024-12-31 → 24 mois.
        Period p = Period.between(periodeDebut, periodeFin.plusDays(1));
        int nbMoisPeriode = p.getYears() * 12 + p.getMonths();
        if (nbMoisPeriode <= 0) {
            // Période < 1 mois entier : on la considère comme 1 mois (pratique majoritaire)
            nbMoisPeriode = 1;
        }

        BigDecimal salaireDu = montantSalaireDuMensuelEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal salaireVerse = montantSalairePerVerseMensuelEur.setScale(2, RoundingMode.HALF_UP);
        BigDecimal differentiel = salaireDu.subtract(salaireVerse).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalRappelBrutHorsRevalo = differentiel
                .multiply(new BigDecimal(nbMoisPeriode))
                .setScale(2, RoundingMode.HALF_UP);

        // --- Revalorisation INSEE ---
        BigDecimal montantRevalorisation = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (indexInseeRevalorise) {
            montantRevalorisation = totalRappelBrutHorsRevalo
                    .multiply(tauxRevalorisationPct)
                    .divide(CENT, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // --- Prime d'ancienneté CCN ---
        BigDecimal primeAnciennete = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (primePourcentageCcn != null && primePourcentageCcn.signum() > 0) {
            primeAnciennete = totalRappelBrutHorsRevalo
                    .multiply(primePourcentageCcn)
                    .divide(CENT, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalRappelBrut = totalRappelBrutHorsRevalo
                .add(montantRevalorisation)
                .add(primeAnciennete)
                .setScale(2, RoundingMode.HALF_UP);

        // --- CP sur rappel ---
        BigDecimal congesPayes = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        switch (methodeCpSurRappel) {
            case DIX_POURCENT -> congesPayes = totalRappelBrut
                    .multiply(DIX_POURCENT)
                    .setScale(2, RoundingMode.HALF_UP);
            case MAINTIEN -> congesPayes = totalRappelBrut
                    .divide(DOUZE, 4, RoundingMode.HALF_UP)
                    .setScale(2, RoundingMode.HALF_UP);
            case AUCUN -> {
                // 0 par défaut
            }
        }

        BigDecimal totalAvecCp = totalRappelBrut.add(congesPayes).setScale(2, RoundingMode.HALF_UP);

        String baseJuridique = buildBaseJuridique(conventionCollectiveCode, primePourcentageCcn,
                indexInseeRevalorise, methodeCpSurRappel);
        String formule = buildFormule(differentiel, nbMoisPeriode, indexInseeRevalorise,
                tauxRevalorisationPct, methodeCpSurRappel, totalAvecCp);
        List<String> messages = buildMessages(conventionCollectiveCode, primePourcentageCcn,
                indexInseeRevalorise, methodeCpSurRappel, nbMoisPeriode);

        return new RappelSalaireResult(
                periodeDebut,
                periodeFin,
                salaireDu,
                salaireVerse,
                conventionCollectiveCode,
                ancienneteAnneesPrime,
                indexInseeRevalorise,
                indexInseeRevalorise ? tauxRevalorisationPct : null,
                methodeCpSurRappel,
                nbMoisPeriode,
                differentiel,
                totalRappelBrutHorsRevalo,
                montantRevalorisation,
                primeAnciennete,
                totalRappelBrut,
                congesPayes,
                totalAvecCp,
                baseJuridique,
                formule,
                messages
        );
    }

    private static String buildBaseJuridique(String ccnCode,
                                             BigDecimal primePctCcn,
                                             boolean indexInsee,
                                             RappelSalaireMethodeCpSurRappel methode) {
        StringBuilder sb = new StringBuilder("Art. L.3242-1");
        if (indexInsee) {
            sb.append(" (revalorisation)");
        }
        if (methode != RappelSalaireMethodeCpSurRappel.AUCUN) {
            sb.append(" + L.3141-26 (CP sur rappel)");
        }
        if (ccnCode != null && !ccnCode.isBlank()
                && primePctCcn != null && primePctCcn.signum() > 0) {
            sb.append(" + CCN ").append(ccnCode).append(" (prime ancienneté)");
        }
        return sb.toString();
    }

    private static String buildFormule(BigDecimal differentiel,
                                       int nbMoisPeriode,
                                       boolean indexInsee,
                                       BigDecimal tauxRevalo,
                                       RappelSalaireMethodeCpSurRappel methode,
                                       BigDecimal totalAvecCp) {
        StringBuilder sb = new StringBuilder("Différentiel ");
        sb.append(formatMontant(differentiel)).append(" €/mois × ").append(nbMoisPeriode).append(" mois");
        if (indexInsee && tauxRevalo != null) {
            sb.append(" + revalorisation ").append(tauxRevalo.toPlainString().replace('.', ',')).append(" %");
        }
        switch (methode) {
            case DIX_POURCENT -> sb.append(" + CP 10 %");
            case MAINTIEN -> sb.append(" + CP maintien (1/12)");
            case AUCUN -> sb.append(" (sans CP sur rappel)");
        }
        sb.append(" = ").append(formatMontant(totalAvecCp)).append(" €");
        return sb.toString();
    }

    private static List<String> buildMessages(String ccnCode,
                                              BigDecimal primePctCcn,
                                              boolean indexInsee,
                                              RappelSalaireMethodeCpSurRappel methode,
                                              int nbMoisPeriode) {
        List<String> msgs = new ArrayList<>();
        msgs.add("Prescription action 3 ans (art. L.3245-1) — vérifier que la période réclamée n'excède pas 3 ans à compter de la date d'exigibilité du salaire.");
        if (indexInsee) {
            msgs.add("Revalorisation INSEE appliquée — joindre la justification du taux retenu (publication INSEE pertinente).");
        } else {
            msgs.add("Aucune revalorisation INSEE appliquée — vérifier si le rappel doit être réévalué (jurisprudence Cass. soc. constante).");
        }
        if (ccnCode == null || ccnCode.isBlank()) {
            msgs.add("Aucune convention collective renseignée — la prime d'ancienneté n'a pas été appliquée. Vérifier la CCN du contrat.");
        } else if (primePctCcn == null || primePctCcn.signum() <= 0) {
            msgs.add("CCN " + ccnCode + " : aucune tranche de prime d'ancienneté applicable pour cette ancienneté (ou CCN inconnue en base).");
        } else {
            msgs.add("Prime d'ancienneté CCN " + ccnCode + " appliquée : "
                    + primePctCcn.toPlainString().replace('.', ',') + " % du rappel brut hors revalorisation.");
        }
        switch (methode) {
            case DIX_POURCENT -> msgs.add("Méthode légale du dixième retenue (art. L.3141-24 I) — pratique majoritaire pour les CP sur rappel.");
            case MAINTIEN -> msgs.add("Méthode du maintien retenue (approximation 1/12 du rappel brut). La méthode du dixième est généralement plus favorable au salarié.");
            case AUCUN -> msgs.add("Aucune ICCP appliquée sur le rappel — n'utiliser ce mode que si l'ICCP a été versée séparément (typiquement CDD).");
        }
        if (nbMoisPeriode > 36) {
            msgs.add("Période > 36 mois : risque de prescription partielle (action limitée aux 3 dernières années — art. L.3245-1).");
        }
        return msgs;
    }

    private static String formatMontant(BigDecimal m) {
        return m.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
