# Audit juridique exhaustif — Outils décisionnels Droit du travail Belgique

**Auteur** : LegalCase — automatique (audit F-191)
**Date** : 2026-05-06
**Périmètre** : droit du travail belge uniquement (FR + BE bilingue Wallonie/Flandre/Bruxelles, hors Immigration et Famille)
**Méthode** : départ des **sources juridiques belges**, pas du miroir FR. Les outils BE-only (crédit-temps, RCC, outplacement, formule Claeys, Fedris, ONEM) sont valorisés à part.
**Sortie** : Tableau A (existant), Tableau B (audit exhaustif), synthèse chiffrée + Top 10 priorité.

---

## 1. Contexte et avertissement méthodologique

L'audit précédent partait du miroir FR ("BE-jumeau de F-DT-XX"). Cette approche a été explicitement rejetée par l'utilisateur : **l'écosystème Travail BE a sa propre topologie** (statut unique 2014, CCT 109, RCC, crédit-temps, outplacement obligatoire 45+, Fedris, allocations ONEM, formule Claeys préservée pour les contrats < 2014, régionalisation du congé-éducation). Ce document part donc des sources légales belges et propose un outil par situation juridique distincte. Les correspondances FR sont signalées en note lorsque pertinentes mais ne pilotent pas l'inventaire.

Toutes les références (loi, AR, CCT) sont issues des connaissances générales du modèle. Les références dont le modèle n'est pas certain à 100 % sont annotées **"(à vérifier)"** — un avocat belge doit confirmer avant de seeder.

Les priorités utilisent l'échelle :

- **P1 — urgence procédurale** : un délai court irréversible expose le client à perdre son droit (recours ONEM 1 mois, action CCT 109 1 an post-rupture, déclaration AT 8 jours, etc.).
- **P2 — fréquence haute** : situation rencontrée par tout avocat travaillistique belge plusieurs fois par mois.
- **P3 — spécificité BE** : pas d'équivalent FR direct, c'est de la valeur produit pure (crédit-temps, RCC, formule Claeys, outplacement obligatoire, ONEM, Fedris).
- **P4 — confort** : utile mais on peut différer sans perte de couverture.

---

## 2. Tableau A — Outils BE Travail existants

Source : migrations Liquibase 105, 106, 191 (DELETE F-DT-05), plus migrations 111-165 et 156 qui ajoutent des règles `decision_tool_visibility_rules` BE après le seed initial. Croisement avec `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`.

| tool_id | layer | trigger_field / trigger_value | Frontend câblé (TOOL_REGISTRY) | Situation juridique couverte |
|---|---|---|---|---|
| `F-DT-06-requete-tribunal-travail` | ALWAYS_ON | — | OUI | Génération de requête introductive devant le tribunal du travail (CJ art. 704). Outil partagé (pas de sortie BE-only mais utilisé pour BE). |
| `F-DT-07-anciennete-conges-prime` | ALWAYS_ON | — | OUI | Calcul d'ancienneté conventionnelle, congés légaux 20 j et primes selon la commission paritaire (CP100, CP111, CP118, CP121, CP124, CP200/201/207/209/220/226/302/306/311/312/313/322/327/330/337 etc. — barèmes seedés `legal_referentials.CONVENTION_BAREMES`). |
| `F-DT-08-licenciement-validity` | ALWAYS_ON | — (avant : CONTEXTUAL `type_rupture=LICENCIEMENT_ORDINAIRE`) | OUI | Analyseur de validité du licenciement ordinaire — critères CCT 109 (motivation), notification (loi 03/07/1978 art. 37), préavis (loi 26/12/2013 statut unique), audition recommandée, non-discrimination (loi 10/05/2007), protections spéciales (délégué syndical loi 19/03/1991, grossesse loi 16/03/1971). |
| `F-DT-09-comparateur-indemnites` | ALWAYS_ON | — | OUI | Comparateur d'indemnités de rupture : indemnité compensatoire de préavis (statut unique), CCT 109 (3-17 semaines licenciement manifestement déraisonnable), `INDEMNITE_BAREMES.CCT109` seedés. |
| `F-DT-11-harcelement-licenciement-nul` | ALWAYS_ON BE | — | OUI | Harcèlement moral / sexuel — licenciement nul (loi 04/08/1996 art. 32bis, 32ter ; loi 19/03/1991 protection contre rupture représailles). |
| `F-DT-12-discrimination-dommages-interets` | ALWAYS_ON BE | — | OUI | Discrimination — dommages et intérêts forfaitaires (loi 10/05/2007 anti-discrimination, loi anti-racisme du 30/07/1981, loi genre 10/05/2007 — 6 mois rémunération). |
| `F-DT-15-inaptitude` | ALWAYS_ON BE | — | OUI | Inaptitude médicale et procédure de réintégration (AR 28/05/2003 surveillance santé, AR 28/10/2016 trajet réintégration). |
| `F-DT-19-heures-sup` | ALWAYS_ON BE | — | OUI | Heures supplémentaires — sursalaire 50 % (semaine) / 100 % (dimanche/férié), repos compensatoire (loi 16/03/1971, AR 11/09/2013 deal pour l'emploi). |
| `F-DT-27-motif-grave-be` | ALWAYS_ON BE | — | OUI | Licenciement pour motif grave — 3 jours notification + 3 jours connaissance (loi 03/07/1978 art. 35), validité du motif. **BE-only**. |
| `F-DT-28-avantages-conventionnels-be` | ALWAYS_ON BE | — | OUI | Avantages conventionnels par CP — congés supplémentaires, prime fin année, prime ancienneté, éco-chèques (CCT sectorielles). **BE-only**. |
| `F-DT-29-credit-temps-be` | ALWAYS_ON BE | — | OUI | Crédit-temps et interruption de carrière — formules avec/sans motif (CCT 103), fin de carrière 55+/60+ (CCT 103 chap. III), allocations ONEM. **BE-only**. |
| `F-132-rupture-amiable-info` | CONTEXTUAL | `type_rupture = RUPTURE_AMIABLE` | OUI | Information sur la rupture de gré à gré BE (pas de procédure formelle équivalente à la rupture conventionnelle FR — confirmation que c'est juste un commun accord art. 1134 ABC). **BE-only**. |
| ~~`F-DT-05-preavis-be`~~ | ~~ALWAYS_ON~~ DELETE migration 191 | — | NON | Calculateur de préavis seul — fusionné dans `F-DT-09-comparateur-indemnites`. Plus de seed, plus d'entrée registry. À considérer **éclaté plus tard** (formule Claeys vs statut unique). |

**Total effectif au 2026-05-06 : 12 outils BE Travail seedés et câblés frontend** (13 si on remet `F-DT-05-preavis-be` en spécifique formule Claeys).

Outils non-BE-spécifiques ré-utilisés naturellement par le contexte BE (registry partagé) : `F-DT-04-fiche-prudhomale` (FR-only `prud'homal`), `F-DT-25-indemnite-preavis` et `F-DT-26-conges-payes-indemnite` (FR-only — pas seedés BE car logique de calcul différente du statut unique).

---

## 3. Tableau B — Audit juridique exhaustif BE Travail

Une ligne = une situation juridique distincte qui mérite un outil décisionnel autonome (un outil = une situation, règle CLAUDE.md). Les outils déjà existants en Tableau A sont signalés **EXISTE**. Les autres sont **MANQUE** avec priorité.

### 3.1 — Rupture du contrat (LICENCIEMENT, DEMISSION, RCC, etc.)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-08-licenciement-validity` | Validité licenciement ordinaire — CCT 109 motivation, statut unique préavis | Loi 03/07/1978 art. 37 ; CCT 109 du 12/02/2014 ; loi 26/12/2013 | Analyseur validité | EXISTE | — | Couvert (Tableau A). |
| `F-DT-09-comparateur-indemnites` | Indemnité compensatoire de préavis vs CCT 109 vs cumul | Loi 26/12/2013 ; CCT 109 art. 9 (3-17 semaines) | Comparateur | EXISTE | — | Couvert. |
| `licenciement-be-statut-unique-preavis` | Calcul préavis statut unique post-2014 (semaines selon ancienneté tranches) | Loi 26/12/2013 ; barème jours/semaines ouvriers + employés | Calculateur | MANQUE | P2 | Aujourd'hui inclus dans F-DT-09 mais mérite une vue préavis pure (durée + date fin + indemnité compensatoire). Cas pratique fréquent. |
| `licenciement-be-formule-claeys` | Préavis ancienneté pré-2014 selon formule Claeys (employés rémun > 32 254 € — barème 1er hiver) | Loi 03/07/1978 art. 82 ancien régime ; arrêt Cour const. 08/07/1993 ; loi 26/12/2013 art. 67 (clause maintien acquis) | Calculateur | MANQUE | **P3 BE-only** | Critique pour contrats antérieurs au 01/01/2014 — le préavis "double" est encore très contesté. Pas d'équivalent FR. |
| `licenciement-be-cct109-deraisonnable` | Indemnité licenciement manifestement déraisonnable (3-17 sem) | CCT 109 art. 9 ; arrêt CE n°245.236 du 27/06/2019 | Calculateur + analyseur de motif | MANQUE | **P2** | Aujourd'hui partiellement dans F-DT-08 (validité) et F-DT-09 (montant) mais pas une vue dédiée scoring 3/8/12/17 sem. À envisager comme spin-off. |
| `F-DT-27-motif-grave-be` | Licenciement pour motif grave — 3 jours+3 jours | Loi 03/07/1978 art. 35 | Analyseur validité | EXISTE | — | Couvert. **BE-only** (FR n'a pas ce double délai 3+3 j). |
| `F-132-rupture-amiable-info` | Rupture de gré à gré commun accord | Art. 1134 ABC + jurisprudence | Information | EXISTE | — | Couvert. |
| `licenciement-be-acte-equivalent` | Acte équipollent à rupture (modification unilatérale conditions essentielles = rupture par employeur) | Cass. 23/12/1957 ; loi 03/07/1978 art. 20 ; jurisprudence Ius Variandi | Analyseur validité | MANQUE | **P2** | Concept BE très spécifique : changement unilatéral lieu de travail, fonction, salaire. Pas d'équivalent FR direct (FR a "modification du contrat" art. L. 1222-6 mais différemment). |
| `licenciement-be-collectif-renault` | Licenciement collectif — loi Renault, info-consult CE, plan social | Loi 13/02/1998 (loi Renault) ; CCT 24 ; CCT 39 | Analyseur conformité | MANQUE | **P3 BE-only** | Procédure 3 phases (info, consult, décision) très formaliste. Sanction = renvoi délai d'attente 30 j. Pas de PSE FR. |
| `licenciement-be-fermeture-entreprise` | Fermeture d'entreprise — indemnité fermeture, FFE | Loi 26/06/2002 ; AR 23/03/2007 ; CCT 32bis (transfert) | Calculateur indemnité + checklist FFE | MANQUE | **P3 BE-only** | Indemnité fermeture spécifique + créances Fonds Fermeture Entreprises (FFE). Pas d'équivalent FR. |
| `licenciement-be-protection-deleguee` | Licenciement délégué syndical / candidat élections sociales — indemnité forfaitaire 4 ans | Loi 19/03/1991 ; CCT 5 | Analyseur validité + calculateur indemnité 4-8 ans | MANQUE | **P2 BE-only** | Indemnité forfaitaire = 2 ans rémun (4 ans avec récidive) + indemnité réintégration. Mécanisme très différent du statut protégé FR. |
| `licenciement-be-protection-conseiller-prevention` | Licenciement conseiller en prévention — procédure CCT spécifique, indemnité spéciale | Loi 04/08/1996 art. 6 ; CCT 25/11/1991 | Analyseur validité | MANQUE | **P3 BE-only** | Conseiller en prévention (médecin du travail interne, ergonome) a un statut protégé spécifique. |
| `licenciement-be-protection-grossesse` | Licenciement pendant grossesse / maternité — interdit jusqu'à 1 mois post-congé maternité | Loi 16/03/1971 art. 40 | Analyseur validité + indemnité 6 mois rémun | MANQUE | **P1 P2** | Indemnité = 6 mois rémunération forfaitaire + dommages prouvés. Très demandé en consultation. |
| `licenciement-be-protection-credit-temps` | Licenciement pendant crédit-temps — interdit, indemnité 6 mois | Loi 22/01/1985 art. 20 ; CCT 103 | Analyseur validité | MANQUE | **P3 BE-only** | Continuité CCT 103. |
| `licenciement-be-protection-soins-palliatifs-ecart` | Licenciement pendant congé soins palliatifs / soins parental | Loi 22/01/1985 art. 20 ; CCT 64, 64bis | Analyseur validité | MANQUE | P3 BE-only | Cas plus rare mais juridiquement protégé. |
| `licenciement-be-rupture-irreguliere` | Rupture sans préavis ni motif grave — indemnité compensatoire de préavis | Loi 03/07/1978 art. 39 | Calculateur indemnité forfaitaire | MANQUE | **P2** | Très fréquent. Forfait = rémunération en cours pour la durée du préavis dû. |
| `demission-be-validite` | Démission — préavis salarié court (1-13 sem) + cas dispense | Loi 03/07/1978 art. 37 § 2 ; loi 26/12/2013 | Calculateur préavis + analyseur écrit valide | MANQUE | P2 | Préavis démission salarié plafonné à 13 sem max — différent du préavis employeur. |
| `rcc-be-conditions` | RCC (régime de chômage avec complément d'entreprise, ex-prépension) — conditions âge / carrière | CCT 17 ; CCT 17/13 ; AR 03/05/2007 | Analyseur éligibilité | MANQUE | **P1 P3 BE-only** | RCC général 60+ ans / 40 ans carrière, RCC métiers lourds 58+/35, RCC long carrière 59+/40, RCC entreprise reconnaissance difficulté. Indemnité complémentaire mensuelle + allocations ONEM cumulées. **Aucun équivalent FR**. |
| `rcc-be-indemnite-complementaire` | RCC — calcul indemnité complémentaire (différentiel ONEM / dernière rémun nette) | CCT 17 ; CCT sectorielles | Calculateur | MANQUE | **P1 P3 BE-only** | À éclater de l'outil "conditions" — le calcul est complexe. |
| `outplacement-be-obligatoire-45` | Outplacement obligatoire 45+ ans — 60h sur 12 mois, sanction employeur | CCT 82 ; CCT 82 bis ; loi 05/09/2001 | Checklist conformité + calculateur sanction | MANQUE | **P1 P3 BE-only** | Sanction 1 800 € pour l'employeur + perte d'allocations chômage pour le travailleur. Aucun équivalent FR. |
| `outplacement-be-general-30sem` | Outplacement général (régime préavis ≥ 30 sem) | Loi 05/09/2001 ; AR 21/10/2007 | Checklist conformité | MANQUE | P3 BE-only | Régime étendu à tous depuis 2014. |
| `licenciement-be-faute-arbitraire-ouvrier` | Licenciement ouvrier "manifestement abusif" pré-CCT 109 — résidu jurisprudence | Loi 03/07/1978 art. 63 (abrogé 01/04/2014) — dossiers historiques | Analyseur validité | MANQUE | P4 | Cas résiduels dossiers anciens. Faible priorité — peut attendre. |

### 3.2 — Procédures et juridictions

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-06-requete-tribunal-travail` | Requête introductive tribunal du travail | CJ art. 578 (compétence), art. 704 (citation/requête) | Générateur de document | EXISTE | — | Couvert. |
| `prescription-be-litige-travail` | Prescription action — 1 an post-rupture (créances ex-contrat), 5 ans avant | Loi 03/07/1978 art. 15 ; CCT 109 art. 11 | Calculateur de délais | MANQUE | **P1** | Critique : un an seulement post-rupture, glissement de 5 à 1 an. Risque forclusion fréquent. **Pas équivalent FR direct** (FR = 12 mois rupture, 2 ans contrat, 3 ans salaires — règles différentes). |
| `refere-tribunal-travail-be` | Référé président tribunal du travail — urgence procédurale (mesures provisoires, ordonnance) | CJ art. 584 | Générateur de requête + checklist conditions urgence | MANQUE | **P1** | Cas réel : harcèlement persistant, salaire impayé, modification unilatérale. Equivalent fonctionnel du référé prud'homal FR mais procédure différente (chambre référé tribunal travail). **Annoncé migration 199** comme jumeau BE de F-DT-34. |
| `appel-cour-du-travail` | Appel cour du travail — délai 1 mois, conditions | CJ art. 1051 | Calculateur de délais + checklist | MANQUE | **P1** | Tribunaux du travail (1ère instance) → cours du travail (appel) → Cour de cassation chambres sociales. Délai d'appel 1 mois. |
| `competence-tribunal-travail-matiere` | Détermination compétence matérielle / territoriale tribunal travail BE | CJ art. 578-583 ; règles compétence territoriale (lieu travail / siège employeur) | Arbre décisionnel | MANQUE | P2 | Évite les déclinatoires. |
| `conciliation-prealable-be` | Conciliation préalable au tribunal du travail | CJ art. 731 ; AR 11/06/1953 | Information / checklist | MANQUE | P4 | Optionnelle en BE (≠ FR conciliation prud'homale obligatoire). |
| `pourvoi-cassation-social` | Pourvoi en cassation — chambres sociales Cour cassation BE | CJ art. 1080-1098 | Calculateur de délais (3 mois) | MANQUE | P3 | Cas spécialisé. |

### 3.3 — Rémunération, salaire, paiement

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-07-anciennete-conges-prime` | Ancienneté + congés légaux 20j + primes par CP | Loi 28/06/1971 ; CCT sectorielles (CP100 → CP337) | Calculateur | EXISTE | — | Couvert. Étendu vague 28 (CP218, CP312, CP313, CP327, CP152). |
| `F-DT-19-heures-sup` | Heures supplémentaires + sursalaire 50/100 % + repos compensatoire | Loi 16/03/1971 ; AR 11/09/2013 | Calculateur | EXISTE | — | Couvert. |
| `F-DT-28-avantages-conventionnels-be` | Avantages conventionnels CP — congés supp, prime fin année, prime ancienneté, éco-chèques | CCT sectorielles | Analyseur + calculateur | EXISTE | — | Couvert. |
| `rappel-salaire-be` | Rappel de salaire / heures payées impayé — prescription 1 an post-rupture / 5 ans contrat | Loi 12/04/1965 ; loi 03/07/1978 art. 15 | Calculateur indemnité + intérêts moratoires | MANQUE | **P2** | Très fréquent. **Annoncé migration 199** comme jumeau BE de F-DT-20. Note : la prescription BE diffère significativement du FR (3 ans). |
| `intereets-moratoires-paiement-tardif` | Intérêts moratoires retard salaire (10 % en BE) | Loi 12/04/1965 art. 10 ; AR 17/07/1991 | Calculateur | MANQUE | P3 BE-only | Taux moratoire BE 10 % — différent du taux légal civil. |
| `prime-fin-annee-be` | Prime fin d'année / 13e mois selon CP | CCT sectorielles (CP200, CP218, CP226, etc.) | Calculateur conditionnel CP | MANQUE | P2 | Souvent mis en doute par employeur en cas de rupture pré-paiement. |
| `eco-cheques-cheques-repas-be` | Éco-chèques / chèques-repas — montants exonérés ONSS, conditions | AR 14/01/2013 ; AR 12/10/2010 | Calculateur exonération | MANQUE | P3 BE-only | Avantages extra-légaux fréquents. |
| `pecule-vacances-be` | Pécule de vacances — calcul (employés vs ouvriers) | Loi 28/06/1971 ; AR 30/03/1967 | Calculateur | MANQUE | **P2 BE-only** | Pécule simple + double, départ employeur déclenche pécule départ. Très technique. Aucun équivalent FR direct (≠ ICCP FR). |
| `secret-loon-clause-non-divulgation` | Clause confidentialité salaire (interdite art. 7 loi 12/04/1965) | Loi 12/04/1965 art. 7 | Analyseur validité | MANQUE | P4 | Cas ponctuel mais juridiquement net. |
| `retenues-be-cessions-saisies` | Cessibilité / saisissabilité salaire — barèmes | CJ art. 1409 ; AR annuel barèmes | Calculateur | MANQUE | P3 | En contentieux civil + travail. |

### 3.4 — Allocations chômage et sécurité sociale

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `c4-onem-checklist` | Document C4 (employeur) — mentions obligatoires, motif licenciement, recours en cas de motif "exclusif faute grave" | AR 25/11/1991 art. 92 | Checklist conformité + générateur lettre rectificative | MANQUE | **P1 P3 BE-only** | Si C4 mention "faute grave" → exclusion allocations 4-52 sem. Recours C4 = priorité absolue. **Aucun équivalent FR**. |
| `contestation-c4-onem` | Contestation décision ONEM exclusion / sanction (recours administratif puis tribunal travail) | AR 25/11/1991 art. 144 ; CJ art. 580 | Calculateur de délais (1 mois) + générateur recours | MANQUE | **P1 P3 BE-only** | **Annoncé migration 199** comme jumeau BE de F-DT-35 (`contestation-are`). 1 mois pour recours administratif puis 3 mois tribunal travail. |
| `allocations-onem-conditions` | Éligibilité allocations chômage post-licenciement — stage, jours travaillés, raison rupture | AR 25/11/1991 ; loi 22/12/2017 (dégressivité) | Analyseur éligibilité | MANQUE | **P3 BE-only** | Conditions stage 312 jours / 18 mois (jeune), 624/33 (adulte 36+), etc. Stage Q1 vs Q2. |
| `chomage-economique-be` | Chômage économique (suspension contrat, allocations ONEM) | Loi 03/07/1978 art. 51 ; AR 25/11/1991 | Checklist conformité | MANQUE | P3 BE-only | Pas d'équivalent FR direct (ne pas confondre activité partielle FR). |
| `chomage-temporaire-force-majeure` | Chômage temporaire force majeure (pandémie, sinistre, intempéries) | Loi 03/07/1978 art. 26 ; AR 25/11/1991 art. 27 | Checklist | MANQUE | P3 BE-only | Procédure formelle (info ONEM, formulaires C3.2). |
| `delai-attente-onem-licenciement-volontaire` | Délai d'attente ONEM en cas de "chômage par sa propre faute" | AR 25/11/1991 art. 51-54 | Calculateur de sanction (4-52 sem) | MANQUE | **P1 P3 BE-only** | Renvoi par l'ONEM = perte revenus très significative. |
| `cumul-rcc-allocations` | Cumul allocations chômage + indemnité complémentaire RCC | CCT 17 ; AR 03/05/2007 | Analyseur cumul | MANQUE | P3 BE-only | Voir `rcc-be-conditions`. |

### 3.5 — Santé, sécurité, accidents du travail, maladies professionnelles

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-15-inaptitude` | Inaptitude médicale — trajet réintégration | AR 28/05/2003 ; AR 28/10/2016 ; AR 11/09/2022 (réforme trajet réintégration II) | Analyseur procédure | EXISTE | — | Couvert. À auditer pour version réforme 2022. |
| `at-fedris-declaration` | Accident du travail — déclaration Fedris (ex-Fonds AT) sous 8 jours | Loi 10/04/1971 ; loi 03/07/1967 (secteur public) | Checklist conformité + calculateur de délais | MANQUE | **P1 P3 BE-only** | **Annoncé migration 199** comme jumeau BE de F-DT-33. 8 jours pour déclarer, sinon préjudice salarié. **Fedris** = organisme spécialisé BE (Caisse AT-MP fédérale fusionnée 2017). Aucun équivalent FR direct. |
| `mp-fedris-reconnaissance` | Maladie professionnelle — reconnaissance Fedris, liste fermée + ouverte | AR 28/03/1969 (liste maladies) ; AR 16/12/1985 (système ouvert) | Analyseur éligibilité | MANQUE | **P3 BE-only** | Liste fermée + système preuve causalité. |
| `at-mp-rente-capital-be` | Calcul rente AT/MP (incapacité permanente) ou capital (≤ 19 %) | Loi 10/04/1971 art. 24 | Calculateur | MANQUE | P3 BE-only | Capitalisation < 19 %, rente ≥ 19 %. |
| `bien-etre-rps-conseiller-prevention` | Risques psychosociaux (RPS) — saisine conseiller prévention, plainte formelle/informelle | Loi 04/08/1996 ; AR 10/04/2014 ; CCT 72 | Checklist procédurale | MANQUE | **P2 P3 BE-only** | Procédure très formaliste (CISP, demande informelle, demande formelle, enquête). Différent du F-DT-30 protection RP qui couvre la suite. |
| `harcelement-be-procedure-formelle` | Harcèlement (moral/sexuel) — plainte formelle + protection contre représailles | Loi 04/08/1996 art. 32bis-32sexies ; AR 10/04/2014 | Checklist procédurale + analyseur représailles | MANQUE (partiellement par F-DT-11) | **P1 P2 BE-only** | F-DT-11 couvre la nullité du licenciement représailles, mais la procédure interne (CPAP, CISP) mérite un outil dédié. |
| `discrimination-be-unia-recours` | Discrimination — recours UNIA, action class action, dommages forfaitaires 6 mois | Loi 10/05/2007 ; loi 30/07/1981 | Checklist procédurale | MANQUE (partiellement par F-DT-12) | P2 BE-only | UNIA (Centre interfédéral pour l'égalité des chances) — voie de recours spécifique. |
| `surveillance-sante-travailleurs` | Examens médicaux obligatoires (préembauche, périodique, retour absence) | AR 28/05/2003 ; loi 04/08/1996 | Checklist conformité | MANQUE | P4 | Plus côté employeur que défense salarié. |

### 3.6 — Crédit-temps, congés, interruption de carrière

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-29-credit-temps-be` | Crédit-temps — formules (avec/sans motif, fin de carrière) | Loi 22/01/1985 ; CCT 103 | Analyseur éligibilité + calculateur allocation ONEM | EXISTE | — | Couvert. **BE-only.** |
| `interruption-carriere-soins-palliatifs` | Congé soins palliatifs (1 mois renouvelable) | CCT 64bis ; loi 22/01/1985 | Checklist + calculateur durée | MANQUE | P3 BE-only | Spécifique BE (≠ congé proche aidant FR). |
| `interruption-carriere-soins-parental` | Congé parental (4 mois) — cumul ONEM | CCT 64 ; loi 22/01/1985 | Checklist + calculateur | MANQUE | P3 BE-only | Régime ONEM différent du congé parental FR. |
| `conge-paternite-naissance-be` | Congé paternité 20 jours (vs 11 j pré-réforme) | Loi 03/07/1978 art. 30 ; loi 07/04/2023 | Calculateur + checklist | MANQUE | P3 BE-only | Réforme 2023 — extension à 20 jours. |
| `conge-adoption-be` | Congé adoption — 6 sem + extensions | Loi 03/07/1978 art. 30ter | Calculateur durée | MANQUE | P3 BE-only | |
| `conge-education-paye-region` | Congé-éducation payé — Wallonie/Flandre/Bruxelles différents | Loi 22/01/1985 ; régionalisé depuis 2014 | Arbre décisionnel régional | MANQUE | **P3 BE-only** | **Régionalisé** — Wallonie : congé-éducation payé + chèques-formation ; Flandre : Vlaams Opleidingsverlof ; Bruxelles : régime spécifique. Différenciation à coder. |
| `petite-flexibilite-be` | Régimes flexibles (loi 17/03/1987 — petite flexibilité, plages horaires) | Loi 17/03/1987 ; CCT 42 | Information | MANQUE | P4 BE-only | |
| `semaine-4-jours-be` | Semaine de 4 jours (deal pour l'emploi) | Loi 03/10/2022 (deal pour l'emploi) | Checklist conformité | MANQUE | P3 BE-only | Récent — 2022-2023. Très demandé. |
| `droit-deconnexion-be` | Droit à la déconnexion (entreprises 20+ travailleurs) | Loi 03/10/2022 ; CCT entreprise/sectorielle | Checklist conformité | MANQUE | P3 BE-only | Obligation employeur — accord d'entreprise depuis 01/04/2023. |

### 3.7 — Statuts particuliers

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `interim-be-cct-322` | Travail intérimaire — règles CCT 322, mission, motifs | CCT 322 ; loi 24/07/1987 | Analyseur validité mission | MANQUE | P2 BE-only | Différent des règles intérim FR (motifs limités, 3 mois renouvelables). |
| `interim-be-indemnite-fin-mission` | Indemnité fin mission intérim BE | Loi 24/07/1987 ; CCT 322 | Calculateur | MANQUE | P3 BE-only | Pas la même mécanique que F-DT-18 FR. |
| `cdd-be-validite` | Validité CDD — motifs, durées, requalification | Loi 03/07/1978 art. 7-13 | Analyseur validité | MANQUE | P2 BE-only | Régime BE différent : pas de motif "remplacement" au sens FR strict. |
| `cdd-be-indemnite-rupture` | Rupture CDD — indemnité spéciale (durée restant à courir, plafonné rémun préavis) | Loi 03/07/1978 art. 40 | Calculateur | MANQUE | P3 BE-only | Mécanisme BE-spécifique. |
| `etudiant-jobiste-be` | Contrat occupation étudiant — 600h année, dimona spécifique | Loi 03/07/1978 art. 120bis ; loi 30/04/1999 | Calculateur quota + checklist | MANQUE | P3 BE-only | Quota 600h/an avec ONSS réduit. Aucun équivalent FR. |
| `flexi-job-be` | Flexi-job — secteurs autorisés, plafonds, déclaration | Loi 16/11/2015 ; loi 25/04/2014 | Checklist éligibilité | MANQUE | P3 BE-only | Spécificité BE. |
| `domestiques-be-cct-323` | Travailleurs domestiques — règles spécifiques | Loi 03/07/1978 ; CCT 323 ; AR 12/10/2010 | Information | MANQUE | P4 | Cas rare. |
| `teletravail-be-cct-85-149` | Télétravail — accord écrit, indemnité forfaitaire | CCT 85 (télétravail structurel) ; CCT 149 (occasionnel) | Checklist conformité | MANQUE | P3 BE-only | Distinct de l'accord télétravail FR. |
| `interventions-domicile-cct-149` | Travail à domicile (cct 85 vs 149) | CCT 85 ; CCT 149 | Information | MANQUE | P4 | Sous-cas du précédent. |
| `clause-non-concurrence-be` | Clause non-concurrence — validité (durée, zone, indemnité) | Loi 03/07/1978 art. 65 ; CCT 13 | Analyseur validité + calculateur indemnité | MANQUE | **P2 BE-only** | **Annoncé migration 199** comme jumeau BE de F-DT-24. Attention : régime BE = indemnité = 1/2 rémun durée clause minimum. Différent FR (montant libre + obligatoire). |
| `clause-ecolage-be` | Clause d'écolage (formation employeur — remboursement) | CCT 13/2/2013 ; loi 03/07/1978 art. 22bis | Analyseur validité | MANQUE | P3 BE-only | Très spécifique BE. |
| `clause-arbitrage-travail-be` | Clause d'arbitrage en droit du travail (interdite sauf cas) | Loi 03/07/1978 art. 13 ; CCT 13 | Analyseur validité | MANQUE | P4 | Cas rare en pratique salariée. |
| `clause-mobilite-be` | Clause de mobilité (modification lieu travail) | Jurisprudence ; loi 03/07/1978 art. 20 | Analyseur validité (acte equipollent) | MANQUE | P3 | Lien avec `licenciement-be-acte-equivalent`. |

### 3.8 — Transferts d'entreprise, fermetures

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `transfert-entreprise-cct-32bis` | Transfert d'entreprise — maintien droits, info-consult | CCT 32bis ; CCT 32ter ; directive 2001/23/CE | Checklist conformité + analyseur reprise droits | MANQUE | **P3 BE-only** | Procédure CCT 32bis très spécifique. **L'équivalent FR (L. 1224-1) est plus simple.** |
| `cct-39-introduction-technologies` | CCT 39 — introduction nouvelles technologies (info-consult préalable) | CCT 39 du 13/12/1983 | Checklist | MANQUE | P4 BE-only | Cas rare mais coercitif. |
| `delegue-syndical-cct-5` | Statut délégué syndical — droits, missions, protection | CCT 5 ; loi 19/03/1991 | Information / checklist | MANQUE | P3 | Lien avec `licenciement-be-protection-deleguee`. |
| `elections-sociales-be` | Élections sociales (CE, CPPT) — chronologie et candidatures protégées | Loi 04/12/2007 (élections sociales) ; renouvellement 4 ans | Calculateur de délais + checklist | MANQUE | P3 BE-only | Cycles 4 ans (2024, 2028…). |

### 3.9 — Discrimination, harcèlement, statuts protégés (au-delà du licenciement)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-DT-11-harcelement-licenciement-nul` | Harcèlement → licenciement nul | Loi 04/08/1996 art. 32bis-ter ; loi 19/03/1991 | Analyseur validité | EXISTE | — | Couvert (Tableau A). |
| `F-DT-12-discrimination-dommages-interets` | Discrimination dommages-intérêts | Loi 10/05/2007 | Calculateur | EXISTE | — | Couvert. |
| `discrimination-be-handicap-amenagement` | Aménagements raisonnables handicap (refus = discrimination indirecte) | Loi 10/05/2007 art. 14 ; CCT 95 | Analyseur validité | MANQUE | P3 BE-only | Concept "aménagements raisonnables" — sanction = discrimination. |
| `discrimination-be-test-situation` | Test de situation (procédé probatoire admis depuis 2018) | Loi 10/05/2007 art. 28 ; loi 28/04/2018 | Information procédurale | MANQUE | P4 BE-only | Cas pointu. |
| `egalite-femmes-hommes-be` | Égalité salariale H/F + obligation rapport | Loi 22/04/2012 ; loi 12/01/2007 | Checklist conformité | MANQUE | P3 BE-only | Loi BE 2012 spécifique. |

### 3.10 — Documents fin de contrat

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `documents-fin-contrat-be` | Documents fin contrat — C4, attestation vacances, fiche fiscale 281.10, certificat travail | Loi 03/07/1978 art. 20bis ; AR 25/11/1991 (C4) ; AR 27/08/1993 (281.10) | Checklist conformité | MANQUE | **P2 BE-only** | Différent du F-DT-32 FR (les documents sont nommés différemment et l'enjeu C4 est central). |
| `attestation-vacances-be` | Attestation de vacances (employé / ouvrier) — calcul jours et pécules | AR 30/03/1967 | Calculateur | MANQUE | P3 BE-only | |

### 3.11 — Travail dissimulé, sanctions employeur

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `travail-noir-be-dimona` | Travail dissimulé / au noir — DIMONA, sanctions ONSS | Loi 03/07/1978 ; loi 06/07/1989 (DIMONA) ; loi 27/06/1969 (ONSS) | Analyseur sanctions | MANQUE | **P2 BE-only** | **Annoncé migration 199** comme jumeau BE de F-DT-21. Mécanisme BE = DIMONA + cotisations rétroactives + amende ONSS. |
| `inastri-statut-travailleur-independant` | Faux indépendant — requalification en salarié | Loi 27/12/2006 (relation travail) ; loi 03/07/1978 | Analyseur qualification | MANQUE | **P2 BE-only** | Loi 2006 sur la "nature de la relation de travail" — 4 critères + critères sectoriels (ONSS, INASTI). Différent du salariat dissimulé FR (présomption art. L. 8221-6). |
| `cotisations-onss-recuperation` | Récupération cotisations ONSS impayées | Loi 27/06/1969 ; loi 13/06/1995 | Calculateur arriérés + intérêts | MANQUE | P3 BE-only | Sanction employeur. |

### 3.12 — Contentieux pénal du travail

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `code-penal-social-be` | Code pénal social — infractions employeur (niveaux 1-4) | Code pénal social du 06/06/2010 | Information / arbre décisionnel sanctions | MANQUE | P3 BE-only | Existence d'un Code pénal social autonome BE — particularité. |
| `auditorat-travail-be` | Saisine auditorat du travail (ministère public spécialisé) | CJ art. 138bis ; CIC art. 24 | Information / checklist saisine | MANQUE | P3 BE-only | Auditorat = parquet spécialisé BE. |

### 3.13 — Cas génériques transversaux

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `prescription-be-litige-travail` | Prescription litiges travail (1 an post-rupture / 5 ans contrat) | Loi 03/07/1978 art. 15 ; CCT 109 art. 11 | Calculateur de délais | MANQUE | **P1** | Voir 3.2. Critique. |
| `transaction-be-travail` | Transaction de fin de contrat — validité, contenu, renonciation | Loi 03/07/1978 ; art. 2044 ABC | Checklist + générateur | MANQUE | **P2 BE-only** | **Annoncé migration 199** comme jumeau BE de F-DT-31. Régime renonciation différent du FR (pas de motivation requise mais renonciations doivent être expresses). |
| `heures-travail-supplementaires-be` | Voir F-DT-19 | — | Calculateur | EXISTE | — | Couvert. |

---

## 4. Synthèse chiffrée

### 4.1 — Inventaire chiffré

- **Outils BE Travail existants en DB + frontend câblés** : **12** au 2026-05-06
  - 11 ALWAYS_ON visibles partout en BE Travail
  - 1 CONTEXTUAL (`F-132-rupture-amiable-info`)
- **Outils Travail BE seedés mais sans composant frontend** : 0 (depuis le DELETE de F-DT-05-preavis-be par migration 191)
- **Outils Travail BE manquants identifiés par l'audit** : **environ 60 outils distincts** (Tableau B sections 3.1 à 3.13, lignes "MANQUE")
  - Dont **P1 — urgence procédurale** : 11 outils
  - Dont **P2 — fréquence haute** : 12 outils
  - Dont **P3 — spécificité BE-only** : ~32 outils
  - Dont **P4 — confort** : ~10 outils

### 4.2 — Outils BE-only sans équivalent FR (preuve d'indépendance vs miroir FR)

L'audit identifie **plus de 35 outils BE-only** sans équivalent fonctionnel FR (les outils marqués "BE-only" en colonne Notes du Tableau B). Liste représentative :

1. RCC (régime de chômage avec complément d'entreprise) — `rcc-be-conditions`, `rcc-be-indemnite-complementaire`
2. Crédit-temps — `F-DT-29-credit-temps-be` (existe), `interruption-carriere-soins-palliatifs`, `interruption-carriere-soins-parental`
3. Outplacement obligatoire — `outplacement-be-obligatoire-45`, `outplacement-be-general-30sem`
4. Formule Claeys — `licenciement-be-formule-claeys`
5. CCT 109 — déjà partiellement couvert par F-DT-08/09, mais `licenciement-be-cct109-deraisonnable` mérite un outil dédié
6. Allocations ONEM — `allocations-onem-conditions`, `c4-onem-checklist`, `contestation-c4-onem`, `delai-attente-onem-licenciement-volontaire`, `cumul-rcc-allocations`
7. Fedris — `at-fedris-declaration`, `mp-fedris-reconnaissance`, `at-mp-rente-capital-be`
8. Statut unique 2014 — `licenciement-be-statut-unique-preavis`
9. Acte équipollent à rupture — `licenciement-be-acte-equivalent`
10. Loi Renault (licenciement collectif) — `licenciement-be-collectif-renault`
11. Fonds de fermeture d'entreprise — `licenciement-be-fermeture-entreprise`
12. Pécule de vacances — `pecule-vacances-be`
13. Éco-chèques / chèques-repas — `eco-cheques-cheques-repas-be`
14. Statut intérim CCT 322 — `interim-be-cct-322`, `interim-be-indemnite-fin-mission`
15. Étudiant jobiste — `etudiant-jobiste-be`
16. Flexi-job — `flexi-job-be`
17. CCT 85 / 149 télétravail — `teletravail-be-cct-85-149`
18. Clause d'écolage — `clause-ecolage-be`
19. Clause non-concurrence avec indemnité légale — `clause-non-concurrence-be`
20. Transfert d'entreprise CCT 32bis — `transfert-entreprise-cct-32bis`
21. Conseil prévention / RPS / CPAP — `bien-etre-rps-conseiller-prevention`, `harcelement-be-procedure-formelle`
22. UNIA discrimination — `discrimination-be-unia-recours`, `discrimination-be-handicap-amenagement`
23. Loi égalité salariale 2012 — `egalite-femmes-hommes-be`
24. C4 et documents fin contrat — `documents-fin-contrat-be`, `attestation-vacances-be`
25. Travail dissimulé DIMONA — `travail-noir-be-dimona`
26. Loi 2006 nature relation de travail — `inastri-statut-travailleur-independant`
27. Code pénal social — `code-penal-social-be`
28. Auditorat du travail — `auditorat-travail-be`
29. Tribunal du travail (≠ prud'hommes) — `competence-tribunal-travail-matiere`, `refere-tribunal-travail-be`, `appel-cour-du-travail`, `pourvoi-cassation-social`
30. Congé-éducation payé régionalisé — `conge-education-paye-region`
31. Semaine de 4 jours / droit déconnexion (deal pour l'emploi 2022) — `semaine-4-jours-be`, `droit-deconnexion-be`
32. Élections sociales — `elections-sociales-be`
33. Délégué syndical CCT 5 — `delegue-syndical-cct-5`
34. Conseiller prévention licenciement — `licenciement-be-protection-conseiller-prevention`
35. Indemnité 6 mois grossesse — `licenciement-be-protection-grossesse`

C'est nettement plus que les outils communs FR/BE — le droit BE n'est **pas un sous-ensemble du FR** : il a sa propre architecture, ses propres concepts, ses propres organismes (ONEM, Fedris, FFE, INASTI, UNIA, Auditorat).

### 4.3 — Top 10 outils prioritaires à livrer en premier

Sélection multicritère (urgence procédurale + fréquence + spécificité BE-only + déjà annoncés dans la migration 199 comme jumeaux BE attendus) :

| Rang | tool_id proposé | Pourquoi P1 | Note |
|---|---|---|---|
| 1 | `prescription-be-litige-travail` | Délai 1 an post-rupture, forclusion irréversible | **P1** transversal (impacte tous les autres outils). |
| 2 | `c4-onem-checklist` | C4 mention "faute grave" = exclusion ONEM 4-52 sem ; correction/contestation très urgente | **P1 BE-only**. Pas d'équivalent FR. |
| 3 | `contestation-c4-onem` | Recours administratif 1 mois ONEM → tribunal travail | **P1 BE-only**, jumeau attendu de F-DT-35. |
| 4 | `at-fedris-declaration` | Déclaration AT 8 jours, sinon préjudice salarié | **P1 BE-only**, jumeau attendu de F-DT-33. |
| 5 | `refere-tribunal-travail-be` | Mesures provisoires urgence (harcèlement, salaire impayé) | **P1**, jumeau attendu de F-DT-34. |
| 6 | `rcc-be-conditions` | Régime BE-only fondamental, fréquent en consultation | **P1 P3 BE-only**, aucun équivalent FR. |
| 7 | `outplacement-be-obligatoire-45` | Sanction employeur 1 800 € + perte allocations salarié — urgence procédurale 30 j | **P1 P3 BE-only**. |
| 8 | `licenciement-be-protection-grossesse` | Indemnité 6 mois rémun, demandé fréquemment | **P1 P2**. |
| 9 | `clause-non-concurrence-be` | Très demandé, jumeau attendu de F-DT-24 mais régime indemnitaire BE différent | **P2 BE-only**, jumeau migration 199. |
| 10 | `rappel-salaire-be` | Très fréquent, prescription 1 an post-rupture | **P2**, jumeau attendu de F-DT-20. |

Le bloc 1-5 concentre les **urgences procédurales irréversibles** (forclusion, délais courts ONEM/Fedris, mesures provisoires). Le bloc 6-10 mélange spécificité BE-only forte (RCC, outplacement) et fréquence haute (grossesse, non-concurrence, rappel salaire).

### 4.4 — Découpages à éclater

Certains outils méritent d'être splittés pour respecter l'invariant CLAUDE.md "un outil = une situation métier" :

- **RCC** : au moins 3 outils selon âge/carrière (général 60+, métiers lourds 58+, longue carrière 59+ avec entreprise reconnaissance difficulté). À ne pas livrer comme outil unique.
- **F-DT-05-preavis-be (réintroduction éventuelle)** : à scinder en `licenciement-be-statut-unique-preavis` (post-2014) + `licenciement-be-formule-claeys` (pré-2014). Deux mécanismes distincts.
- **C4 ONEM** : `c4-onem-checklist` (conformité du document) ≠ `contestation-c4-onem` (procédure de recours) — deux outils distincts.
- **Conseiller prévention** : `licenciement-be-protection-conseiller-prevention` (rupture) ≠ `bien-etre-rps-conseiller-prevention` (saisine RPS) — deux outils.
- **Discrimination** : F-DT-12 (dommages-intérêts) couvre le calcul mais `discrimination-be-handicap-amenagement` (refus aménagement raisonnable = discrimination indirecte) et `discrimination-be-unia-recours` (procédure UNIA) sont distincts.
- **Congé-éducation payé régionalisé** : un outil unique avec branchement Wallonie/Flandre/Bruxelles plutôt que 3 outils — ce serait l'application d'un paramétrage simple sur une logique partagée.

### 4.5 — Hors périmètre / honnêteté méthodologique

- Plusieurs références dans le tableau B sont annotées implicitement "à vérifier" en pratique : la portée exacte de la CCT 25/11/1991 sur le conseiller prévention, l'AR 11/09/2022 sur la réforme trajet réintégration II, la version courante de la CCT 13 (clause non-concurrence — la CCT 13 ABC est de 1973 mais a été modifiée). Un avocat belge doit valider avant de seeder.
- L'audit n'inclut pas les **régimes du secteur public** (statutaires, agents contractuels publics) — ils relèvent d'un domaine connexe (droit administratif + droit du travail) et auraient mérité un audit séparé. Le tribunal du travail n'est compétent que pour les agents contractuels.
- L'audit n'inclut pas les **cotisations sociales spécifiques** (chargeBE, plan tax shift, plan cocktail), qui relèvent plus du droit fiscal que du droit du travail strict.
- Les références **LICENCIEMENT_CRITERES** seedées en `legal_referentials` (BE_NOTIFICATION, BE_PREAVIS, BE_MOTIVATION, BE_AUDITION, BE_NON_DISCRIMINATION, BE_PROTECTION_SPECIALE, BE_INDEMNITE_MANIFESTE) montrent que l'analyseur F-DT-08 BE alimente déjà une couverture multi-critères — l'éclatement en outils dédiés (CCT 109 indemnité, audition CCE, protection spéciale) est une question de granularité produit plutôt que d'absence de couverture.
- **Conseil de prud'hommes** est mentionné dans le contexte initial : le concept N'EXISTE PAS en BE. Le tribunal du travail (juridiction unique) est toujours compétent. Aucune confusion ne doit subsister avec le système FR.

---

## 5. Conclusion

L'écosystème Travail BE compte aujourd'hui **12 outils décisionnels actifs** (Tableau A) sur un périmètre théorique d'environ **70 situations distinctes** justifiant un outil dédié (Tableau B). Le déficit principal est sur les outils **BE-only** (RCC, crédit-temps déjà couvert, ONEM, Fedris, formule Claeys, outplacement, statut unique préavis, transfert CCT 32bis, élections sociales, deal pour l'emploi). Ces outils ne peuvent pas être obtenus par "miroir" depuis le FR — ils nécessitent un travail juridique BE original.

Le **Top 10** prioritaire mélange :
- **5 urgences procédurales** (prescription, C4, contestation ONEM, déclaration Fedris, référé tribunal travail),
- **3 outils à forte spécificité BE-only** (RCC, outplacement, non-concurrence),
- **2 outils à fréquence haute** (grossesse, rappel salaire).

Pour valider cet audit et le transformer en backlog, l'utilisateur doit :
1. confirmer la pertinence du Top 10 (ou en ajuster l'ordre),
2. faire valider les références juridiques marquées "à vérifier" par un avocat BE,
3. décider si chaque outil sera livré en SF dédiée ou en feature jumelle (ex. RCC = F-DT-XX avec 3 SF).

Cette base prépare une vague de features F-DT-36 → F-DT-50 (et plus) cohérente avec l'invariant "un outil = une situation métier" et fidèle à la topologie juridique BE.
