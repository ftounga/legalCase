package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-41 : résultat interne business de l'analyse de validité d'un retrait de
 * titre de séjour pour fraude (art. L. 412-7 CESEDA). Vérifie le respect du
 * contradictoire préalable obligatoire, identifie les vices de procédure et les
 * moyens de contestation au fond selon le motif, et calcule le délai de recours
 * devant le tribunal administratif (2 mois).
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 *
 * @param dateRetrait date de la décision de retrait.
 * @param motifRetrait motif invoqué par l'administration.
 * @param miseEnDemeurePrealable true si un contradictoire préalable a été conduit.
 * @param dateMiseEnDemeure date de la mise en demeure (peut être null).
 * @param vicesDeProcedure vices de procédure identifiés (peut être vide).
 * @param motifsContestation moyens de contestation au fond selon le motif.
 * @param delaiRecoursTA date limite de saisine du TA (dateRetrait + 2 mois).
 * @param statut statut du recours au regard du délai.
 * @param recoursPossible true tant que le délai n'est pas expiré.
 * @param recommandation synthèse opérationnelle.
 * @param baseJuridique fondement juridique de l'analyse.
 */
public record RetraitTitreFraudeResult(
        LocalDate dateRetrait,
        RetraitTitreFraudeMotifEnum motifRetrait,
        boolean miseEnDemeurePrealable,
        LocalDate dateMiseEnDemeure,
        List<String> vicesDeProcedure,
        List<String> motifsContestation,
        LocalDate delaiRecoursTA,
        RetraitTitreFraudeStatut statut,
        boolean recoursPossible,
        String recommandation,
        String baseJuridique
) {}
