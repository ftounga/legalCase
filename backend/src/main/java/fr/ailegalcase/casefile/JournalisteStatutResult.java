package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-218-15 : résultat interne business de l'analyse du statut de journaliste
 * professionnel lors d'une rupture (clause de cession / conscience, indemnité de
 * congédiement, commission arbitrale). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * @param dateEntree début du contrat.
 * @param dateRupture date de notification de la rupture.
 * @param typeRupture type de rupture retenu.
 * @param salaireMensuelMoyen base de l'indemnité de congédiement (€).
 * @param carteIdentiteProfessionnelle détention de la carte de presse.
 * @param ancienneteAnnees ancienneté en années pleines/entamées (toute fraction
 *        comptant pour une année entière — art. L.7112-3).
 * @param statutJournaliste qualification du statut (CONFIRME / A_QUALIFIER).
 * @param clauseValide validité de la clause invoquée (VALIDE / NON_VALIDE /
 *        SANS_OBJET).
 * @param motifClause motif associé à la validité de la clause (null si VALIDE).
 * @param indemniteCongediement montant de l'indemnité de congédiement (€,
 *        plafonné à 15 mensualités hors commission arbitrale).
 * @param commissionArbitraleRequise true si la fixation de l'indemnité relève de
 *        la commission arbitrale paritaire (ancienneté > 15 ans ou faute grave).
 * @param noteCommissionArbitrale note explicative (null si non requise).
 * @param verdictGlobal verdict global de la rupture.
 * @param baseJuridique fondements juridiques applicables.
 */
public record JournalisteStatutResult(
        LocalDate dateEntree,
        LocalDate dateRupture,
        JournalisteStatutTypeRupture typeRupture,
        BigDecimal salaireMensuelMoyen,
        boolean carteIdentiteProfessionnelle,
        int ancienneteAnnees,
        JournalisteStatutQualification statutJournaliste,
        JournalisteStatutClauseValidite clauseValide,
        String motifClause,
        BigDecimal indemniteCongediement,
        boolean commissionArbitraleRequise,
        String noteCommissionArbitrale,
        JournalisteStatutVerdict verdictGlobal,
        String baseJuridique
) {}
