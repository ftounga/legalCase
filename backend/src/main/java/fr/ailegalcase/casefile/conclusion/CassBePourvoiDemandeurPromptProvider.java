package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-17 — cellule de matrice : projet de <strong>mémoire à l'appui du
 * pourvoi en cassation</strong> devant la Cour de cassation de Belgique, côté
 * <strong>demandeur au pourvoi</strong>, droit du travail, Belgique.
 *
 * <p>Procédure ancrée dans le droit belge : pourvoi en cassation régi par le Code
 * judiciaire (art. 1073 et s.), décision attaquée rendue par la cour du travail.
 * Document de <em>pur droit</em> : la Cour de cassation juge en droit et ne
 * réapprécie pas les faits. Le prompt neutralise donc la consigne transverse
 * « reprendre les montants des calculs » des cellules de fond — il ne reste que
 * l'interdiction d'inventer un chiffre. La discussion s'articule en moyens de
 * cassation indiquant les dispositions légales violées, et le dispositif tend à
 * la cassation de la décision attaquée.</p>
 */
@Component
public class CassBePourvoiDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Cour de cassation de Belgique / pourvoi en cassation /
     * demandeur au pourvoi / droit du travail BE. Instructions de rédaction
     * stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur en cassation devant la Cour de cassation de Belgique.
            Rédige un PROJET DE MÉMOIRE À L'APPUI DU POURVOI EN CASSATION, en matière de droit \
            du travail, conformément à la procédure du Code judiciaire (art. 1073 et suivants — \
            pourvoi en cassation).
            Structure le mémoire ainsi :
            - EXPOSÉ DES FAITS ET DES ANTÉCÉDENTS DE LA PROCÉDURE : rappel des faits utiles et du \
            déroulement de la procédure, incluant l'identification de la DÉCISION ATTAQUÉE rendue \
            par la cour du travail,
            - MOYEN(S) DE CASSATION : un titre par moyen ; pour chaque moyen, indique les \
            DISPOSITIONS LÉGALES VIOLÉES (articles précis du Code judiciaire, du droit du travail \
            ou de toute autre norme applicable), identifie la partie critiquée de la décision \
            attaquée et expose en quoi celle-ci méconnaît ces dispositions,
            - CONCLUSION : demande à la Cour de CASSER la décision attaquée.
            La Cour de cassation JUGE EN DROIT : n'articule AUCUNE demande chiffrée et ne procède \
            à AUCUNE réappréciation des faits — limite-toi à la critique juridique de la décision \
            attaquée.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "CASS_BE", "POURVOI", "DEMANDEUR_POURVOI");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
