package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * SF-216-15 : calculateur statique de la recevabilité d'une adoption de
 * l'enfant du conjoint (adoption intra-familiale) en droit français
 * (art. 343-1 al. 2 Cciv + art. 345-1 Cciv + art. 360-362 Cciv +
 * Loi n°2022-219 du 21/2/2022).
 *
 * <p>Détermine quelles formes (PLENIERE / SIMPLE) sont juridiquement
 * possibles selon la situation, valide la forme demandée et émet les alertes
 * d'irréversibilité (rupture définitive de la filiation antérieure en
 * plénière, art. 356 Cciv) ainsi que les exigences procédurales (consentement
 * de l'adopté à partir de 13 ans, art. 360 al. 2).</p>
 *
 * <p>Règles clés :</p>
 * <ul>
 *   <li><strong>Pré-requis bloquant</strong> : adoptant marié OU pacsé avec
 *       le parent (art. 345-1 al. 1 — réforme 2022). Sans ce lien, aucune
 *       forme n'est ouverte.</li>
 *   <li><strong>Adoption plénière</strong> (art. 345-1 al. 2) : ouverte
 *       uniquement si l'autre parent biologique est décédé OU déchu OU
 *       consent expressément. Rompt la filiation antérieure — alerte forte
 *       systématique.</li>
 *   <li><strong>Adoption simple</strong> (art. 360-362) : toujours ouverte
 *       dès que les conditions communes sont remplies. Cumulable avec la
 *       filiation d'origine — recommandée si l'autre parent vivant n'est ni
 *       consentant ni déchu.</li>
 *   <li><strong>Différence d'âge</strong> (art. 344) : 10 ans suffisent en
 *       intra-familial (vs 15 ans dans le cas général) — alerte si la marge
 *       n'est pas atteinte.</li>
 *   <li><strong>Consentement de l'adopté</strong> : exigé à partir de 13 ans
 *       (art. 360 al. 2).</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. La matière est régie en Belgique par
 * les art. 343 et suiv. CcB avec des conditions distinctes (outil
 * F-FA-BE-ADOPTION futur, hors périmètre F-216).</p>
 */
public final class AdoptionIntraFrCalculator {

    static final String BASE_JURIDIQUE =
            "art. 343-1 al. 2 Cciv + art. 344 Cciv + art. 345-1 Cciv + art. 356 Cciv"
                    + " + art. 360-362 Cciv + Loi n°2022-219 du 21/2/2022";

    /**
     * Différence d'âge minimale entre l'adoptant et l'adopté en intra-familial
     * (art. 344 al. 1 — dérogation au principe général de 15 ans).
     */
    static final int DIFFERENCE_AGE_MINIMALE_INTRA = 10;

    /** Âge à partir duquel le consentement de l'adopté est requis (art. 360 al. 2). */
    static final int AGE_CONSENTEMENT_ENFANT = 13;

    private AdoptionIntraFrCalculator() {}

    /**
     * Évalue la recevabilité de l'adoption intra-familiale et émet les alertes
     * associées.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static AdoptionIntraFrResult compute(AdoptionIntraFrRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-ADOPTION-INTRA applicable uniquement en France (art. 345-1 Cciv).");
        }

        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        FormeAdoptionEnum formeDemandee = req.formeAdoptionDemandee();
        boolean marieOuPacse = Boolean.TRUE.equals(req.mariageOuPacsAdoptantParent());
        boolean autreParentDecede = Boolean.TRUE.equals(req.autreParentDecede());
        boolean decheance = Boolean.TRUE.equals(req.decheanceAutreParent());
        boolean consentementAutreParent = Boolean.TRUE.equals(req.consentementAutreParentBiologique());
        Boolean consentementEnfantBrut = req.consentementEnfant();
        int ageAdoptant = nz(req.ageAdoptant());
        int ageAdopte = nz(req.ageAdopte());

        // 1. Pré-requis bloquant — lien mariage ou PACS (art. 345-1 al. 1, réforme 2022)
        if (!marieOuPacse) {
            alertes.add("Pré-requis bloquant : l'adoption de l'enfant du conjoint exige un mariage "
                    + "ou un PACS entre l'adoptant et le parent biologique (art. 345-1 al. 1 Cciv, "
                    + "réforme Loi n°2022-219 du 21/2/2022).");
            return new AdoptionIntraFrResult(
                    List.of(),
                    formeDemandee,
                    false,
                    formeDemandee == FormeAdoptionEnum.PLENIERE,
                    ageAdopte >= AGE_CONSENTEMENT_ENFANT,
                    BASE_JURIDIQUE,
                    messages,
                    alertes
            );
        }

        // 2. Détermination des formes ouvertes
        EnumSet<FormeAdoptionEnum> possibles = EnumSet.noneOf(FormeAdoptionEnum.class);

        // Adoption simple toujours ouverte si conditions communes OK (art. 360-362)
        possibles.add(FormeAdoptionEnum.SIMPLE);
        messages.add("Adoption simple ouverte (art. 360-362 Cciv) : crée un lien de filiation "
                + "additionnel avec l'adoptant sans rompre la filiation d'origine de l'enfant — "
                + "voie recommandée si l'autre parent biologique est vivant et n'a pas consenti.");

        // Adoption plénière : conditions cumulatives (art. 345-1 al. 2)
        boolean plenierePossible = autreParentDecede || decheance || consentementAutreParent;
        if (plenierePossible) {
            possibles.add(FormeAdoptionEnum.PLENIERE);
            if (autreParentDecede) {
                messages.add("Adoption plénière ouverte (art. 345-1 al. 2 Cciv) : l'autre parent "
                        + "biologique étant décédé, la rupture de la filiation antérieure ne pose pas "
                        + "de difficulté pratique.");
            } else if (decheance) {
                messages.add("Adoption plénière ouverte (art. 345-1 al. 2 Cciv) : déchéance de "
                        + "l'autorité parentale de l'autre parent biologique. Joindre la décision de "
                        + "déchéance au dossier (jugement définitif).");
            } else {
                messages.add("Adoption plénière ouverte (art. 345-1 al. 2 Cciv) : consentement "
                        + "exprès de l'autre parent biologique. Vérifier que le consentement a été "
                        + "donné devant notaire (forme authentique exigée, art. 348-3 Cciv) et qu'il "
                        + "n'a pas été rétracté dans le délai légal de 2 mois.");
            }
        } else {
            alertes.add("Adoption plénière fermée : l'autre parent biologique est vivant, non "
                    + "déchu et n'a pas consenti. Seule l'adoption simple est ouverte "
                    + "(art. 360-362 Cciv) — sauf à obtenir préalablement la déchéance ou le "
                    + "consentement.");
        }

        // 3. Validation de la forme demandée
        boolean conditionsRemplies = possibles.contains(formeDemandee);
        if (!conditionsRemplies && formeDemandee == FormeAdoptionEnum.PLENIERE) {
            alertes.add("Forme demandée PLÉNIÈRE non recevable en l'état : conditions de "
                    + "l'art. 345-1 al. 2 Cciv non remplies. Orienter le client vers l'adoption "
                    + "simple ou engager au préalable une procédure de déchéance / recueil du "
                    + "consentement de l'autre parent.");
        }

        // 4. Alerte irréversibilité (systématique en plénière, art. 356 Cciv)
        boolean alerteIrreversibilite = formeDemandee == FormeAdoptionEnum.PLENIERE;
        if (alerteIrreversibilite) {
            alertes.add("Irréversibilité de l'adoption plénière : rompt définitivement la "
                    + "filiation avec l'autre parent biologique et toute sa branche familiale "
                    + "(art. 356 Cciv). Devoir de conseil renforcé — formaliser l'information du "
                    + "client par écrit (note d'information + accusé de réception). À privilégier "
                    + "seulement si la rupture de filiation est délibérément voulue.");
        }

        // 5. Consentement de l'adopté (art. 360 al. 2)
        boolean consentementEnfantRequis = ageAdopte >= AGE_CONSENTEMENT_ENFANT;
        if (consentementEnfantRequis) {
            if (Boolean.FALSE.equals(consentementEnfantBrut)) {
                alertes.add("Consentement de l'adopté requis (≥ 13 ans, art. 360 al. 2 Cciv) — "
                        + "l'enfant a refusé. La procédure ne peut aboutir sans son consentement, "
                        + "recueilli devant notaire ou en justice.");
            } else if (consentementEnfantBrut == null) {
                messages.add("Consentement de l'adopté requis (≥ 13 ans, art. 360 al. 2 Cciv). "
                        + "Le recueillir devant notaire (acte authentique) ou en cours d'audience.");
            } else {
                messages.add("Consentement de l'adopté (≥ 13 ans, art. 360 al. 2 Cciv) : présent. "
                        + "Conserver l'acte authentique au dossier.");
            }
        }

        // 6. Différence d'âge (art. 344, dérogation intra-familiale 10 ans)
        if (ageAdoptant > 0 && ageAdopte > 0) {
            int diff = ageAdoptant - ageAdopte;
            if (diff < DIFFERENCE_AGE_MINIMALE_INTRA) {
                alertes.add("Différence d'âge entre adoptant (" + ageAdoptant + " ans) et adopté ("
                        + ageAdopte + " ans) inférieure à " + DIFFERENCE_AGE_MINIMALE_INTRA + " ans — "
                        + "dérogation possible par le tribunal en présence de justes motifs "
                        + "(art. 344 al. 2 Cciv), à motiver dans la requête.");
            }
        }

        // 7. Réforme 2022 — rappel utile
        messages.add("Réforme 2022 (Loi n°2022-219 du 21/2/2022) : extension de l'adoption "
                + "intra-familiale aux couples pacsés, suppression de l'âge minimum de l'adopté, "
                + "différence d'âge minimale réduite à " + DIFFERENCE_AGE_MINIMALE_INTRA
                + " ans en intra-familial.");

        return new AdoptionIntraFrResult(
                new ArrayList<>(possibles),
                formeDemandee,
                conditionsRemplies,
                alerteIrreversibilite,
                consentementEnfantRequis,
                BASE_JURIDIQUE,
                messages,
                alertes
        );
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
