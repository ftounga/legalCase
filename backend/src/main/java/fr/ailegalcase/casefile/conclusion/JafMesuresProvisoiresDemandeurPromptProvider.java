package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-32 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * sur les <strong>mesures provisoires</strong> devant le juge aux affaires familiales
 * (JAF), pendant l'instance en divorce, droit de la famille, France.
 *
 * <p>Le prompt système cible les mesures provisoires ordonnées par le JAF pour la
 * durée de l'instance (art. 254-255 du code civil) et reste stable (cachable). La
 * consigne de style F-98-47 est appliquée par-dessus par
 * {@link CaseConclusionPromptBuilder}.</p>
 */
@Component
public class JafMesuresProvisoiresDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / mesures provisoires / demandeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur devant le juge aux affaires familiales (JAF), dans le cadre
            d'une demande de mesures provisoires pendant l'instance en divorce.
            Rédige un PROJET DE CONCLUSIONS sur les mesures provisoires structuré :
            - en-tête (POUR [demandeur] / CONTRE [défendeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION (un paragraphe argumenté et motivé par mesure provisoire sollicitée),
            - PAR CES MOTIFS (dispositif avec demandes chiffrées).
            Ancre les demandes dans le code civil (art. 254 et 255) : les mesures provisoires que le
            juge peut ordonner pendant l'instance, notamment la résidence séparée des époux, la
            jouissance du logement et du mobilier du ménage, la pension alimentaire au titre du devoir
            de secours, la contribution à l'entretien et à l'éducation des enfants, l'exercice de
            l'autorité parentale, la résidence des enfants et le droit de visite et d'hébergement.
            Précise que ces mesures sont provisoires et qu'elles valent pour la durée de l'instance
            en divorce, jusqu'au prononcé du divorce.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque mesure sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "MESURES_PROVISOIRES", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
