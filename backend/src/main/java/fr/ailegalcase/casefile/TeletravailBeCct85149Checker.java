package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-219-16 : vérificateur de conformité d'un accord de télétravail BE
 * (CCT n° 85 du 09/11/2005 du CNT — structurel ; CCT n° 149 du
 * 26/01/2021 du CNT — occasionnel / COVID) — fonction <b>pure</b>
 * (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>CCT n° 85 du 09/11/2005</b> conclue au Conseil National du
 *       Travail concernant le <i>télétravail</i> — rendue obligatoire
 *       par AR du 13/06/2006 (M.B. 05/09/2006). Encadre le télétravail
 *       <b>régulier et structurel</b> : volontariat (art. 5), convention
 *       individuelle écrite (art. 6), équipement fourni/indemnisé par
 *       l'employeur (art. 9), droits sociaux maintenus (art. 4).</li>
 *   <li><b>CCT n° 149 du 26/01/2021</b> conclue au CNT relative au
 *       télétravail recommandé ou obligatoire en raison de la crise
 *       du coronavirus — devenue cadre de référence du télétravail
 *       <b>occasionnel</b> (force majeure / raison personnelle).</li>
 *   <li><b>Loi du 03/07/1978</b> sur les contrats de travail —
 *       continuité du contrat, art. 17 (obligations du travailleur).</li>
 *   <li><b>Loi du 04/08/1996</b> relative au bien-être des travailleurs
 *       + <b>Code du bien-être au travail, Livre VIII / Titre 1</b>
 *       (postes de travail à écran) — application au domicile.</li>
 *   <li><b>Loi du 26/03/2018</b> relative au renforcement de la
 *       croissance économique et de la cohésion sociale, art. 16-18
 *       (droit à la déconnexion — concertation collective).</li>
 *   <li><b>Loi du 03/10/2022</b> (M.B. 10/11/2022) portant des
 *       dispositions diverses en matière de travail (« Deal pour
 *       l'emploi ») — obligation pour les entreprises ≥ 20
 *       travailleurs de définir des modalités de déconnexion par
 *       CCT d'entreprise ou règlement de travail (entrée en vigueur
 *       01/04/2023).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — type {@link
 *       TeletravailBeCct85149TypeTeletravail#INDETERMINE} → analyse
 *       au cas par cas (sortie immédiate).</li>
 *   <li>(Structurel uniquement) <b>NON_CONFORME_CONVENTION_ECRITE_MANQUANTE</b>
 *       — pas de convention individuelle écrite (art. 6 CCT n° 85).</li>
 *   <li>(Structurel uniquement) <b>NON_CONFORME_EQUIPEMENT_NON_FOURNI</b>
 *       — équipement non fourni / non indemnisé (art. 9 CCT n° 85).</li>
 *   <li><b>NON_CONFORME_DROITS_REDUITS</b> — droits sociaux non
 *       maintenus (art. 4 CCT n° 85, transposable CCT n° 149).</li>
 *   <li><b>FRAGILE_DECONNEXION_NON_DEFINIE</b> — entreprise &gt;= 20
 *       travailleurs sans modalités de déconnexion (Loi 03/10/2022).</li>
 *   <li>Sinon : <b>CONFORME_CCT_85_STRUCTUREL</b> ou
 *       <b>CONFORME_CCT_149_OCCASIONNEL</b> selon le type.</li>
 * </ol>
 *
 * <h2>Indemnité forfaitaire (information complémentaire)</h2>
 * <p>L'écart d'indemnité ({@code indemniteExcedentaire}) est calculé
 * en {@code max(0, indemnite - plafond)} : un excédent positif signale
 * une probable requalification en rémunération imposable / soumise à
 * cotisations ONSS — non bloquant en V1, simple alerte côté UI.</p>
 */
public final class TeletravailBeCct85149Checker {

    static final String BASE_JURIDIQUE =
            "CCT n° 85 du 09/11/2005 conclue au CNT concernant le"
                    + " télétravail (M.B. 05/09/2006), art. 4 (égalité"
                    + " des droits), art. 5 (volontariat réciproque),"
                    + " art. 6 (convention individuelle écrite), art. 9"
                    + " (équipement fourni / indemnisé par l'employeur) ;"
                    + " CCT n° 149 du 26/01/2021 du CNT relative au"
                    + " télétravail recommandé ou obligatoire en raison"
                    + " de la crise du coronavirus (devenue cadre du"
                    + " télétravail occasionnel / force majeure) ; Loi"
                    + " du 03/07/1978 sur les contrats de travail, art."
                    + " 17 (obligations du travailleur) ; Loi du"
                    + " 04/08/1996 relative au bien-être des travailleurs"
                    + " + Code du bien-être au travail, Livre VIII /"
                    + " Titre 1 (postes à écran applicables au domicile) ;"
                    + " Loi du 26/03/2018, art. 16-18 (droit à la"
                    + " déconnexion — concertation collective) ; Loi du"
                    + " 03/10/2022 (M.B. 10/11/2022), art. 16-17 (« Deal"
                    + " pour l'emploi » — modalités de déconnexion par"
                    + " CCT / règlement de travail obligatoires pour les"
                    + " entreprises ≥ 20 travailleurs, e.e.v. 01/04/2023).";

    static final String AVERT_AVOCAT_BE =
            "Les plafonds exacts d'indemnité forfaitaire (frais internet,"
                    + " électricité, chauffage, ergonomie — circulaires"
                    + " ONSS et SPF Finances révisées annuellement) ainsi"
                    + " que l'application précise du droit à la"
                    + " déconnexion (forme de la CCT d'entreprise vs"
                    + " règlement de travail, contenu minimum, organe"
                    + " compétent CE/CPPT, sanctions SPF ETCS) doivent"
                    + " être confirmés par un avocat spécialisé en droit"
                    + " social BE pour la date d'occupation considérée"
                    + " et la commission paritaire applicable. En cas"
                    + " de télétravail structurel pratiqué sans"
                    + " convention individuelle écrite (art. 6 CCT n° 85),"
                    + " le télétravailleur peut exiger la régularisation,"
                    + " réclamer la prise en charge intégrale des frais"
                    + " professionnels exposés et, en cas de litige,"
                    + " invoquer la nullité des effets défavorables de"
                    + " la formule unilatérale. La fourniture d'un"
                    + " équipement adéquat (art. 9 CCT n° 85) est une"
                    + " obligation à charge exclusive de l'employeur ;"
                    + " un excédent d'indemnité au-delà du plafond ONSS"
                    + " admis est requalifié en rémunération imposable"
                    + " et soumise à cotisations.";

    private TeletravailBeCct85149Checker() {
    }

    public static TeletravailBeCct85149Result analyze(
            TeletravailBeCct85149Request request) {
        validateInputs(request);

        boolean isStructurel = request.typeTeletravail()
                == TeletravailBeCct85149TypeTeletravail.STRUCTUREL_CCT_85;
        boolean isOccasionnel = request.typeTeletravail()
                == TeletravailBeCct85149TypeTeletravail.OCCASIONNEL_CCT_149;
        boolean isIndetermine = request.typeTeletravail()
                == TeletravailBeCct85149TypeTeletravail.INDETERMINE;

        // Calculs dérivés communs
        boolean conventionRespectee = !isStructurel
                || request.conventionEcriteIndividuelle();
        boolean equipementRespecte = !isStructurel
                || request.equipementFourniOuIndemnise();
        boolean droitsRespectes = request.droitsSociauxMaintenus();
        boolean deconnexionExigible = request.effectifEntreprise() >= 20;
        boolean deconnexionRespectee = !deconnexionExigible
                || request.deconnexionDefinie();
        BigDecimal indemniteExcedentaire = request
                .indemniteForfaitaireMensuelleEuros()
                .subtract(request.plafondIndemniteMensuelleEuros())
                .max(BigDecimal.ZERO);

        // ── Hiérarchie 1 : type indéterminé ───────────────────────────────────
        if (isIndetermine) {
            String synthese = "Type de télétravail indéterminé : la"
                    + " qualification (structurel CCT n° 85 vs occasionnel"
                    + " CCT n° 149) n'est pas encore opérée ou fait l'objet"
                    + " d'un litige. Une analyse au cas par cas est requise"
                    + " pour déterminer la convention applicable, le"
                    + " formalisme et les obligations financières.";
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.A_ANALYSER,
                    conventionRespectee, equipementRespecte, droitsRespectes,
                    deconnexionRespectee, indemniteExcedentaire,
                    "TYPE_TELETRAVAIL_INDETERMINE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 2 : convention écrite manquante (structurel) ───────────
        if (isStructurel && !request.conventionEcriteIndividuelle()) {
            String synthese = "Télétravail structurel pratiqué sans"
                    + " convention individuelle écrite (art. 6 CCT n° 85"
                    + " du 09/11/2005). L'écrit doit reprendre la"
                    + " fréquence, les jours télétravaillés, le lieu"
                    + " d'exécution, l'équipement fourni et l'indemnité."
                    + " Présomption d'irrégularité : le travailleur peut"
                    + " exiger la régularisation, réclamer la prise en"
                    + " charge intégrale des frais exposés et, en cas"
                    + " de litige, invoquer la nullité des effets"
                    + " défavorables de la formule unilatérale.";
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.NON_CONFORME_CONVENTION_ECRITE_MANQUANTE,
                    false, equipementRespecte, droitsRespectes,
                    deconnexionRespectee, indemniteExcedentaire,
                    "CONVENTION_ECRITE_MANQUANTE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 3 : équipement non fourni / non indemnisé (structurel) ─
        if (isStructurel && !request.equipementFourniOuIndemnise()) {
            String synthese = "Télétravail structurel : l'employeur ne"
                    + " fournit pas l'équipement nécessaire (ordinateur,"
                    + " accès, support technique) et n'indemnise pas"
                    + " le travailleur qui utilise son propre matériel —"
                    + " manquement à l'art. 9 CCT n° 85. Le télétravailleur"
                    + " peut réclamer le remboursement intégral des frais"
                    + " professionnels exposés (matériel, internet,"
                    + " électricité, ergonomie).";
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.NON_CONFORME_EQUIPEMENT_NON_FOURNI,
                    true, false, droitsRespectes, deconnexionRespectee,
                    indemniteExcedentaire,
                    "EQUIPEMENT_NON_FOURNI",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 4 : droits réduits (structurel + occasionnel) ──────────
        if (!droitsRespectes) {
            String synthese = "Le télétravailleur ne bénéficie pas des"
                    + " mêmes droits (formation, carrière, accès collectif,"
                    + " protection sociale) que les travailleurs"
                    + " présentiels — discrimination contraire à l'art."
                    + " 4 CCT n° 85 (transposable au télétravail"
                    + " occasionnel CCT n° 149). Égalité de traitement"
                    + " exigible immédiatement, recours individuel +"
                    + " action de la délégation syndicale possibles.";
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.NON_CONFORME_DROITS_REDUITS,
                    conventionRespectee, equipementRespecte, false,
                    deconnexionRespectee, indemniteExcedentaire,
                    "DROITS_SOCIAUX_REDUITS",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Hiérarchie 5 : déconnexion non définie (≥ 20 travailleurs) ────────
        if (deconnexionExigible && !request.deconnexionDefinie()) {
            String synthese = "Entreprise de " + request.effectifEntreprise()
                    + " travailleur(s) : la Loi du 03/10/2022 (« Deal pour"
                    + " l'emploi », e.e.v. 01/04/2023) impose la définition"
                    + " collective des modalités de déconnexion (CCT"
                    + " d'entreprise ou règlement de travail). En l'absence"
                    + " d'accord collectif / de règlement adapté, sanction"
                    + " SPF ETCS possible et exposition contentieuse en"
                    + " cas de litige sur les heures supplémentaires"
                    + " connectées hors temps de travail.";
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.FRAGILE_DECONNEXION_NON_DEFINIE,
                    conventionRespectee, equipementRespecte, true, false,
                    indemniteExcedentaire,
                    "DECONNEXION_NON_DEFINIE",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas qualifiant : conforme (structurel CCT 85) ─────────────────────
        if (isStructurel) {
            String synthese = "Télétravail structurel pleinement conforme"
                    + " à la CCT n° 85 du 09/11/2005 : volontariat"
                    + " réciproque, convention individuelle écrite,"
                    + " équipement fourni ou indemnisé, droits sociaux"
                    + " maintenus, modalités de déconnexion définies"
                    + " (ou effectif < 20 hors champ Loi 03/10/2022)."
                    + (indemniteExcedentaire.compareTo(BigDecimal.ZERO) > 0
                            ? " ATTENTION : indemnité mensuelle de "
                                    + request.indemniteForfaitaireMensuelleEuros()
                                    + " € dépasse le plafond admis ("
                                    + request.plafondIndemniteMensuelleEuros()
                                    + " €) de " + indemniteExcedentaire
                                    + " € — excédent imposable / soumis"
                                    + " à cotisations ONSS."
                            : "");
            return new TeletravailBeCct85149Result(
                    request.dateDebutTeletravail(),
                    request.typeTeletravail(),
                    request.volontariatReciproque(),
                    request.conventionEcriteIndividuelle(),
                    request.equipementFourniOuIndemnise(),
                    request.indemniteForfaitaireMensuelleEuros(),
                    request.plafondIndemniteMensuelleEuros(),
                    request.droitsSociauxMaintenus(),
                    request.deconnexionDefinie(),
                    request.effectifEntreprise(),
                    TeletravailBeCct85149Verdict.CONFORME_CCT_85_STRUCTUREL,
                    true, true, true, true,
                    indemniteExcedentaire,
                    "CONFORME_STRUCTUREL",
                    synthese,
                    BASE_JURIDIQUE,
                    AVERT_AVOCAT_BE);
        }

        // ── Cas qualifiant : conforme (occasionnel CCT 149) ───────────────────
        // isOccasionnel == true (les autres types ont été traités)
        String synthese = "Télétravail occasionnel relevant de la CCT n° 149"
                + " du 26/01/2021 (force majeure, raison personnelle,"
                + " COVID) — cadre minimal respecté : droits sociaux"
                + " maintenus, modalités de déconnexion définies si"
                + " effectif ≥ 20. Pas de convention écrite individuelle"
                + " obligatoire au cas par cas. Indemnité de "
                + request.indemniteForfaitaireMensuelleEuros() + " € /"
                + " mois (plafond admis : "
                + request.plafondIndemniteMensuelleEuros() + " €).";
        // (silence sur isOccasionnel pour éviter le warning compilateur)
        if (!isOccasionnel) {
            // Garde-fou : ce chemin ne doit pas être atteint
            throw new IllegalStateException("typeTeletravail inattendu : "
                    + request.typeTeletravail());
        }
        return new TeletravailBeCct85149Result(
                request.dateDebutTeletravail(),
                request.typeTeletravail(),
                request.volontariatReciproque(),
                request.conventionEcriteIndividuelle(),
                request.equipementFourniOuIndemnise(),
                request.indemniteForfaitaireMensuelleEuros(),
                request.plafondIndemniteMensuelleEuros(),
                request.droitsSociauxMaintenus(),
                request.deconnexionDefinie(),
                request.effectifEntreprise(),
                TeletravailBeCct85149Verdict.CONFORME_CCT_149_OCCASIONNEL,
                true, true, true, true,
                indemniteExcedentaire,
                "CONFORME_OCCASIONNEL",
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(TeletravailBeCct85149Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.dateDebutTeletravail() == null) {
            throw new IllegalArgumentException(
                    "dateDebutTeletravail est requise");
        }
        if (request.typeTeletravail() == null) {
            throw new IllegalArgumentException(
                    "typeTeletravail est requis");
        }
        if (request.volontariatReciproque() == null) {
            throw new IllegalArgumentException(
                    "volontariatReciproque est requis");
        }
        if (request.conventionEcriteIndividuelle() == null) {
            throw new IllegalArgumentException(
                    "conventionEcriteIndividuelle est requise");
        }
        if (request.equipementFourniOuIndemnise() == null) {
            throw new IllegalArgumentException(
                    "equipementFourniOuIndemnise est requis");
        }
        if (request.indemniteForfaitaireMensuelleEuros() == null) {
            throw new IllegalArgumentException(
                    "indemniteForfaitaireMensuelleEuros est requise");
        }
        if (request.indemniteForfaitaireMensuelleEuros()
                .compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "indemniteForfaitaireMensuelleEuros doit être positive ou nulle");
        }
        if (request.plafondIndemniteMensuelleEuros() == null) {
            throw new IllegalArgumentException(
                    "plafondIndemniteMensuelleEuros est requis");
        }
        if (request.plafondIndemniteMensuelleEuros()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "plafondIndemniteMensuelleEuros doit être strictement positif");
        }
        if (request.droitsSociauxMaintenus() == null) {
            throw new IllegalArgumentException(
                    "droitsSociauxMaintenus est requis");
        }
        if (request.deconnexionDefinie() == null) {
            throw new IllegalArgumentException(
                    "deconnexionDefinie est requise");
        }
        if (request.effectifEntreprise() == null) {
            throw new IllegalArgumentException(
                    "effectifEntreprise est requis");
        }
        if (request.effectifEntreprise() < 0) {
            throw new IllegalArgumentException(
                    "effectifEntreprise doit être positif ou nul");
        }
    }
}
