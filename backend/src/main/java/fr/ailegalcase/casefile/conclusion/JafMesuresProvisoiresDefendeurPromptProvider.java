package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-33 — cellule de matrice : conclusions <strong>en défense</strong>
 * sur les mesures provisoires devant le juge aux affaires familiales, côté
 * défendeur, droit de la famille, France.
 *
 * <p>Cellule miroir de {@code JafMesuresProvisoiresDemandeurPromptProvider}
 * (SF-98-32) : même structure de conclusions, rôle inversé. La {@code DISCUSSION}
 * réfute et formule des contre-propositions mesure par mesure (résidence des
 * enfants, contribution à l'entretien et à l'éducation, pension au titre du
 * devoir de secours, jouissance du logement, droit de visite et d'hébergement)
 * et le dispositif vise à écarter les demandes adverses. Ancrage : code civil
 * (art. 254-255).</p>
 */
@Component
public class JafMesuresProvisoiresDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / mesures provisoires / défendeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur devant le juge aux affaires familiales, sur les mesures provisoires.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (réfutation des demandes adverses et contre-propositions mesure par mesure, \
            au visa des articles 254 et 255 du code civil : résidence des enfants, contribution à \
            l'entretien et à l'éducation, pension au titre du devoir de secours, jouissance du \
            logement, droit de visite et d'hébergement — un paragraphe argumenté par mesure),
            - PAR CES MOTIFS (dispositif chiffré, en défense : écarter les demandes adverses ; \
            subsidiairement, fixer les mesures provisoires à de plus justes proportions).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque mesure sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "MESURES_PROVISOIRES", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
