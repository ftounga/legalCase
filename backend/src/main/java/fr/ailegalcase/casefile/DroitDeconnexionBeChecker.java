package fr.ailegalcase.casefile;

/**
 * SF-219-19 : vérificateur de conformité de l'obligation <i>droit à
 * la déconnexion BE</i> (Loi du 03/10/2022 « Deal pour l'emploi »
 * M.B. 10/11/2022, art. 16 + AR du 19/02/2023 fixant la date d'entrée
 * en vigueur au 01/04/2023 + CCT n° 149 NAR/CNT télétravail recommandé)
 * — fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Loi du 03/10/2022</b> portant des dispositions diverses en
 *       matière de travail dite « Deal pour l'emploi » (M.B.
 *       10/11/2022), <b>art. 16</b> — obligation pesant sur les
 *       employeurs occupant au moins 20 travailleurs de conclure une
 *       CCT d'entreprise ou de modifier le règlement de travail pour
 *       définir les modalités du droit à la déconnexion. Trois thèmes
 *       obligatoires (§ 2) : 1° modalités pratiques, 2° sensibilisation
 *       et formation, 3° modalités d'organisation du travail garantissant
 *       le respect du temps de repos.</li>
 *   <li><b>AR du 19/02/2023</b> portant exécution de l'art. 16 — date
 *       d'entrée en vigueur au 01/04/2023.</li>
 *   <li><b>CCT n° 149</b> du Conseil national du travail
 *       (NAR/CNT) du 26/01/2021 sur le télétravail recommandé ou
 *       obligatoire — couvre la déconnexion pour les travailleurs en
 *       télétravail (référence d'inspiration sectorielle).</li>
 *   <li><b>Loi du 26/03/2018</b> relative au renforcement de la
 *       croissance économique et de la cohésion sociale, art. 15 à
 *       17 — premier dispositif national de concertation au sein du
 *       CPPT (Comité pour la prévention et la protection au travail)
 *       sur l'usage des outils numériques (abrogé / remplacé pour les
 *       employeurs ≥ 20 travailleurs par l'art. 16 Loi 03/10/2022,
 *       mais socle utile pour les &lt; 20).</li>
 *   <li><b>C. pén. social du 06/06/2010</b>, art. 137 — sanction de
 *       niveau 2 (amende administrative 100 à 1 000 € ou pénale 200 à
 *       2 000 € par travailleur, plafonnée) en cas de manquement à
 *       l'obligation d'inscription au règlement de travail.</li>
 *   <li><b>Loi du 16/03/1971</b> sur le travail (M.B. 30/03/1971),
 *       art. 38 quinquies — droit au repos quotidien de 11 heures
 *       consécutives entre deux périodes de travail (substrat
 *       fondamental que la déconnexion vise à garantir).</li>
 * </ul>
 *
 * <h2>Hiérarchie des verdicts (court-circuits successifs)</h2>
 * <ol>
 *   <li><b>A_ANALYSER</b> — statut accord
 *       {@link DroitDeconnexionBeStatutAccord#INDETERMINE}.</li>
 *   <li><b>HORS_CHAMP_SEUIL_NON_ATTEINT</b> — effectif &lt; 20
 *       travailleurs : obligation non applicable.</li>
 *   <li><b>MANQUEMENT_GRAVE_AUCUNE_INITIATIVE</b> — aucun accord ni
 *       amorce de concertation.</li>
 *   <li><b>NON_CONFORME_INSTRUMENT_MANQUANT</b> — concertation en
 *       cours mais aucun instrument formalisé (CCT non déposée et
 *       règlement de travail non modifié).</li>
 *   <li><b>NON_CONFORME_CONTENU_INCOMPLET</b> — instrument formalisé
 *       mais un ou plusieurs thèmes art. 16 § 2 manquants.</li>
 *   <li>Sinon → <b>CONFORME_ACCORD_COMPLET</b>.</li>
 * </ol>
 */
public final class DroitDeconnexionBeChecker {

    /** Seuil d'effectif déclenchant l'obligation (art. 16 § 1 Loi 03/10/2022). */
    static final int SEUIL_EFFECTIF_DECLENCHEUR = 20;

    static final String BASE_JURIDIQUE =
            "Loi du 03/10/2022 portant des dispositions diverses en"
                    + " matière de travail dite « Deal pour l'emploi »"
                    + " (M.B. 10/11/2022), art. 16 (obligation pesant"
                    + " sur les employeurs occupant au moins 20"
                    + " travailleurs de conclure une CCT d'entreprise ou"
                    + " de modifier le règlement de travail pour définir"
                    + " les modalités du droit à la déconnexion ; trois"
                    + " thèmes obligatoires § 2 : 1° modalités pratiques,"
                    + " 2° sensibilisation et formation, 3° modalités"
                    + " d'organisation garantissant le respect du temps"
                    + " de repos) ; AR du 19/02/2023 portant exécution"
                    + " de l'art. 16 (entrée en vigueur 01/04/2023) ;"
                    + " CCT n° 149 du Conseil national du travail"
                    + " (NAR/CNT) du 26/01/2021 sur le télétravail"
                    + " recommandé ou obligatoire (couvre la déconnexion"
                    + " pour les travailleurs en télétravail —"
                    + " inspiration sectorielle) ; Loi du 26/03/2018"
                    + " relative au renforcement de la croissance"
                    + " économique et de la cohésion sociale, art. 15 à"
                    + " 17 (premier dispositif national de concertation"
                    + " au sein du CPPT sur l'usage des outils numériques,"
                    + " socle utile pour les employeurs < 20 travailleurs) ;"
                    + " C. pén. social du 06/06/2010, art. 137 (sanction"
                    + " de niveau 2 en cas de manquement à l'inscription"
                    + " au règlement de travail) ; Loi du 16/03/1971 sur"
                    + " le travail (M.B. 30/03/1971), art. 38 quinquies"
                    + " (droit au repos quotidien de 11 heures consécutives"
                    + " — substrat fondamental garanti par la déconnexion).";

    static final String AVERT_AVOCAT_BE =
            "Le droit à la déconnexion BE est récent (entrée en vigueur"
                    + " 01/04/2023) et la jurisprudence post-2023 reste"
                    + " limitée. L'avocat spécialisé en droit social BE"
                    + " doit confirmer pour la situation considérée :"
                    + " (i) le décompte exact de l'effectif au sens de"
                    + " la Loi 04/12/2007 sur les élections sociales,"
                    + " art. 7 (équivalents temps plein, période de"
                    + " référence, sociétés liées) — un effectif autour"
                    + " du seuil de 20 mérite un audit complet ; (ii)"
                    + " l'instrument le plus adapté au cabinet (CCT"
                    + " d'entreprise déposée au greffe SPF Emploi"
                    + " versus modification du règlement de travail par"
                    + " procédure ordinaire Loi 08/04/1965 art. 11) et"
                    + " l'articulation avec la CCT n° 149 si du"
                    + " télétravail est pratiqué ; (iii) le contenu"
                    + " minimum effectif de chaque thème (modalités"
                    + " pratiques : plages horaires, canaux,"
                    + " gestion des urgences ; sensibilisation :"
                    + " fréquence, format, public cible ; organisation :"
                    + " coupure des serveurs, paramétrage outils,"
                    + " modulation charge de travail) ; (iv) le risque"
                    + " de sanction pénale niveau 2 (C. pén. social"
                    + " art. 137) et l'action en cessation pouvant être"
                    + " introduite par les organisations syndicales ;"
                    + " (v) l'opportunité pour le travailleur individuel"
                    + " de fonder une action en responsabilité civile"
                    + " contractuelle (art. 1147 anc. C. civ. /"
                    + " art. 5.83 nouveau C. civ.) ou un recours en"
                    + " résolution judiciaire du contrat aux torts de"
                    + " l'employeur en cas de manquement caractérisé.";

    private DroitDeconnexionBeChecker() {
    }

    public static DroitDeconnexionBeResult analyze(
            DroitDeconnexionBeRequest request) {
        validateInputs(request);

        boolean seuilAtteint =
                request.effectifEntreprise() >= SEUIL_EFFECTIF_DECLENCHEUR;
        boolean instrumentFormalise =
                request.statutAccord()
                        == DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE
                        || request.statutAccord()
                        == DroitDeconnexionBeStatutAccord.REGLEMENT_TRAVAIL_MODIFIE;
        boolean modalitesPratiquesRespectees =
                request.modalitesPratiquesDeconnexionDefinies();
        boolean sensibilisationRespectee =
                request.sensibilisationFormationPrevue();
        boolean modalitesOrganisationRespectees =
                request.modalitesOrganisationTravailDefinies();
        boolean consultationRespectee =
                request.consultationOrganeConcertationEffectuee();
        boolean contenuComplet = modalitesPratiquesRespectees
                && sensibilisationRespectee
                && modalitesOrganisationRespectees;

        DroitDeconnexionBeStatutAccord statut = request.statutAccord();

        // ── Hiérarchie 1 : statut indéterminé ────────────────────────────────
        if (statut == DroitDeconnexionBeStatutAccord.INDETERMINE) {
            String synthese = "Statut de l'accord sur le droit à la"
                    + " déconnexion indéterminé : pas encore"
                    + " d'instruction sur l'existence d'une CCT"
                    + " d'entreprise, d'une modification du règlement"
                    + " de travail ou d'une concertation en cours."
                    + " Une analyse au cas par cas est requise pour"
                    + " qualifier la situation au regard de l'art. 16"
                    + " Loi du 03/10/2022 (« Deal pour l'emploi »).";
            return build(request,
                    DroitDeconnexionBeVerdict.A_ANALYSER,
                    seuilAtteint, instrumentFormalise, contenuComplet,
                    modalitesPratiquesRespectees, sensibilisationRespectee,
                    modalitesOrganisationRespectees, consultationRespectee,
                    "STATUT_ACCORD_INDETERMINE",
                    synthese);
        }

        // ── Hiérarchie 2 : seuil non atteint ────────────────────────────────
        if (!seuilAtteint) {
            String synthese = "L'entreprise occupe "
                    + request.effectifEntreprise() + " travailleur(s),"
                    + " ce qui est inférieur au seuil de "
                    + SEUIL_EFFECTIF_DECLENCHEUR + " déclenchant"
                    + " l'obligation art. 16 § 1 Loi du 03/10/2022."
                    + " L'employeur n'est pas tenu de conclure une CCT"
                    + " d'entreprise ou de modifier le règlement de"
                    + " travail. La concertation au sein du CPPT sur"
                    + " l'usage des outils numériques (Loi 26/03/2018"
                    + " art. 15 à 17) reste un cadre subsidiaire"
                    + " utile, et le droit au repos quotidien de 11"
                    + " heures (Loi 16/03/1971 art. 38 quinquies)"
                    + " demeure pleinement applicable.";
            return build(request,
                    DroitDeconnexionBeVerdict.HORS_CHAMP_SEUIL_NON_ATTEINT,
                    false, instrumentFormalise, contenuComplet,
                    modalitesPratiquesRespectees, sensibilisationRespectee,
                    modalitesOrganisationRespectees, consultationRespectee,
                    "EFFECTIF_INFERIEUR_SEUIL",
                    synthese);
        }

        // ── Hiérarchie 3 : aucune initiative — manquement grave ─────────────
        if (statut == DroitDeconnexionBeStatutAccord.AUCUNE_INITIATIVE) {
            String synthese = "L'entreprise occupe "
                    + request.effectifEntreprise() + " travailleurs"
                    + " (≥ " + SEUIL_EFFECTIF_DECLENCHEUR + ") mais"
                    + " aucune CCT d'entreprise n'a été conclue, le"
                    + " règlement de travail n'a pas été modifié et"
                    + " aucune concertation n'est engagée. Manquement"
                    + " frontal à l'obligation art. 16 Loi 03/10/2022"
                    + " exposant l'employeur à la sanction pénale de"
                    + " niveau 2 (C. pén. social art. 137 — amende 100"
                    + " à 1 000 € administrative ou 200 à 2 000 € pénale"
                    + " par travailleur, plafonnée) et à une action en"
                    + " cessation par les organisations syndicales. Le"
                    + " travailleur individuel peut engager une action"
                    + " en responsabilité civile contractuelle"
                    + " (art. 1147 anc. C. civ. / art. 5.83 nouveau"
                    + " C. civ.) ou demander la résolution judiciaire"
                    + " aux torts de l'employeur en cas de préjudice"
                    + " caractérisé (burn-out, atteinte au temps de"
                    + " repos).";
            return build(request,
                    DroitDeconnexionBeVerdict.MANQUEMENT_GRAVE_AUCUNE_INITIATIVE,
                    true, false, false,
                    modalitesPratiquesRespectees, sensibilisationRespectee,
                    modalitesOrganisationRespectees, consultationRespectee,
                    "AUCUNE_INITIATIVE",
                    synthese);
        }

        // ── Hiérarchie 4 : négociation en cours — instrument manquant ───────
        if (statut == DroitDeconnexionBeStatutAccord.NEGOCIATION_EN_COURS) {
            String synthese = "L'entreprise occupe "
                    + request.effectifEntreprise() + " travailleurs"
                    + " (≥ " + SEUIL_EFFECTIF_DECLENCHEUR + "), une"
                    + " concertation est engagée mais aucun instrument"
                    + " (CCT d'entreprise déposée au greffe SPF Emploi"
                    + " ou modification du règlement de travail) n'a"
                    + " été formalisé. L'art. 16 Loi 03/10/2022"
                    + " impose une obligation de résultat : la"
                    + " concertation seule ne suffit pas. Le SPF Emploi"
                    + " peut constater le manquement lors d'un"
                    + " contrôle (C. pén. social art. 137). L'avocat"
                    + " conseille de finaliser sans délai l'instrument"
                    + " choisi en se ménageant la preuve écrite de la"
                    + " bonne foi des négociations (procès-verbaux CE /"
                    + " CPPT) pour atténuer une éventuelle sanction.";
            return build(request,
                    DroitDeconnexionBeVerdict.NON_CONFORME_INSTRUMENT_MANQUANT,
                    true, false, contenuComplet,
                    modalitesPratiquesRespectees, sensibilisationRespectee,
                    modalitesOrganisationRespectees, consultationRespectee,
                    "INSTRUMENT_NON_FORMALISE",
                    synthese);
        }

        // ── Hiérarchie 5 : instrument formalisé, contenu incomplet ──────────
        if (!contenuComplet) {
            StringBuilder manques = new StringBuilder();
            if (!modalitesPratiquesRespectees) {
                manques.append(" modalités pratiques de déconnexion"
                        + " (art. 16 § 2 1°),");
            }
            if (!sensibilisationRespectee) {
                manques.append(" sensibilisation et formation"
                        + " (art. 16 § 2 2°),");
            }
            if (!modalitesOrganisationRespectees) {
                manques.append(" modalités d'organisation garantissant"
                        + " le respect du temps de repos"
                        + " (art. 16 § 2 3°),");
            }
            String manquants = manques.length() > 0
                    ? manques.substring(0, manques.length() - 1)
                    : "";
            String synthese = "L'instrument formalisé ("
                    + (statut == DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE
                            ? "CCT d'entreprise" : "règlement de travail modifié")
                    + ") ne couvre pas l'ensemble des trois thèmes"
                    + " obligatoires art. 16 § 2. Manque(nt) :"
                    + manquants + ". Le SPF Emploi peut considérer"
                    + " l'obligation comme partiellement remplie et"
                    + " imposer la régularisation par injonction (C."
                    + " pén. social art. 137). L'avocat invite à"
                    + " compléter l'instrument par avenant CCT ou"
                    + " modification additionnelle du règlement de"
                    + " travail avant tout contrôle ou contentieux.";
            return build(request,
                    DroitDeconnexionBeVerdict.NON_CONFORME_CONTENU_INCOMPLET,
                    true, true, false,
                    modalitesPratiquesRespectees, sensibilisationRespectee,
                    modalitesOrganisationRespectees, consultationRespectee,
                    "CONTENU_INCOMPLET",
                    synthese);
        }

        // ── Cas qualifiant : accord complet ─────────────────────────────────
        String avertConsult = consultationRespectee
                ? ""
                : " Attention : la consultation de l'organe de"
                + " concertation (CE / CPPT / DS) n'apparaît pas"
                + " documentée — fragilité procédurale pouvant"
                + " être invoquée par les organisations syndicales"
                + " pour contester la validité de l'instrument.";
        String synthese = "L'entreprise occupe "
                + request.effectifEntreprise() + " travailleurs et a"
                + " formalisé son obligation déconnexion via "
                + (statut == DroitDeconnexionBeStatutAccord.CCT_ENTREPRISE_DEPOSEE
                        ? "une CCT d'entreprise déposée au greffe du SPF"
                        + " Emploi"
                        : "une modification du règlement de travail")
                + (request.dateEntreeVigueurInstrument() != null
                        ? " (entrée en vigueur " + request
                        .dateEntreeVigueurInstrument() + ")" : "")
                + ". Les trois thèmes obligatoires art. 16 § 2 Loi"
                + " 03/10/2022 sont traités : modalités pratiques de"
                + " déconnexion, sensibilisation et formation,"
                + " modalités d'organisation garantissant le respect"
                + " du temps de repos. Conformité globale à"
                + " l'obligation déconnexion." + avertConsult;
        return build(request,
                DroitDeconnexionBeVerdict.CONFORME_ACCORD_COMPLET,
                true, true, true,
                modalitesPratiquesRespectees, sensibilisationRespectee,
                modalitesOrganisationRespectees, consultationRespectee,
                "CONFORME_ACCORD_COMPLET",
                synthese);
    }

    private static DroitDeconnexionBeResult build(
            DroitDeconnexionBeRequest request,
            DroitDeconnexionBeVerdict verdict,
            boolean seuilAtteint,
            boolean instrumentFormalise,
            boolean contenuComplet,
            boolean modalitesPratiquesRespectees,
            boolean sensibilisationRespectee,
            boolean modalitesOrganisationRespectees,
            boolean consultationRespectee,
            String raison,
            String synthese) {
        return new DroitDeconnexionBeResult(
                request.effectifEntreprise(),
                request.statutAccord(),
                request.dateEntreeVigueurInstrument(),
                request.modalitesPratiquesDeconnexionDefinies(),
                request.sensibilisationFormationPrevue(),
                request.modalitesOrganisationTravailDefinies(),
                request.consultationOrganeConcertationEffectuee(),
                request.manquementSignaleCbeOuSpf(),
                verdict,
                seuilAtteint,
                instrumentFormalise,
                contenuComplet,
                modalitesPratiquesRespectees,
                sensibilisationRespectee,
                modalitesOrganisationRespectees,
                consultationRespectee,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(DroitDeconnexionBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.effectifEntreprise() == null) {
            throw new IllegalArgumentException(
                    "effectifEntreprise est requis");
        }
        if (request.effectifEntreprise() < 0) {
            throw new IllegalArgumentException(
                    "effectifEntreprise doit être positif ou nul");
        }
        if (request.statutAccord() == null) {
            throw new IllegalArgumentException(
                    "statutAccord est requis");
        }
        if (request.modalitesPratiquesDeconnexionDefinies() == null) {
            throw new IllegalArgumentException(
                    "modalitesPratiquesDeconnexionDefinies est requis");
        }
        if (request.sensibilisationFormationPrevue() == null) {
            throw new IllegalArgumentException(
                    "sensibilisationFormationPrevue est requis");
        }
        if (request.modalitesOrganisationTravailDefinies() == null) {
            throw new IllegalArgumentException(
                    "modalitesOrganisationTravailDefinies est requis");
        }
        if (request.consultationOrganeConcertationEffectuee() == null) {
            throw new IllegalArgumentException(
                    "consultationOrganeConcertationEffectuee est requis");
        }
        if (request.manquementSignaleCbeOuSpf() == null) {
            throw new IllegalArgumentException(
                    "manquementSignaleCbeOuSpf est requis");
        }
    }
}
