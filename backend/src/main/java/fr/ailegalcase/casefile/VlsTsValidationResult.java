package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-07 : résultat interne business du calcul du délai de validation du
 * VLS-TS auprès de l'OFII — 3 mois à compter de l'entrée en France
 * (art. R. 311-3 CESEDA).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-01 (checklist de renouvellement du titre post-VLS-TS) ;</li>
 *   <li>F-IM-26 (regroupement familial) et F-IM-27 (VPF liens personnels).</li>
 * </ul>
 *
 * @param joursRestantsValidation jours calendaires restants avant l'échéance
 *        (négatif si dépassé) ; {@code null} lorsque la validation est effectuée.
 * @param procedureRecours texte de recours pour faute de l'administration
 *        (indisponibilité ANEF, etc.) ; {@code null} hors cas EXPIRE non validé.
 */
public record VlsTsValidationResult(
        LocalDate dateEntreeFrance,
        VlsTsValidationTypeEnum typeVlsTs,
        boolean validationOFIIEffectuee,
        LocalDate dateValidationOFII,
        LocalDate dateEcheanceValidation,
        Long joursRestantsValidation,
        VlsTsValidationStatut statut,
        boolean risqueIrregularite,
        String procedureRecours,
        String recommandation,
        String baseJuridique
) {}
