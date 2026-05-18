package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-39 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur en matière de <strong>filiation</strong> devant le tribunal
 * judiciaire, droit de la famille, France.
 *
 * <p>Cellule miroir de {@link TjFiliationDemandeurPromptProvider} : même cadre
 * procédural (titre VII du livre I<sup>er</sup> du code civil), rôle inversé. La
 * {@code DISCUSSION} conteste la recevabilité de l'action (prescription, qualité à
 * agir), réfute les éléments de preuve adverses et prend position sur l'expertise
 * sollicitée.</p>
 */
@Component
public class TjFiliationDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — TJ / filiation / défendeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur en matière de filiation devant le tribunal judiciaire.
            L'action adverse vise l'établissement ou la contestation d'un lien de filiation \
            (titre VII du livre Iᵉʳ du code civil — de la filiation).
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE structuré :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION : contestation de la recevabilité de l'action (prescription de l'action \
            en filiation, défaut de qualité ou d'intérêt à agir du demandeur), réfutation des \
            modes de preuve invoqués (présomptions, possession d'état, indices de fait) et \
            position sur la demande d'expertise biologique / génétique ; un paragraphe argumenté \
            par moyen,
            - PAR CES MOTIFS (dispositif : déclarer l'action irrecevable ou, à défaut, débouter \
            le demandeur de l'ensemble de ses demandes).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "TJ", "FILIATION", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
