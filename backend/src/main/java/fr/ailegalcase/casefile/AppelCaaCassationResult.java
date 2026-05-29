package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-33 : résultat interne business du calcul des délais d'appel devant la CAA
 * (1 mois de droit commun ou 15 jours en OQTF sans délai) et de cassation devant le
 * Conseil d'État (2 mois, info) en contentieux des étrangers.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> — distinct de :
 * <ul>
 *   <li>F-IM-06 (recours contentieux TA, première instance) ;</li>
 *   <li>F-IM-12 / F-IM-34 (recours CNDA en matière d'asile, ordre juridictionnel distinct).</li>
 * </ul>
 *
 * @param joursRestantsAppel jours calendaires restants avant l'échéance d'appel CAA
 *        (négatif ou nul si dépassé).
 * @param filtrePourvoisCassation true si le pourvoi en cassation au CE serait soumis
 *        au filtre d'admission (art. L. 821-2 CJA) — vrai en matière d'OQTF.
 */
public record AppelCaaCassationResult(
        LocalDate dateJugementTA,
        AppelCaaCassationTypeDecisionEnum typeDecisionTA,
        AppelCaaCassationTypeContentieuxEnum typeContentieux,
        boolean delaiSpecialOQTF,
        LocalDate dateEcheanceAppelCaa,
        long joursRestantsAppel,
        String courAppelCompetente,
        List<String> motifsAppelPossibles,
        boolean filtrePourvoisCassation,
        int delaiCassationCeMois,
        AppelCaaCassationStatut statut,
        String recommandation,
        String baseJuridique
) {}
