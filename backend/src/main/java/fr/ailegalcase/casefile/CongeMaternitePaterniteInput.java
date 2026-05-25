package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-29 : données d'entrée du calculateur d'analyse du congé maternité /
 * paternité (F-DT-77-conge-paternite-maternite, FRANCE — fondements :
 * L. 1225-1 à L. 1225-34 CT (maternité) ; L. 1225-35 à L. 1225-40 CT
 * (paternité, loi du 16/03/2021 portant le congé paternité à 25 jours
 * calendaires dont 4 obligatoires) ; L. 331-3 CSS (indemnités journalières
 * maternité — 100 % du salaire journalier de référence plafonné) ;
 * L. 1225-4 CT (protection contre le licenciement pendant le congé maternité
 * et dans les 10 semaines suivant le retour) ; L. 1225-4-1 CT (protection
 * paternité, équivalent).
 *
 * <p>L'outil est franco-français — le régime BE (loi du 16/03/1971 et
 * AR du 25/11/1991) est distinct et relève d'un outil jumeau non couvert
 * par cette mini-spec.</p>
 *
 * @param typeConge                   {@link TypeConge#MATERNITE} ou
 *                                    {@link TypeConge#PATERNITE} — détermine
 *                                    le barème durée applicable et le
 *                                    fondement légal de la protection
 * @param rangEnfant                  rang du nouvel enfant à naître / né
 *                                    (1=premier, 2=deuxième, 3=troisième ou
 *                                    plus, valeur ≥ 1). Détermine la durée
 *                                    de congé maternité (16/26 sem)
 * @param naissanceMultiple           true si naissance multiple (jumeaux,
 *                                    triplés et plus). Surcharge le rang
 *                                    pour le congé maternité (34/46 sem) et
 *                                    porte le paternité à 32 jours
 *                                    calendaires (L. 1225-35 al. 1 CT)
 * @param salaireMensuelBrutEuros     salaire mensuel brut en euros (≥ 0)
 *                                    — base du calcul des IJ CPAM
 *                                    (100 % du SJR plafonné — L. 331-3 CSS)
 * @param dateDebutConge              date de début effective du congé
 *                                    (ISO YYYY-MM-DD côté HTTP)
 * @param licenciementPendantConge    true si l'employeur a notifié un
 *                                    licenciement pendant la période
 *                                    protégée (déclenche l'alerte
 *                                    {@code alerteLicenciementIllegal})
 * @param retourPosteDifferent        true si l'employeur a affecté la
 *                                    salariée à un poste différent au
 *                                    retour (déclenche l'alerte
 *                                    {@code alerteRetourPosteIllegal} —
 *                                    L. 1225-25 / L. 1225-27 CT)
 */
public record CongeMaternitePaterniteInput(
        TypeConge typeConge,
        int rangEnfant,
        boolean naissanceMultiple,
        double salaireMensuelBrutEuros,
        LocalDate dateDebutConge,
        boolean licenciementPendantConge,
        boolean retourPosteDifferent
) {

    /** Type de congé — détermine le barème durée et le fondement légal. */
    public enum TypeConge {
        MATERNITE,
        PATERNITE
    }
}
