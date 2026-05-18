package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-12 — cellule de matrice : conclusions <strong>en défense</strong>
 * du défendeur (employeur) devant le <strong>tribunal du travail belge</strong>, au
 * fond, droit du travail, Belgique.
 *
 * <p>Cellule miroir de la cellule TT / FOND / DEMANDEUR côté défense : même
 * structure de conclusions belges, rôle inversé. La {@code DISCUSSION} réfute
 * moyen par moyen les demandes du travailleur et le dispositif vise à les
 * déclarer non fondées.</p>
 *
 * <p>Ancrage strictement belge : procédure du Code judiciaire (art. 740 et s.),
 * droit applicable issu de la loi du 3 juillet 1978 relative aux contrats de
 * travail, de la CCT n° 109 et des conventions collectives de travail. Aucune
 * référence au droit français.</p>
 */
@Component
public class TtFondDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — tribunal du travail belge / fond / défendeur (employeur) /
     * droit du travail BE. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur (employeur) devant le tribunal du travail.
            Rédige un PROJET DE CONCLUSIONS EN DÉFENSE conforme à la procédure belge, \
            ancré dans le Code judiciaire (art. 740 et suivants) et appliquant la loi du \
            3 juillet 1978 relative aux contrats de travail, la CCT n° 109 et les \
            conventions collectives de travail applicables.
            Structure le document ainsi :
            - en-tête (POUR [défendeur / employeur] / CONTRE [demandeur / travailleur]),
            - EXPOSÉ DES FAITS,
            - RECEVABILITÉ ET COMPÉTENCE (compétence du tribunal du travail),
            - DISCUSSION (réfutation moyen par moyen des demandes du travailleur : \
            régularité et motivation du congé, caractère non manifestement déraisonnable \
            du licenciement au regard de la CCT n° 109, contestation du quantum des \
            sommes réclamées — un paragraphe argumenté par moyen),
            - PAR CES MOTIFS (dispositif : plaise au Tribunal du travail de déclarer les \
            demandes du travailleur non fondées et de l'en débouter ; subsidiairement, \
            réduire les sommes réclamées à de plus justes proportions),
            - inventaire des pièces.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            Reprends les montants exacts des calculs fournis — n'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "TT", "FOND", "DEFENDEUR");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
