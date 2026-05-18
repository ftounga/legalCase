package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-35 — cellule de matrice : requête / conclusions en
 * <strong>ordonnance de protection</strong> devant le juge aux affaires familiales,
 * côté <strong>requérant</strong>, droit de la famille, France.
 *
 * <p>L'ordonnance de protection (art. 515-9 et s. du code civil) répond à un
 * standard probatoire allégé : le juge la délivre s'il existe des raisons
 * sérieuses de considérer comme <em>vraisemblables</em> les faits de violence
 * allégués et le danger — non une preuve certaine. Le prompt système explicite
 * ce standard.</p>
 */
@Component
public class JafOrdonnanceProtectionRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / ordonnance de protection / requérant / droit de la
     * famille FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant devant le juge aux affaires familiales (JAF).
            Rédige un PROJET DE REQUÊTE / CONCLUSIONS EN ORDONNANCE DE PROTECTION, en cas de
            violences exercées au sein du couple ou de la famille.
            Ancrage juridique : code civil, articles 515-9 et suivants — le juge délivre
            l'ordonnance de protection s'il existe des raisons sérieuses de considérer comme
            VRAISEMBLABLES les faits de violence allégués et le danger auquel la victime
            ou les enfants sont exposés.
            Structure le projet ainsi :
            - en-tête (POUR [requérant] / CONTRE [défendeur], saisine du JAF),
            - FAITS (récit chronologique et circonstancié des violences),
            - DISCUSSION : d'abord la vraisemblance des violences et du danger, puis les
              mesures de protection sollicitées (interdiction d'entrer en contact avec le
              requérant, éviction du conjoint violent du domicile, dissimulation de
              l'adresse du requérant, attribution de la jouissance du logement, modalités
              relatives aux enfants),
            - PAR CES MOTIFS (dispositif listant les mesures demandées).
            Souligne le standard probatoire allégé propre à l'ordonnance de protection :
            il s'agit d'établir la VRAISEMBLANCE des violences et du danger, non d'en
            rapporter la preuve certaine.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque demande sur les verdicts des outils décisionnels fournis et sur
            la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "ORDONNANCE_PROTECTION", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
