package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.springframework.stereotype.Component;

/**
 * F-98 / SF-98-10 — cellule de matrice : <strong>mémoire en défense</strong> du
 * <strong>défendeur au pourvoi</strong> devant la chambre sociale de la Cour de
 * cassation, sur pourvoi en cassation, droit du travail, France.
 *
 * <p>Cellule miroir de SF-98-09 côté défense : la {@code DISCUSSION} réfute moyen
 * par moyen et branche par branche le mémoire ampliatif adverse et peut soulever
 * l'irrecevabilité d'un moyen ; le dispositif conclut au rejet du pourvoi. La Cour
 * de cassation juge en droit : ce document de pur droit ne comporte
 * <strong>aucune demande chiffrée</strong> (à la différence des conclusions de
 * fond) — la consigne transverse de reprise des montants est neutralisée ici.</p>
 */
@Component
public class CassSocPourvoiDefendeurPromptProvider implements ConclusionPromptProvider {

    /**
     * Prompt système — Cour de cassation chambre sociale / pourvoi / défendeur au
     * pourvoi / droit du travail FR. Instructions de rédaction stables (cachables).
     */
    static final String SYSTEM_PROMPT = """
            Tu es l'avocat du défendeur au pourvoi devant la chambre sociale de la Cour de cassation.
            Rédige un PROJET DE MÉMOIRE EN DÉFENSE structuré :
            - FAITS ET PROCÉDURE (rappel du litige et de la procédure, en identifiant précisément \
            l'arrêt attaqué par le pourvoi),
            - DISCUSSION : réfutation moyen par moyen et branche par branche du mémoire ampliatif \
            adverse ; pour chaque moyen ou branche, démontre que la décision attaquée est \
            légalement justifiée ; le cas échéant, soulève l'IRRECEVABILITÉ d'un moyen (moyen \
            nouveau, mélangé de fait et de droit, ou manquant en fait),
            - conclusion de la discussion : le pourvoi doit être rejeté,
            - PAR CES MOTIFS (dispositif : rejeter le pourvoi).
            La Cour de cassation juge en droit : ne formule AUCUNE demande chiffrée \
            (pas de quantum, pas de dispositif financier — à la différence des conclusions de fond).
            Cite les pièces par leur numéro (Pièce n° X).
            Appuie chaque réfutation sur les verdicts des outils décisionnels fournis et sur la stratégie retenue.
            N'invente aucun chiffre.
            Style sobre, juridique, en français.
            Ce n'est qu'un projet : il sera relu par l'avocat.
            """;

    @Override
    public CombinationKey combination() {
        return new CombinationKey(ProcedureStageCatalog.DROIT_DU_TRAVAIL,
                ProcedureStageCatalog.FRANCE, "CASS_SOC", "POURVOI", "DEFENDEUR_POURVOI");
    }

    @Override
    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }
}
