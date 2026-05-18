package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-40 — cellule de matrice : conclusions <strong>en défense</strong>
 * en succession / partage judiciaire devant le tribunal judiciaire, droit de la
 * famille, France.
 *
 * <p>Cellule miroir de {@link TjSuccessionDemandeurPromptProvider} : même ancrage
 * (livre III du code civil), rôle inversé. La {@code DISCUSSION} réfute et oppose
 * des contre-propositions sur la masse partageable, les rapports et la réduction
 * des libéralités, et l'évaluation des biens.</p>
 */
@Component
public class TjSuccessionDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TJ / succession / partage judiciaire / défendeur / droit de
     * la famille FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur dans une procédure de succession / partage judiciaire \
            devant le tribunal judiciaire.
            Ancrage juridique : code civil, livre III — successions et partage (indivision, \
            rapport et réduction des libéralités, réserve héréditaire, opérations de \
            comptes-liquidation-partage).
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (réfutation moyen par moyen des prétentions du demandeur et \
            contre-propositions : contestation de la composition de la masse partageable, \
            discussion des rapports et de la réduction des libéralités, critique de \
            l'évaluation des biens à partager — un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif en défense : débouter le demandeur de ses prétentions \
            sur la masse partageable et les rapports ; subsidiairement, retenir l'évaluation et \
            les montants des rapports proposés en défense).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "TJ", "SUCCESSION", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
