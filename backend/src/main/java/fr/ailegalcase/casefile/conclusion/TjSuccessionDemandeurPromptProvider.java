package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-40 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * en succession / partage judiciaire devant le tribunal judiciaire, droit de la
 * famille, France.
 *
 * <p>La cellule cible la sortie d'indivision et l'ouverture des opérations de
 * comptes-liquidation-partage. L'ancrage juridique est le livre III du code civil
 * (successions et partage : indivision, rapport et réduction des libéralités,
 * réserve héréditaire).</p>
 */
@Component
public class TjSuccessionDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TJ / succession / partage judiciaire / demandeur / droit de
     * la famille FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur dans une procédure de succession / partage judiciaire \
            devant le tribunal judiciaire.
            Ancrage juridique : code civil, livre III — successions et partage (indivision, \
            rapport et réduction des libéralités, réserve héréditaire, ouverture des opérations \
            de comptes-liquidation-partage).
            Rédige un PROJET DE CONCLUSIONS structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur, co-indivisaires / cohéritiers]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (un paragraphe argumenté par moyen : demande d'ouverture des opérations \
            de partage et de sortie d'indivision, désignation d'un notaire chargé des opérations \
            de comptes-liquidation-partage, contestations sur la composition de la masse \
            partageable, les rapports et la réduction des libéralités, la réserve héréditaire),
            - PAR CES MOTIFS (dispositif avec demandes chiffrées : ouverture des opérations de \
            partage, désignation du notaire, montants des rapports et soultes).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "TJ", "SUCCESSION", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
