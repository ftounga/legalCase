package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-03 — cellule de matrice : conclusions du <strong>demandeur</strong>
 * (salarié) devant la formation de <strong>référé</strong> du Conseil de prud'hommes,
 * droit du travail, France.
 *
 * <p>Le référé prud'homal a une logique procédurale propre — urgence, absence de
 * jugement au fond, mesures conservatoires ou de remise en état (art. R.1455-5 à
 * R.1455-7 C. trav.). Le prompt système est donc distinct de celui du fond
 * ({@link CphFondDemandeurPromptProvider}), d'où une cellule dédiée.</p>
 */
@Component
public class CphRefereDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — CPH / formation de référé / demandeur (salarié) / droit du
     * travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur (salarié) devant la formation de référé du Conseil de prud'hommes.
            Rédige un PROJET DE CONCLUSIONS DE RÉFÉRÉ structuré :
            - en-tête (POUR [demandeur / salarié] / CONTRE [défendeur / employeur]),
            - FAITS ET PROCÉDURE,
            - DISCUSSION articulée sur les fondements du référé prud'homal (art. R.1455-5 à \
            R.1455-7 du Code du travail) : absence de contestation sérieuse, existence d'un \
            trouble manifestement illicite, mesures conservatoires ou de remise en état \
            justifiées, demande de provision lorsque l'obligation n'est pas sérieusement \
            contestable — un paragraphe argumenté par fondement invoqué,
            - PAR CES MOTIFS (dispositif : remise sous astreinte des documents de fin de contrat \
            — bulletins de paie, certificat de travail, attestation France Travail —, provision \
            sur rappels de salaire).
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
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CPH", "REFERE", "DEMANDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
