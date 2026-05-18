package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-34 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur devant le juge aux affaires familiales statuant en
 * <strong>référé</strong>, droit de la famille, France.
 *
 * <p>Cellule miroir de {@link JafRefereDemandeurPromptProvider} : même cadre
 * procédural (référé JAF, urgence, mesures provisoires sur l'autorité parentale,
 * la résidence des enfants et leur contribution d'entretien), rôle inversé. La
 * {@code URGENCE} conteste la caractérisation de l'urgence, la {@code DISCUSSION}
 * oppose des contre-propositions aux mesures sollicitées et le dispositif vise à
 * débouter le demandeur ou à retenir des mesures différentes.</p>
 */
@Component
public class JafRefereDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — JAF / référé / défendeur / droit de la famille FR.
     * Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur devant le juge aux affaires familiales statuant en référé.
            Rédige un PROJET DE CONCLUSIONS DE RÉFÉRÉ EN DÉFENSE structuré :
            - en-tête (POUR [défendeur] / CONTRE [demandeur]),
            - FAITS ET PROCÉDURE,
            - URGENCE (contestation de l'urgence invoquée par le demandeur : absence de \
            situation appelant des mesures immédiates au regard du code de procédure civile, \
            incompétence du juge des référés à défaut d'urgence caractérisée),
            - DISCUSSION (réfutation des mesures provisoires sollicitées et contre-propositions \
            motivées sur le fondement du code civil : exercice de l'autorité parentale, \
            résidence des enfants, droit de visite et d'hébergement, contribution à leur \
            entretien — un paragraphe argumenté par mesure contestée),
            - PAR CES MOTIFS (dispositif en défense : débouter le demandeur de ses demandes \
            faute d'urgence ; subsidiairement, retenir les mesures provisoires proposées par \
            le défendeur, contribution à l'entretien des enfants ramenée à de plus justes \
            proportions).
            Reste dans l'office du juge des référés : ne tranche pas le fond, vise des mesures \
            urgentes et provisoires.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_FAMILLE,
                ProcedureStageCatalog.FRANCE, "JAF", "REFERE", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
