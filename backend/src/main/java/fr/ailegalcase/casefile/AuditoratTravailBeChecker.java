package fr.ailegalcase.casefile;

/**
 * SF-219-25 : orienteur de saisine de l'auditorat du travail BE —
 * fonction <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <h2>Source juridique</h2>
 * <ul>
 *   <li><b>Art. 138bis Code judiciaire</b> — l'auditorat du travail
 *       est le parquet spécialisé compétent en matière sociale ;
 *       exerce l'action publique pour les infractions au Code pénal
 *       social et rend un avis obligatoire dans les matières
 *       art. 580 et 581 C. jud. (contentieux sécurité sociale et
 *       travailleurs indépendants).</li>
 *   <li><b>Art. 24 Code d'instruction criminelle</b> — modes de
 *       saisine du parquet : plainte, dénonciation, transmission de
 *       PV par les services d'inspection (art. 76 C. pén. soc.).</li>
 *   <li><b>Loi du 03/08/1992</b> sur le Code judiciaire — organisation
 *       de l'auditorat du travail (un par ressort de tribunal du
 *       travail, sous l'autorité d'un auditeur général près la Cour
 *       du travail).</li>
 *   <li><b>Art. 73 à 78 C. pén. soc.</b> — « règle Una Via » : choix
 *       exclusif de l'auditeur entre voie pénale (citation tribunal
 *       correctionnel) et voie administrative (transmission SPF
 *       Emploi pour amende administrative). Cour const. arrêt
 *       61/2014 du 03/04/2014.</li>
 *   <li><b>Art. 76 C. pén. soc.</b> — transmission obligatoire des
 *       PV d'inspection à l'auditeur du travail dans les 14 jours.</li>
 *   <li><b>Art. 81 C. pén. soc.</b> — prescription pénale : 5 ans à
 *       compter du jour de l'infraction pour les infractions de
 *       niveaux 2, 3 et 4 ; 3 ans pour les infractions de niveau 1.
 *       Renvoi aux art. 21-22 Titre prél. CIC pour l'interruption.</li>
 *   <li><b>Art. 22-23 Titre prél. CIC</b> — interruption de la
 *       prescription par tout acte d'instruction ou de poursuite
 *       valable.</li>
 *   <li><b>Art. 28bis et 28quater CIC</b> — déclenchement obligatoire
 *       de l'information par la plainte avec constitution de partie
 *       civile ; opportunité des poursuites du parquet pour la
 *       dénonciation simple.</li>
 *   <li><b>Art. 4 Titre prél. CIC</b> — autonomie de la voie
 *       répressive vis-à-vis de la voie civile (l'action civile
 *       n'éteint pas l'action publique et vice versa, sous réserve
 *       des règles « le criminel tient le civil en l'état »
 *       art. 4 in fine).</li>
 *   <li><b>Art. 100 et 100bis C. pén. soc.</b> — PV d'inspection
 *       faisant foi jusqu'à preuve contraire ; force probante
 *       particulière conditionnée par les mentions obligatoires.</li>
 *   <li><b>Art. 442bis Code pénal</b> — harcèlement moral / sexuel
 *       constituant l'infraction de droit commun (en sus de la
 *       Loi 04/08/1996 sur le bien-être).</li>
 *   <li><b>Art. 22-24 Loi du 10/05/2007</b> tendant à lutter contre
 *       certaines formes de discrimination — qualifications pénales
 *       de la discrimination intentionnelle.</li>
 *   <li><b>Cass. ch. néerl. 20/03/2018 P.17.1175.N</b> — compétence
 *       exclusive de l'auditeur du travail pour la poursuite des
 *       infractions au Code pénal social (exception au monopole du
 *       procureur du Roi art. 22 C. jud.).</li>
 * </ul>
 *
 * <h2>Logique du qualificateur</h2>
 * <ol>
 *   <li>Si la nature est {@link AuditoratTravailBeNatureFait#AUTRE} →
 *       verdict {@link AuditoratTravailBeVerdict#A_QUALIFIER}.</li>
 *   <li>Si {@code faitsPrescrits} = true → verdict
 *       {@link AuditoratTravailBeVerdict#SAISINE_NON_PERTINENTE} avec
 *       drapeau {@code prescriptionBloquante}.</li>
 *   <li>Si nature {@link AuditoratTravailBeNatureFait#LITIGE_CIVIL_PUR}
 *       → {@link AuditoratTravailBeVerdict#SAISINE_NON_PERTINENTE}
 *       (compétence tribunal du travail art. 578 C. jud.).</li>
 *   <li>Si nature {@link AuditoratTravailBeNatureFait#TRAVAIL_NON_DECLARE_SUSPECTE}
 *       et inspection non saisie → recommandation
 *       {@link AuditoratTravailBeVerdict#DENONCIATION_INSPECTION_PREALABLE}
 *       pour objectiver les faits via PV.</li>
 *   <li>Sinon (infraction pénale sociale, accident grave, discrimination
 *       pénale, harcèlement pénal, entrave inspection) →
 *       {@link AuditoratTravailBeVerdict#SAISINE_AUDITORAT_RECOMMANDEE}.</li>
 * </ol>
 */
public final class AuditoratTravailBeChecker {

    static final String BASE_JURIDIQUE =
            "Code judiciaire art. 138bis (auditorat du travail parquet"
                    + " spécialisé compétent pour les infractions au"
                    + " Code pénal social et avis obligatoire matières"
                    + " art. 580 et 581 C. jud.) ; Code d'instruction"
                    + " criminelle art. 24 (modes de saisine du parquet"
                    + " plainte, dénonciation, transmission de PV"
                    + " inspection) + art. 28bis et 28quater CIC"
                    + " (déclenchement de l'information par plainte"
                    + " avec constitution partie civile, opportunité"
                    + " des poursuites par le parquet pour la"
                    + " dénonciation simple) + art. 4 Titre prél. CIC"
                    + " (autonomie voie répressive vis-à-vis voie"
                    + " civile, règle « le criminel tient le civil en"
                    + " l'état ») + art. 63 CIC (constitution de"
                    + " partie civile, droit du plaignant à"
                    + " déclencher l'information) ; Loi du 03/08/1992"
                    + " sur le Code judiciaire (organisation de"
                    + " l'auditorat du travail un par ressort de"
                    + " tribunal du travail sous l'autorité d'un"
                    + " auditeur général près la Cour du travail) ;"
                    + " Loi du 06/06/2010 introduisant le Code pénal"
                    + " social (M.B. 01/07/2010, e.e.v. 01/07/2011)"
                    + " — art. 73 à 78 C. pén. soc. (« règle Una Via »"
                    + " choix exclusif de l'auditeur entre voie pénale"
                    + " citation tribunal correctionnel art. 76 et"
                    + " voie administrative transmission SPF Emploi"
                    + " pour amende administrative art. 84, exclusivité"
                    + " du non bis in idem, Cour const. arrêt 61/2014"
                    + " du 03/04/2014, CEDH Grande Stevens c. Italie"
                    + " 04/03/2014) + art. 76 C. pén. soc."
                    + " (transmission obligatoire des PV d'inspection"
                    + " à l'auditeur du travail dans les 14 jours)"
                    + " + art. 81 C. pén. soc. (prescription pénale"
                    + " 5 ans à compter du jour de l'infraction pour"
                    + " les niveaux 2, 3 et 4 ; 3 ans pour le niveau"
                    + " 1 ; renvoi art. 21-22 Titre prél. CIC pour"
                    + " l'interruption) + art. 100 et 100bis C. pén."
                    + " soc. (PV d'inspection faisant foi jusqu'à"
                    + " preuve contraire, force probante particulière"
                    + " conditionnée par les mentions obligatoires) +"
                    + " art. 209-212 C. pén. soc. (entrave à"
                    + " l'inspection sociale niveau 3) + art. 124"
                    + " C. pén. soc. (accident grave non déclaré"
                    + " niveau 4) + art. 181 C. pén. soc. (travail non"
                    + " déclaré Dimona niveau 4) ; Loi du 04/08/1996"
                    + " relative au bien-être des travailleurs"
                    + " art. 26 (obligation de notification de"
                    + " l'accident grave à l'inspection bien-être) ;"
                    + " Code pénal art. 442bis (harcèlement moral et"
                    + " sexuel, infraction de droit commun en sus de"
                    + " la Loi 04/08/1996) + art. 5 C. pén."
                    + " (responsabilité pénale des personnes morales) ;"
                    + " Loi du 10/05/2007 tendant à lutter contre"
                    + " certaines formes de discrimination art. 22 à"
                    + " 24 (qualifications pénales de la discrimination"
                    + " intentionnelle) + art. 587bis C. jud. (action"
                    + " civile en cessation devant le tribunal du"
                    + " travail) ; Loi du 12/04/1965 concernant la"
                    + " protection de la rémunération des travailleurs"
                    + " (compétence civile tribunal du travail"
                    + " art. 578 C. jud.) ; Loi du 03/07/1978 relative"
                    + " aux contrats de travail (indemnités de rupture,"
                    + " compétence civile tribunal du travail"
                    + " art. 578) ; art. 578 et 580-581 C. jud."
                    + " (compétence du tribunal du travail pour les"
                    + " litiges civils du travail et matières où"
                    + " l'auditeur rend un avis obligatoire) ;"
                    + " Cass. ch. néerl. 20/03/2018 P.17.1175.N"
                    + " (compétence exclusive de l'auditeur du travail"
                    + " pour la poursuite des infractions au Code"
                    + " pénal social, exception au monopole du"
                    + " procureur du Roi art. 22 C. jud.).";

    static final String AVERT_AVOCAT_BE =
            "L'outil d'orientation de saisine de l'auditorat du"
                    + " travail BE produit un verdict indicatif fondé"
                    + " sur les déclarations de l'avocat. L'avocat"
                    + " spécialisé en droit social pénal BE doit"
                    + " confirmer pour la situation considérée : (i) la"
                    + " qualification pénale précise des faits au"
                    + " regard du Livre 2 du Code pénal social"
                    + " (art. 119 à 235) ou du Code pénal commun"
                    + " (art. 442bis harcèlement, art. 196 et s. faux,"
                    + " art. 25-26 Loi 10/05/2007 discrimination) — un"
                    + " même fait peut tomber sous plusieurs"
                    + " qualifications concurrentes, le concours"
                    + " d'infractions obéit aux règles de l'art. 65"
                    + " C. pén. (peine unique du chef le plus grave) ;"
                    + " (ii) la prescription pénale art. 81 C. pén."
                    + " soc. (5 ans niveaux 2-4 ; 3 ans niveau 1)"
                    + " avec les actes interruptifs art. 22-23 Titre"
                    + " prél. CIC (toute mesure d'instruction valable"
                    + " interrompt et fait courir un nouveau délai)"
                    + " et la suspension pour difficulté procédurale"
                    + " art. 24 Titre prél. CIC ; (iii) l'option"
                    + " administrative ou pénale de l'auditeur"
                    + " art. 73-78 C. pén. soc. (« règle Una Via »,"
                    + " choix exclusif, non bis in idem strictement"
                    + " opposable, Cour const. 61/2014, CEDH Grande"
                    + " Stevens 04/03/2014) — l'auditeur reste seul"
                    + " maître de l'opportunité des poursuites"
                    + " art. 28quater § 1er CIC ; (iv) la nécessité"
                    + " ou l'opportunité d'une saisine préalable de"
                    + " l'inspection sociale (Contrôle des lois"
                    + " sociales SPF Emploi, ONSS, INASTI, médecine"
                    + " du travail) pour objectiver les faits sur le"
                    + " terrain par PV faisant foi jusqu'à preuve"
                    + " contraire art. 100 et 100bis C. pén. soc. ;"
                    + " (v) la stratégie de constitution de partie"
                    + " civile art. 63 CIC + art. 4 Titre prél. CIC"
                    + " (autonomie voie répressive) versus dénonciation"
                    + " simple art. 29 CIC (sans qualité de partie au"
                    + " procès pénal, pas d'accès au dossier répressif"
                    + " hors information judiciaire) ; (vi)"
                    + " l'articulation avec une éventuelle procédure"
                    + " civile parallèle devant le tribunal du travail"
                    + " art. 578 C. jud. (paiement des arriérés Loi"
                    + " 12/04/1965, indemnités de rupture Loi"
                    + " 03/07/1978, dommages et intérêts art. 1382"
                    + " anc. C. civ. / 5.83 nouveau C. civ.) — la voie"
                    + " civile et la voie pénale sont autonomes mais"
                    + " la règle « le criminel tient le civil en"
                    + " l'état » art. 4 Titre prél. CIC peut imposer"
                    + " un sursis à statuer ; (vii) la compétence"
                    + " territoriale de l'auditorat (auditeur du"
                    + " travail du ressort où l'infraction a été"
                    + " commise ou du domicile du prévenu, choix du"
                    + " plaignant art. 62bis CIC) ; (viii) la"
                    + " compétence ratione materiae (art. 138bis"
                    + " C. jud. — infractions au Code pénal social ;"
                    + " parquet général ou procureur du Roi pour les"
                    + " infractions de droit commun connexes telles"
                    + " que faux art. 196, abus de confiance"
                    + " art. 491, escroquerie art. 496 ; saisine"
                    + " mixte possible) ; (ix) l'opportunité d'une"
                    + " transaction administrative avec le SPF Emploi"
                    + " art. 84 C. pén. soc. permettant d'éviter la"
                    + " citation pénale moyennant paiement de l'amende"
                    + " administrative ; (x) les voies de recours :"
                    + " tribunal du travail si voie administrative"
                    + " (art. 580, 1° C. jud., recours contre la"
                    + " décision du fonctionnaire SPF Emploi), tribunal"
                    + " correctionnel si voie pénale (art. 76 C. jud."
                    + " ressort territorial), Cour du travail puis"
                    + " Cass. comme juridiction de cassation, ou Cour"
                    + " d'appel correctionnelle puis Cass.";

    private AuditoratTravailBeChecker() {
    }

    public static AuditoratTravailBeResult analyze(
            AuditoratTravailBeRequest request) {
        validateInputs(request);

        AuditoratTravailBeNatureFait nature = request.natureFait();
        boolean faitsPrescrits = request.faitsPrescrits();
        boolean inspectionDejaSaisie = request.inspectionDejaSaisie();
        boolean plainteCivileDeposee = request.plainteCivileDeposee();
        boolean urgence = request.urgenceSecuritePersonnes();
        boolean recoursPenal = request.recoursPenalEnvisage();

        // ── A_QUALIFIER : nature ouverte ────────────────────────────────────
        if (nature == AuditoratTravailBeNatureFait.AUTRE) {
            String synthese = "La nature des faits est ouverte (AUTRE)."
                    + " Une qualification pénale précise est requise"
                    + " préalablement à toute saisine de l'auditorat"
                    + " du travail. L'avocat identifie le siège"
                    + " textuel (Livre 2 Code pénal social art. 119"
                    + " à 235 pour les infractions sociales ;"
                    + " Code pénal commun art. 442bis harcèlement,"
                    + " art. 196 faux, art. 491 abus de confiance pour"
                    + " les infractions de droit commun connexes), la"
                    + " compétence ratione materiae art. 138bis"
                    + " C. jud. et l'opportunité d'une saisine"
                    + " préalable de l'inspection sociale pour"
                    + " objectiver les faits.";
            return build(request,
                    AuditoratTravailBeVerdict.A_QUALIFIER,
                    AuditoratTravailBeModeSaisine.INDETERMINE,
                    false, false, plainteCivileDeposee, false,
                    false, false, false,
                    "QUALIFICATION_OUVERTE",
                    synthese);
        }

        // ── Prescription bloquante ──────────────────────────────────────────
        if (faitsPrescrits) {
            String synthese = "Les faits étant prescrits (art. 81"
                    + " C. pén. soc. — 5 ans pour les infractions de"
                    + " niveaux 2 à 4, 3 ans pour le niveau 1, à"
                    + " compter du jour de l'infraction, renvoi"
                    + " art. 21-22 Titre prél. CIC pour"
                    + " l'interruption), la voie pénale est éteinte."
                    + " L'auditorat du travail ne pourra plus exercer"
                    + " l'action publique. Seule la voie civile reste"
                    + " ouverte devant le tribunal du travail"
                    + " (art. 578 C. jud.) pour les conséquences"
                    + " patrimoniales (arriérés salariaux Loi"
                    + " 12/04/1965, indemnités de rupture Loi"
                    + " 03/07/1978, dommages et intérêts art. 1382"
                    + " anc. C. civ. / 5.83 nouveau C. civ.), avec"
                    + " son propre régime de prescription"
                    + " (typiquement 1 an pour les actions nées du"
                    + " contrat de travail art. 15 Loi 03/07/1978,"
                    + " 5 ans en droit commun).";
            return build(request,
                    AuditoratTravailBeVerdict.SAISINE_NON_PERTINENTE,
                    AuditoratTravailBeModeSaisine.VOIE_CIVILE_PARALLELE,
                    false, false, true, true,
                    false, false, false,
                    "PRESCRIPTION_PENALE_ACQUISE",
                    synthese);
        }

        // ── Litige civil pur — compétence tribunal du travail ───────────────
        if (nature == AuditoratTravailBeNatureFait.LITIGE_CIVIL_PUR) {
            String synthese = "Les faits décrits relèvent d'un litige"
                    + " civil pur (contestation d'exécution du contrat"
                    + " de travail, paiement d'arriérés salariaux,"
                    + " indemnité de rupture, prime, commission,"
                    + " préavis). La compétence ratione materiae"
                    + " appartient au tribunal du travail art. 578"
                    + " C. jud., pas à l'auditorat du travail. La"
                    + " saisine de l'auditorat serait non pertinente"
                    + " (l'auditeur déclinerait sa compétence"
                    + " art. 138bis C. jud., aucune infraction au"
                    + " Code pénal social n'étant caractérisée)."
                    + " Voie recommandée : saisine directe du tribunal"
                    + " du travail par requête contradictoire ou"
                    + " citation art. 700 et s. C. jud.";
            return build(request,
                    AuditoratTravailBeVerdict.SAISINE_NON_PERTINENTE,
                    AuditoratTravailBeModeSaisine.VOIE_CIVILE_PARALLELE,
                    false, false, true, false,
                    false, false, false,
                    "COMPETENCE_TRIBUNAL_TRAVAIL_ART_578",
                    synthese);
        }

        // ── Travail non déclaré suspecté — inspection préalable recommandée ─
        if (nature == AuditoratTravailBeNatureFait.TRAVAIL_NON_DECLARE_SUSPECTE
                && !inspectionDejaSaisie) {
            String synthese = "La suspicion de travail non déclaré"
                    + " (art. 181 C. pén. soc., absence de Dimona ou"
                    + " d'inscription au registre du personnel) appelle"
                    + " une objectivation préalable des faits sur le"
                    + " terrain par les services d'inspection sociale"
                    + " (Contrôle des lois sociales SPF Emploi, ONSS"
                    + " ou INASTI selon le statut). Le PV d'inspection"
                    + " art. 100 C. pén. soc. fait foi jusqu'à preuve"
                    + " contraire et sera transmis à l'auditeur du"
                    + " travail dans les 14 jours art. 76 C. pén."
                    + " soc., déclenchant l'action publique sans"
                    + " formalité supplémentaire du plaignant. Le"
                    + " signalement à l'inspection est plus discret,"
                    + " économe en moyens, et augmente significativement"
                    + " la qualité du dossier répressif (Cass."
                    + " 21/11/2017 P.17.0532.N qualification travail"
                    + " non déclaré niveau 4)."
                    + suffixeUrgence(urgence)
                    + suffixeVoieCivile(plainteCivileDeposee);
            return build(request,
                    AuditoratTravailBeVerdict.DENONCIATION_INSPECTION_PREALABLE,
                    AuditoratTravailBeModeSaisine.SIGNALEMENT_INSPECTION_SOCIALE,
                    false, true, plainteCivileDeposee, false,
                    urgence, true, false,
                    "INSPECTION_PREALABLE_TRAVAIL_NON_DECLARE",
                    synthese);
        }

        // ── Saisine auditorat recommandée — cas nominal ─────────────────────
        AuditoratTravailBeModeSaisine modeRecommande =
                determineModeRecommande(recoursPenal, inspectionDejaSaisie);
        boolean partieCivile = recoursPenal
                && (modeRecommande == AuditoratTravailBeModeSaisine.PLAINTE_DIRECTE);
        boolean avisCivilObligatoire = plainteCivileDeposee
                && estMatiereAvisObligatoire(nature);

        String descriptionNature = describeNature(nature);
        String synthese = "La saisine de l'auditorat du travail est"
                + " recommandée — les faits constitutifs de "
                + descriptionNature + " relèvent de la compétence"
                + " ratione materiae du parquet spécialisé"
                + " art. 138bis C. jud. (compétence exclusive pour"
                + " la poursuite des infractions au Code pénal social,"
                + " Cass. 20/03/2018 P.17.1175.N). Mode de saisine"
                + " recommandé : " + describeMode(modeRecommande) + "."
                + (partieCivile
                        ? " La constitution de partie civile"
                                + " art. 63 CIC déclenche obligatoirement"
                                + " l'information judiciaire (art. 28bis"
                                + " CIC) et confère au plaignant la"
                                + " qualité de partie au procès pénal"
                                + " avec accès au dossier répressif."
                        : " La dénonciation simple art. 29 CIC laisse"
                                + " l'opportunité des poursuites à"
                                + " l'auditeur art. 28quater § 1er CIC,"
                                + " sans qualité de partie au procès"
                                + " pénal pour le dénonciateur.")
                + " L'auditeur arbitrera entre voie pénale (citation"
                + " tribunal correctionnel art. 76 C. jud.) et voie"
                + " administrative (transmission SPF Emploi art. 84"
                + " C. pén. soc., amende administrative) selon la"
                + " « règle Una Via » art. 73-78 C. pén. soc."
                + " (exclusivité non bis in idem, Cour const."
                + " 61/2014)."
                + suffixeUrgence(urgence)
                + suffixeVoieCivile(plainteCivileDeposee)
                + (avisCivilObligatoire
                        ? " L'auditeur rendra un avis obligatoire"
                                + " dans le contentieux civil parallèle"
                                + " si la matière relève de l'art. 580"
                                + " ou 581 C. jud. (art. 138bis"
                                + " § 1er C. jud.)."
                        : "");

        return build(request,
                AuditoratTravailBeVerdict.SAISINE_AUDITORAT_RECOMMANDEE,
                modeRecommande,
                partieCivile,
                false,
                plainteCivileDeposee,
                false,
                urgence,
                true,
                avisCivilObligatoire,
                raisonPourNature(nature),
                synthese);
    }

    /**
     * Détermine le mode de saisine recommandé en fonction du choix
     * stratégique de l'avocat (voie pénale ou non) et de l'éventuelle
     * saisine préalable de l'inspection.
     */
    static AuditoratTravailBeModeSaisine determineModeRecommande(
            boolean recoursPenalEnvisage,
            boolean inspectionDejaSaisie) {
        if (recoursPenalEnvisage) {
            return AuditoratTravailBeModeSaisine.PLAINTE_DIRECTE;
        }
        if (inspectionDejaSaisie) {
            // Inspection déjà saisie → le PV remontera art. 76 C. pén.
            // soc. ; dénonciation simple complémentaire suffit.
            return AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE;
        }
        return AuditoratTravailBeModeSaisine.DENONCIATION_SIMPLE;
    }

    /**
     * Détermine si la nature des faits, conjuguée à une procédure
     * civile parallèle, déclenche l'avis obligatoire de l'auditeur
     * (art. 138bis § 1<sup>er</sup> C. jud., matières art. 580/581).
     *
     * <p>Les matières art. 580 (sécurité sociale travailleurs salariés)
     * et 581 (sécurité sociale travailleurs indépendants) appellent
     * l'avis obligatoire de l'auditeur. Pour le contentieux du
     * contrat de travail art. 578, l'avis est facultatif.</p>
     */
    static boolean estMatiereAvisObligatoire(
            AuditoratTravailBeNatureFait nature) {
        // En V1, on retient la conservatrice : l'avis n'est obligatoire
        // que si la nature des faits touche la sécurité sociale
        // (typiquement travail non déclaré qui implique cotisations
        // ONSS). Pour les autres natures (harcèlement, discrimination),
        // l'avis est facultatif art. 138bis § 2 C. jud.
        return nature == AuditoratTravailBeNatureFait.TRAVAIL_NON_DECLARE_SUSPECTE
                || nature == AuditoratTravailBeNatureFait.INFRACTION_PENALE_SOCIALE;
    }

    private static String describeNature(AuditoratTravailBeNatureFait nature) {
        switch (nature) {
            case INFRACTION_PENALE_SOCIALE:
                return "infraction au Code pénal social (Livre 2"
                        + " art. 119 à 235 — Titres 2 bien-être, 3"
                        + " durée du travail, 4 rémunération, 5"
                        + " Dimona, 6 inspection, 8 intérim, 9"
                        + " sécurité sociale, 10 discrimination, 11"
                        + " entrave inspection, 12 faux social)";
            case ACCIDENT_TRAVAIL_GRAVE:
                return "accident grave au travail (art. 26 Loi"
                        + " 04/08/1996 bien-être + art. 124 C. pén."
                        + " soc. niveau 4 omission de déclaration à"
                        + " l'auditorat et inspection)";
            case TRAVAIL_NON_DECLARE_SUSPECTE:
                return "suspicion de travail non déclaré (art. 181"
                        + " C. pén. soc. niveau 4, Cass."
                        + " 21/11/2017 P.17.0532.N)";
            case DISCRIMINATION_PENALE:
                return "discrimination pénale intentionnelle"
                        + " (art. 22 à 24 Loi du 10/05/2007 tendant à"
                        + " lutter contre certaines formes de"
                        + " discrimination, dol spécial requis)";
            case HARCELEMENT_PENAL:
                return "harcèlement moral ou sexuel constituant"
                        + " l'infraction art. 442bis Code pénal (en"
                        + " sus du manquement à la Loi 04/08/1996 sur"
                        + " le bien-être au travail traité en voie"
                        + " civile + médecin du travail / conseiller"
                        + " en prévention aspects psychosociaux)";
            case ENTRAVE_INSPECTION:
                return "entrave à l'exercice de la surveillance"
                        + " sociale (art. 209 à 212 C. pén. soc."
                        + " niveau 3 — refus d'accès aux lieux de"
                        + " travail, refus de communication de"
                        + " documents, fausse déclaration à"
                        + " l'inspecteur)";
            case LITIGE_CIVIL_PUR:
                return "litige civil pur — compétence tribunal du"
                        + " travail art. 578 C. jud.";
            case AUTRE:
            default:
                return nature.name();
        }
    }

    private static String describeMode(AuditoratTravailBeModeSaisine mode) {
        switch (mode) {
            case PLAINTE_DIRECTE:
                return "plainte directe avec constitution de partie"
                        + " civile devant l'auditeur du travail"
                        + " (art. 63 CIC + art. 138bis C. jud.)"
                        + " déclenchant obligatoirement l'information"
                        + " judiciaire art. 28bis CIC";
            case DENONCIATION_SIMPLE:
                return "dénonciation simple par courrier à l'auditeur"
                        + " du travail (art. 29 CIC) sans"
                        + " constitution de partie civile"
                        + " — l'opportunité des poursuites reste à"
                        + " l'auditeur art. 28quater § 1er CIC";
            case SIGNALEMENT_INSPECTION_SOCIALE:
                return "signalement aux services d'inspection sociale"
                        + " (Contrôle des lois sociales SPF Emploi,"
                        + " ONSS, INASTI, médecine du travail) qui"
                        + " constateront par PV et transmettront à"
                        + " l'auditeur art. 76 C. pén. soc.";
            case VOIE_CIVILE_PARALLELE:
                return "saisine directe du tribunal du travail"
                        + " (art. 578 C. jud.) par requête"
                        + " contradictoire ou citation, sans détour"
                        + " par l'auditorat";
            case INDETERMINE:
            default:
                return "mode non encore choisi — analyse préliminaire"
                        + " avocat";
        }
    }

    private static String suffixeUrgence(boolean urgence) {
        if (!urgence) {
            return "";
        }
        return " En présence d'une urgence relative à la sécurité"
                + " des personnes (accident grave imminent,"
                + " harcèlement persistant avec atteinte à"
                + " l'intégrité), il est recommandé d'attirer"
                + " explicitement l'attention de l'auditeur sur"
                + " cette urgence (mention « URGENT — SÉCURITÉ DES"
                + " PERSONNES » en en-tête de la plainte ou de la"
                + " dénonciation) afin de déclencher les mesures"
                + " conservatoires éventuelles et la réquisition"
                + " d'inspection sociale en urgence (art. 28sexies"
                + " CIC + pouvoirs propres de l'auditeur).";
    }

    private static String suffixeVoieCivile(boolean civileDeposee) {
        if (!civileDeposee) {
            return "";
        }
        return " Une procédure civile parallèle étant en cours"
                + " devant le tribunal du travail (art. 578 C. jud.),"
                + " la règle « le criminel tient le civil en l'état »"
                + " art. 4 in fine Titre prél. CIC peut imposer un"
                + " sursis à statuer civil dans l'attente de l'issue"
                + " pénale — l'autonomie des actions art. 4 al. 1er"
                + " Titre prél. CIC reste cependant le principe et"
                + " l'auditeur peut intervenir d'office au civil"
                + " art. 138bis C. jud.";
    }

    private static String raisonPourNature(
            AuditoratTravailBeNatureFait nature) {
        switch (nature) {
            case INFRACTION_PENALE_SOCIALE:
                return "SAISINE_INFRACTION_PENALE_SOCIALE";
            case ACCIDENT_TRAVAIL_GRAVE:
                return "SAISINE_ACCIDENT_GRAVE_AT";
            case TRAVAIL_NON_DECLARE_SUSPECTE:
                return "SAISINE_TRAVAIL_NON_DECLARE";
            case DISCRIMINATION_PENALE:
                return "SAISINE_DISCRIMINATION_PENALE";
            case HARCELEMENT_PENAL:
                return "SAISINE_HARCELEMENT_PENAL";
            case ENTRAVE_INSPECTION:
                return "SAISINE_ENTRAVE_INSPECTION";
            default:
                return "SAISINE_AUDITORAT";
        }
    }

    private static AuditoratTravailBeResult build(
            AuditoratTravailBeRequest request,
            AuditoratTravailBeVerdict verdict,
            AuditoratTravailBeModeSaisine modeRecommande,
            boolean partieCivile,
            boolean inspectionPrealable,
            boolean voieCivile,
            boolean prescriptionBloquante,
            boolean urgenceSignalee,
            boolean unaViaApplicable,
            boolean avisAuditeurObligatoireCivil,
            String raison,
            String synthese) {
        return new AuditoratTravailBeResult(
                request.natureFait(),
                request.modeSaisineEnvisage(),
                request.dateFaits(),
                request.faitsPrescrits(),
                request.inspectionDejaSaisie(),
                request.plainteCivileDeposee(),
                request.urgenceSecuritePersonnes(),
                request.recoursPenalEnvisage(),
                request.employeurPersonneMorale(),
                verdict,
                modeRecommande,
                partieCivile,
                inspectionPrealable,
                voieCivile,
                prescriptionBloquante,
                urgenceSignalee,
                unaViaApplicable,
                avisAuditeurObligatoireCivil,
                raison,
                synthese,
                BASE_JURIDIQUE,
                AVERT_AVOCAT_BE);
    }

    private static void validateInputs(AuditoratTravailBeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Requête requise");
        }
        if (request.natureFait() == null) {
            throw new IllegalArgumentException(
                    "natureFait est requis");
        }
        if (request.modeSaisineEnvisage() == null) {
            throw new IllegalArgumentException(
                    "modeSaisineEnvisage est requis");
        }
        if (request.faitsPrescrits() == null) {
            throw new IllegalArgumentException(
                    "faitsPrescrits est requis");
        }
        if (request.inspectionDejaSaisie() == null) {
            throw new IllegalArgumentException(
                    "inspectionDejaSaisie est requis");
        }
        if (request.plainteCivileDeposee() == null) {
            throw new IllegalArgumentException(
                    "plainteCivileDeposee est requis");
        }
        if (request.urgenceSecuritePersonnes() == null) {
            throw new IllegalArgumentException(
                    "urgenceSecuritePersonnes est requis");
        }
        if (request.recoursPenalEnvisage() == null) {
            throw new IllegalArgumentException(
                    "recoursPenalEnvisage est requis");
        }
        if (request.employeurPersonneMorale() == null) {
            throw new IllegalArgumentException(
                    "employeurPersonneMorale est requis");
        }
    }
}
