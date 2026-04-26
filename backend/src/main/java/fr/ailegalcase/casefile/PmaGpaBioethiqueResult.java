package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-27-01 : résultat d'une analyse PMA / GPA / bioéthique (loi 2/8/2021 + Cass. 18/12/2022).
 *
 * @param dispositif             code dispositif (PMA_RECONNAISSANCE_ANTICIPEE / GPA_TRANSCRIPTION_ETAT_CIVIL / DON_GAMETES_ACCES_ORIGINES)
 * @param verdictRecevabilite    ELEVEE / MOYENNE / FAIBLE
 * @param proceduresAFollow      liste ordonnée des étapes procédurales à mener
 * @param risqueRefus            FAIBLE / MOYEN / ELEVE
 * @param documentsRequis        pièces canoniques selon dispositif
 * @param delaiInstructionMois   délai indicatif (PMA ≈ 1 / GPA ≈ 6-12 / accès donneur ≈ 6)
 * @param baseJuridique          articles Code civil + jurisprudence applicables
 * @param formule                résumé en une ligne
 * @param messages               conseils pratiques
 */
public record PmaGpaBioethiqueResult(
        String dispositif,
        String verdictRecevabilite,
        List<String> proceduresAFollow,
        String risqueRefus,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
