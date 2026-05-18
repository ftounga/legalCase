package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-21 — cellule de matrice : <strong>requête d'appel</strong> devant la
 * <strong>cour administrative d'appel</strong>, côté <strong>requérant</strong>
 * (appelant), droit de l'immigration, France.
 *
 * <p>La requête d'appel administratif diffère structurellement d'une requête de
 * 1ʳᵉ instance : la {@code DISCUSSION} critique le jugement attaqué du tribunal
 * administratif et développe les moyens d'annulation / de réformation. La cour
 * administrative d'appel statue à nouveau (effet dévolutif) ; le dispositif vise
 * l'annulation du jugement et le droit aux conclusions de première instance.
 * Ancrage : code de justice administrative.</p>
 */
@Component
public class CaaAppelRequerantPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — cour administrative d'appel / appel / requérant / droit de
     * l'immigration FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du requérant (appelant) devant la cour administrative d'appel.
            Rédige un PROJET DE REQUÊTE D'APPEL dirigée contre un jugement du tribunal administratif, \
            sur le fondement du code de justice administrative, structuré :
            - en-tête (POUR [requérant / appelant] / CONTRE [administration intimée]),
            - EXPOSÉ DES FAITS ET DE LA PROCÉDURE : incluant l'identification précise du jugement \
            attaqué rendu par le tribunal administratif (juridiction, date, numéro, sens du dispositif),
            - RECEVABILITÉ DE L'APPEL : qualité et intérêt à agir de l'appelant, respect du délai d'appel,
            - DISCUSSION : critique du jugement attaqué — un moyen argumenté par chef de critique, \
            énonçant expressément en quoi le tribunal administratif a méconnu le droit ou les faits, \
            puis les moyens d'annulation et de réformation ; rappelle que la cour administrative \
            d'appel, saisie de l'effet dévolutif, statue à nouveau sur le litige,
            - PAR CES MOTIFS : conclure à l'annulation du jugement attaqué et à ce qu'il soit fait \
            droit aux conclusions présentées en première instance.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_IMMIGRATION,
                ProcedureStageCatalog.FRANCE, "CAA", "APPEL", "REQUERANT");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
