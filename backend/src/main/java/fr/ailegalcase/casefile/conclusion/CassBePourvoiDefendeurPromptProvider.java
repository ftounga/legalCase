package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-17 — cellule de matrice : projet de <strong>mémoire en réponse</strong>
 * devant la Cour de cassation de Belgique, côté <strong>défendeur au pourvoi</strong>,
 * droit du travail, Belgique.
 *
 * <p>Cellule miroir de {@link CassBePourvoiDemandeurPromptProvider} : pourvoi en
 * cassation régi par le Code judiciaire (art. 1073 et s.), décision attaquée rendue
 * par la cour du travail. Document de <em>pur droit</em> : la Cour de cassation juge
 * en droit et ne réapprécie pas les faits — pas de demandes chiffrées. La discussion
 * réfute moyen par moyen et peut opposer l'irrecevabilité d'un moyen ; le dispositif
 * tend au rejet du pourvoi.</p>
 */
@Component
public class CassBePourvoiDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Cour de cassation de Belgique / pourvoi en cassation /
     * défendeur au pourvoi / droit du travail BE. Instructions de rédaction
     * stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur en cassation devant la Cour de cassation de Belgique.
            Rédige un PROJET DE MÉMOIRE EN RÉPONSE, en matière de droit du travail, conformément \
            à la procédure du Code judiciaire (art. 1073 et suivants — pourvoi en cassation).
            Structure le mémoire en réponse ainsi :
            - EXPOSÉ DES FAITS ET DES ANTÉCÉDENTS DE LA PROCÉDURE : rappel des faits utiles et du \
            déroulement de la procédure, incluant l'identification de la DÉCISION ATTAQUÉE rendue \
            par la cour du travail,
            - DISCUSSION : réfute MOYEN PAR MOYEN les moyens de cassation invoqués par le demandeur ; \
            pour chaque moyen, examine d'abord sa RECEVABILITÉ et oppose, lorsqu'il y a lieu, son \
            IRRECEVABILITÉ (moyen nouveau, moyen imprécis, moyen mélangé de fait et de droit), puis, \
            à titre subsidiaire, démontre que la décision attaquée n'a violé aucune des dispositions \
            légales invoquées,
            - CONCLUSION : demande à la Cour le REJET DU POURVOI.
            La Cour de cassation JUGE EN DROIT : n'articule AUCUNE demande chiffrée et ne procède \
            à AUCUNE réappréciation des faits — limite-toi à la critique juridique des moyens.
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque moyen sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.BELGIQUE, "CASS_BE", "POURVOI", "DEFENDEUR_POURVOI");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
