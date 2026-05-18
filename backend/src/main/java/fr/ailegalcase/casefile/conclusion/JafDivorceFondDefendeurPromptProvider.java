package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-31 — cellule de matrice : conclusions <strong>en défense</strong>
 * au divorce contentieux au fond, côté défendeur, devant le juge aux affaires
 * familiales (JAF), droit de la famille, France.
 *
 * <p>Cellule miroir de la cellule demandeur (SF-98-30) : même structure de
 * conclusions, rôle inversé. La {@code DISCUSSION} prend position sur le cas de
 * divorce invoqué (contestation de la faute, demande reconventionnelle
 * éventuelle) et réfute / contre-propose sur les demandes accessoires.</p>
 */
@Component
public class JafDivorceFondDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / divorce au fond / défendeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur dans une procédure de divorce contentieux au fond \
            devant le juge aux affaires familiales (JAF).
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION, ancrée sur le code civil (art. 233 et suivants) et le code de \
            procédure civile :
              * position sur le cas de divorce invoqué par le demandeur (acceptation, \
            altération définitive du lien conjugal ou faute), contestation de la faute \
            alléguée le cas échéant,
              * demande reconventionnelle en divorce éventuelle (notamment divorce pour \
            faute aux torts du demandeur),
              * réfutation et contre-propositions sur les demandes accessoires : prestation \
            compensatoire, contribution à l'entretien et à l'éducation des enfants, \
            résidence des enfants et droit de visite et d'hébergement, attribution du \
            logement — un paragraphe argumenté par chef de demande,
            - PAR CES MOTIFS (dispositif chiffré, en défense : débouter le demandeur des \
            chefs contestés ; subsidiairement, ramener les sommes réclamées à de plus \
            justes proportions ; le cas échéant, prononcer le divorce sur le fondement \
            reconventionnel retenu).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la \
            stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "DIVORCE_FOND", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
