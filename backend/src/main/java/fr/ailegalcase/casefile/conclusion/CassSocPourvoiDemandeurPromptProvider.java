package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-09 — cellule de matrice : projet de <strong>mémoire ampliatif</strong>
 * devant la chambre sociale de la Cour de cassation, côté <strong>demandeur au
 * pourvoi</strong>, droit du travail, France.
 *
 * <p>Document de <em>pur droit</em> : la Cour de cassation juge en droit et ne
 * réapprécie pas les faits. Le prompt neutralise donc la consigne transverse
 * « reprendre les montants des calculs » des cellules de fond — il ne reste que
 * l'interdiction d'inventer un chiffre. La discussion s'articule en moyens de
 * cassation décomposés en branches, chacune visant un cas d'ouverture à
 * cassation, et le dispositif tend à casser et annuler l'arrêt attaqué.</p>
 */
@Component
public class CassSocPourvoiDemandeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Cour de cassation chambre sociale / pourvoi / demandeur au
     * pourvoi / droit du travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du demandeur au pourvoi devant la chambre sociale de la Cour de cassation.
            Rédige un PROJET DE MÉMOIRE AMPLIATIF structuré :
            - en-tête (POUR [demandeur au pourvoi] / CONTRE [défendeur au pourvoi]),
            - FAITS ET PROCÉDURE (rappel des faits utiles et de la procédure, incluant l'identification \
            de l'ARRÊT ATTAQUÉ rendu par la cour d'appel),
            - MOYEN(S) DE CASSATION : un titre par moyen, chaque moyen articulé en BRANCHES ; pour chaque \
            branche, énonce le CAS D'OUVERTURE À CASSATION invoqué (violation de la loi, manque de base \
            légale, défaut de motifs, contradiction de motifs, dénaturation), identifie précisément la \
            partie critiquée de l'arrêt attaqué et expose le grief de droit,
            - PAR CES MOTIFS (dispositif : CASSER ET ANNULER l'arrêt attaqué et renvoyer l'affaire \
            devant une cour d'appel autrement composée).
            La Cour de cassation JUGE EN DROIT : n'articule AUCUNE demande chiffrée et ne procède à \
            AUCUNE réappréciation des faits ni du quantum — limite-toi à la critique juridique de l'arrêt.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CASS_SOC", "POURVOI", "DEMANDEUR_POURVOI");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
