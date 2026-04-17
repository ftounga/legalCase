# Plan de test manuel — 12 dossiers × features (incluant F-IA-03 cohérence)

## Prérequis

- App démarrée en local ou staging opérationnel
- 5 workspaces créés :
  - DROIT_DU_TRAVAIL / FRANCE
  - DROIT_DU_TRAVAIL / BELGIQUE
  - DROIT_IMMIGRATION / FRANCE
  - DROIT_FAMILLE / FRANCE
  - DROIT_FAMILLE / BELGIQUE

## Nouveautés F-IA-03 à vérifier partout

À chaque test, contrôler systématiquement :
- **Pré-remplissage IA** : les champs des outils métier sont pré-remplis depuis l'analyse IA
- **Provenance notes** : "Pré-rempli depuis l'analyse IA" visible sous les champs concernés (F-DT-09 type rupture, F-FA-05 valeurs, F-FA-06 mode garde, F-IM-06 recours, F-IM-07 titre)
- **Cohérence IA** : si l'avocat modifie une valeur de manière à diverger de l'IA (ou des points F-96 / réponses aux questions IA), un badge warning/blocker apparaît à côté du champ avec tooltip explicatif
- **Bandeau récap** : compteur "X incohérences (Y bloquantes)" en haut de la section concernée quand il y a ≥ 1 alerte

---

## Test 1 — Dupont (Syntec licenciement FR)

**Dossier :** `dossier-licenciement-syntec-dupont/` (4 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 1.1 | Créer dossier "Dupont", uploader les 4 fichiers | Dossier créé, 4 documents listés |
| 1.2 | Lancer l'analyse IA | Synthèse : faits, risques, timeline |
| 1.3 | Vérifier **pré-remplissage F-IA-01** section Ancienneté | Convention=SYNTEC, date entrée=01/09/2018, salaire=4200€ |
| 1.4 | **SF-IA-01-03** : vérifier pré-cochage de la grille Validité licenciement | Critères FR_CONVOCATION, FR_MOTIVATION, etc. pré-remplis (OUI/NON) depuis l'IA |
| 1.5 | Calculer Ancienneté (F-DT-07) → modifier salaire de 10 % (ex: 4620) | **SF-IA-03-04** : badge orange warning "Incohérence IA (4200€)" visible |
| 1.6 | Revenir à 4200 | Badge disparaît |
| 1.7 | Grille Validité licenciement (F-DT-08) → modifier FR_MOTIVATION vers NON contrairement à l'IA | **SF-IA-03-01** : badge rouge `blocker` "Incohérence IA (OUI)", tooltip justification IA |
| 1.8 | Comparateur indemnités (F-DT-09) : vérifier **SF-DT-09-04** sélecteur type de rupture | Pré-sélectionné `LICENCIEMENT` depuis IA, 3 options FR |
| 1.9 | F-DT-09 : modifier type vers RUPTURE_CONVENTIONNELLE | **SF-IA-03-05** : badge blocker + affichage mode INDEMNITE_SPECIFIQUE avec indemnité légale calculée |
| 1.10 | Revenir LICENCIEMENT + calculer | Fourchette Macron affichée normalement |
| 1.11 | Dashboard (F-IA-02) | Cards agrégées |

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-01, F-IA-02, **SF-IA-01-03, SF-DT-09-04, SF-IA-03-01/04/05**

---

## Test 2 — Martin (BTP rupture conventionnelle FR)

**Dossier :** `dossier-rupture-btp-martin/` (2 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 2.1 | Créer dossier "Martin", uploader 2 fichiers, lancer analyse IA | Synthèse avec rupture conventionnelle détectée dans les faits |
| 2.2 | Ouvrir **Barème d'ancienneté et congés conventionnels (F-DT-07)** → Calculer | Convention=BTP, 15 ans, congés 25+4=29j, prime 12% |
| 2.3 | Ouvrir **Comparateur jurisprudentiel d'indemnités (F-DT-09)** | Type `RUPTURE_CONVENTIONNELLE` pré-sélectionné depuis l'IA |
| 2.4 | Cliquer Comparer | Mode **INDEMNITE_SPECIFIQUE** : indemnité légale calculée = (¼ × min(10, anc) + ⅓ × max(0, anc-10)) × salaire, message "doit être ≥ indemnité légale" |
| 2.5 | Modifier type → LICENCIEMENT → Comparer | Bascule vers fourchette Macron, messages contextuels mis à jour. **SF-DT-10-04** : le bloc _Validité de la rupture conventionnelle_ disparaît et _Validité du licenciement_ apparaît |
| 2.6 | Revenir à RUPTURE_CONVENTIONNELLE | **Cohérence IA sur le comparateur (SF-IA-03-05)** : aucune alerte (match IA). **SF-DT-10-04** : le bloc _Validité du licenciement_ disparaît à nouveau, _Validité de la rupture conventionnelle_ réapparaît |
| 2.7 | Modifier ancienneté → 8 ans (vs 15 IA) | **Cohérence IA sur le comparateur (SF-IA-03-05)** : badge warning numérique (écart > 0,5 an) sur ancienneté |
| 2.8 | Ouvrir **Analyse de validité de la rupture conventionnelle (F-DT-10)** | Bloc affiché (SF-DT-10-04). Ne doit **pas** voir le bloc _Validité du licenciement_ (F-DT-08) |
| 2.9 | Cocher : `RC_CONSENTEMENT=OUI`, `RC_DELAI_RETRACTATION=OUI`, `RC_HOMOLOGATION=OUI`, `RC_ASSISTANCE=OUI`, `RC_INDEMNITE=NON`, `RC_ENTRETIENS=OUI` → Analyser | Verdict **INVALIDE** (bloquant sur RC_INDEMNITE), score ≥ 15, jauge rouge, message base juridique "art. L1237-13" sur RC_INDEMNITE |
| 2.10 | Cliquer Modifier → corriger `RC_INDEMNITE=OUI` → Analyser | Verdict **VALIDE**, score 0, jauge verte |
| 2.11 | Remonter au **Tableau de bord décisionnel (F-IA-02)** en haut de page | Cards _Ancienneté_ et _Indemnités_ reflètent les dernières valeurs. **Refresh auto (SF-IA-02-03)** : après chaque Comparer / Analyser ci-dessus, les cards se sont mises à jour **sans reload** |

**Features testées :**
- **Barème d'ancienneté et congés conventionnels (F-DT-07)**
- **Comparateur jurisprudentiel d'indemnités (F-DT-09)** + **SF-DT-09-04 type de rupture pré-rempli** + **SF-DT-09-05 fiabilisation extraction type**
- **Analyse de validité de la rupture conventionnelle (F-DT-10)** + **SF-DT-10-01/02/03/04**
- **Tableau de bord décisionnel (F-IA-02)** + **SF-IA-02-03 refresh auto**
- **Cohérence IA sur le comparateur d'indemnités (SF-IA-03-05)** — volet numérique ancienneté
- **Orchestration UX F-DT-08 / F-DT-10 (SF-DT-10-04)** — le bloc validité du licenciement ne s'affiche pas pour une rupture conventionnelle

**Différences vs Test 1 :**
- Rupture conventionnelle détectée (test 1 = licenciement) → déclenche `INDEMNITE_SPECIFIQUE` et **F-DT-10** à la place de **F-DT-08**
- Cohérence IA testée sur ancienneté numérique (test 1 = salaire + type_rupture)
- Convention BTP (test 1 = SYNTEC)

---

## Test 3 — Janssen (licenciement économique CP 200 BE)

**Dossier :** `dossier-licenciement-cp200-janssen/` (3 fichiers)
**Workspace requis :** DROIT_DU_TRAVAIL / **BELGIQUE**

### Contexte du dossier

- Pieter JANSSEN, 37 ans, Comptable senior
- Employeur : FinConsult SPRL (Bruxelles) — cabinet de comptabilité
- Commission paritaire : **CP 200** (auxiliaire pour employés)
- Embauche : 01/04/2018 | Notification licenciement : 05/01/2026 | Fin contrat : 08/08/2026
- **Ancienneté effective : 7 ans 9 mois**
- Salaire final : **3 100 € brut/mois** (37 200 €/an)
- Type de rupture : **licenciement économique** (motif = restructuration du département comptabilité, CCT 109 art. 4)
- Préavis : 30 semaines (statut unique loi 26/12/2013)

**Enjeu** : dossier apparemment *bien géré* côté employeur (LRAR, motivation conforme CCT 109, préavis légal). C'est un cas de **licenciement valide côté procédure** — le test vérifie que l'IA le reconnaît comme tel, et que les outils métier fonctionnent en mode Belgique.

### Setup (une seule fois)

1. Se connecter sur **https://staging.legalcase.ng-itconsulting.com** (ou le local)
2. Sélectionner (ou créer) un workspace avec `domaine = DROIT_DU_TRAVAIL` et `pays = BELGIQUE`
3. Créer un nouveau dossier intitulé **"Janssen CP 200"**
4. Uploader les 3 fichiers `01-contrat-travail.txt`, `02-lettre-licenciement.txt`, `03-attestation-anciennete.txt`
5. Lancer l'analyse IA complète (attendre synthèse + questions + re-synthèse enrichie)

### Valeurs attendues après analyse IA

| Donnée | Valeur attendue | Source dans le dossier |
|---|---|---|
| Pays détecté | BELGIQUE | adresses BE, CP 200, CCT 109 |
| Convention collective | CP 200 | Article 1 contrat |
| Type de rupture | `LICENCIEMENT_ORDINAIRE` | Lettre licenciement §1 "restructuration" |
| Date entrée | 01/04/2018 | Article 1 contrat |
| Date rupture | 05/01/2026 | Lettre licenciement (envoi LRAR) |
| Ancienneté | 7 ans 9 mois (ou 93 mois) | Attestation §introduction |
| Salaire brut mensuel | 3 100 € | Article 5 contrat + historique salarial attestation |
| Critères F-DT-08 | `BE_NOTIFICATION=OUI`, `BE_PREAVIS=OUI`, `BE_MOTIVATION=OUI` | LRAR, 30 semaines, CCT 109 art. 4 |

### Étapes séquentielles

| Étape | Action | Vérification |
|-------|--------|-------------|
| 3.1 | Ouvrir le dossier → lire la synthèse | Références belges (CP 200, statut unique, CCT 109, Moniteur belge). Timeline ≥ 3 événements (embauche, promotion 2020, notification). Score de risque = **Faible** |
| 3.2 | Dérouler **Validité du licenciement (F-DT-08)** | ✨ **Champ Pays = input disabled "Belgique"** (fix PR #357). Critères listés = `BE_*` uniquement. **SF-IA-01-03** : `BE_NOTIFICATION`, `BE_PREAVIS`, `BE_MOTIVATION` pré-cochés OUI depuis l'IA |
| 3.3 | Dérouler **Barème d'ancienneté et congés conventionnels (F-DT-07)** → Calculer | Convention=CP 200, ancienneté=7 ans 9 mois (ou 8 ans arrondi), congés 20+2=22 jours, prime ~4 %, aucun écart détecté |
| 3.4 | **SF-IA-03-04** : modifier "Congés totaux" → 30 jours | Badge orange warning "Incohérence IA (22 jours)" avec tooltip. Revenir à 22 → badge disparaît |
| 3.5 | Dérouler **Comparateur d'indemnités (F-DT-09)** → Comparer | ✨ **Champ Pays = input disabled "Belgique"** (fix PR #357). **SF-DT-09-04** : type `LICENCIEMENT_ORDINAIRE` pré-sélectionné. Liste du type propose **2 options BE** uniquement (`LICENCIEMENT_ORDINAIRE`, `RUPTURE_AMIABLE`). Mode d'affichage = **CCT_109**, fourchette 3-17 semaines |
| 3.6 | Modifier type vers `RUPTURE_AMIABLE` → Comparer | Mode **NEGOCIATION_LIBRE**, pas de fourchette, message "Le montant résulte de l'accord entre les parties". **SF-IA-03-05** : badge blocker "Incohérence IA (LICENCIEMENT_ORDINAIRE)" sur le sélecteur |
| 3.7 | Revenir à `LICENCIEMENT_ORDINAIRE`. Dans F-DT-08, modifier `BE_NOTIFICATION` de OUI vers NON | **SF-IA-03-01/02** : badge rouge blocker (critère bloquant), tooltip avec justification IA citant la LRAR |
| 3.8 | Remonter en haut de page → Tableau de bord (F-IA-02) | Cards Licenciement + Indemnités + Ancienneté agrégées. `country=BELGIQUE` partout, aucune mention FRANCE |

### ✨ Check-list spécifique au fix pays workspace (PR #357)

À cocher explicitement :

- [ ] Étape 3.2 — champ Pays dans F-DT-08 : **input disabled "Belgique"**, pas de `<mat-select>`
- [ ] Étape 3.5 — champ Pays dans F-DT-09 : **input disabled "Belgique"**, pas de `<mat-select>`
- [ ] La liste des critères F-DT-08 contient uniquement les `BE_*` (aucun `FR_*`)
- [ ] La liste des types de rupture F-DT-09 contient uniquement les options BE (`LICENCIEMENT_ORDINAIRE`, `RUPTURE_AMIABLE`)
- [ ] Aucun moyen dans l'UI de passer à FRANCE (le pays vient du workspace uniquement)

### Cas d'échec à signaler

- Si un champ "Pays" affiche un `<mat-select>` → régression du fix #357
- Si la liste des critères F-DT-08 contient des `FR_*` → bug de propagation workspaceCountry
- Si la liste F-DT-09 propose `LICENCIEMENT` / `LICENCIEMENT_ECONOMIQUE` / `RUPTURE_CONVENTIONNELLE` → pays FRANCE résiduel
- Si l'IA détecte `typeRupture = LICENCIEMENT` (FR) au lieu de `LICENCIEMENT_ORDINAIRE` (BE) → problème de fiabilisation extraction (SF-DT-09-05 à revérifier)

**Features testées :** F-DT-07, F-DT-08, F-DT-09, F-IA-01, F-IA-02, **SF-IA-01-03, SF-DT-09-04, SF-IA-03-01/02/04/05**, **Fix PR #357 (pays workspace verrouillé)**

---

## Test 4 — Peeters (CP124 faute grave BE)

**Dossier :** `dossier-faute-grave-cp124-peeters/` (3 fichiers)
**Workspace :** DROIT_DU_TRAVAIL / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 4.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 4.2 | **SF-IA-01-03** : Validité licenciement pré-cochée | BE_PREAVIS=NON automatique (faute grave) |
| 4.3 | Modifier BE_PREAVIS vers OUI contrairement à l'IA | **SF-IA-03-01** : blocker (critère bloquant) + tooltip justification IA |
| 4.4 | Ancienneté (F-DT-07) | CP124, 3 ans, pas de prime |
| 4.5 | Dashboard | Card licenciement avec alerte rouge |

**Features testées :** F-DT-07, F-DT-08, F-IA-02, **SF-IA-01-03, SF-IA-03-01**

---

## Test 5 — Diallo (titre salarié FR)

**Dossier :** `dossier-titre-salarie-diallo/` (4 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 5.1 | Créer dossier, uploader 4 fichiers, analyser | `type_titre_sejour` extrait |
| 5.2 | **SF-IM-05-04** : ouvrir Titre de séjour recommandé (F-IM-05) | `motif` pré-rempli (TRAVAIL) via mapping CODE_TO_MOTIF, `nationaliteUe` booléen pré-sélectionné |
| 5.3 | **SF-IA-03-09** : modifier motif vers ETUDES | Badge warning "Incohérence IA (TRAVAIL)" |
| 5.4 | Revenir TRAVAIL → résoudre | Résultat : VLS-TS salarié / CST salarié |
| 5.5 | **SF-IA-03-11** : ouvrir Droit au travail (F-IM-07) | Titre `VLS_TS_SALARIE` pré-rempli + provenance note |
| 5.6 | Modifier titre vers CARTE_RESIDENT | Warning "Incohérence IA (VLS_TS_SALARIE)" |
| 5.7 | Revenir → résoudre → OUI, obligations employeur | Autorisé, DPAE, vérification préfecture |
| 5.8 | Checklist pièces (F-IM-01) | Checklist type TITRE_SALARIE/FRANCE |
| 5.9 | Dashboard | Cards titre + droit travail OUI |

**Features testées :** F-IM-01, F-IM-05, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IA-03-09/11**

---

## Test 6 — Yilmaz (asile Belgique)

**Dossier :** `dossier-asile-belgique-yilmaz/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE (workspace FR pour immigration)

| Étape | Action | Vérification |
|-------|--------|-------------|
| 6.1 | Créer dossier, uploader 3 fichiers, analyser | type_procedure = DEMANDE_ASILE |
| 6.2 | Titre recommandé (F-IM-05) | Attestation d'immatriculation / RECEPISSE_ASILE |
| 6.3 | **SF-IM-06-04** : ouvrir Recours (F-IM-06) | Type `RECOURS_CNDA` pré-sélectionné + date notification pré-remplie, 2 provenance notes visibles |
| 6.4 | **SF-IA-03-10** : modifier type vers RECOURS_GRACIEUX_PREFET | Badge warning "Incohérence IA (RECOURS_CNDA)" |
| 6.5 | Modifier date notification de +10 jours vs IA | Badge warning "écart de 10 jours" (seuil 7j strict) |
| 6.6 | Revenir → générer | Délai 30 jours, date limite calculée |
| 6.7 | **SF-IA-03-11** : Droit au travail (F-IM-07) | Pré-remplissage titre, CONDITIONNEL après 4 mois |
| 6.8 | Dashboard | Card recours + droit travail conditionnel |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IM-06-04, SF-IA-03-09/10/11**

---

## Test 7 — Chen (regroupement familial FR)

**Dossier :** `dossier-regroupement-familial-chen/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 7.1 | Créer dossier, uploader 3 fichiers, analyser | Motif FAMILLE |
| 7.2 | **SF-IM-05-04** : Titre recommandé | `motif=FAMILLE` pré-sélectionné, CST_VPF recommandé |
| 7.3 | **SF-IA-03-09** : modifier motif vers TRAVAIL | Warning "Incohérence IA (FAMILLE)" |
| 7.4 | **SF-IA-03-11** : Droit au travail → modifier titre vers VLS_TS_SALARIE | Warning "Incohérence IA (CST_VPF)" |
| 7.5 | Revenir → résoudre | OUI plein droit |
| 7.6 | Dashboard | Cards titre + droit travail OUI (vert) |

**Features testées :** F-IM-05, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IA-03-09/11**

---

## Test 8 — Kowalski (permis unique BE)

**Dossier :** `dossier-permis-unique-kowalski/` (3 fichiers)
**Workspace :** DROIT_IMMIGRATION / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 8.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 8.2 | Titre recommandé (F-IM-05) | UE → libre circulation / carte B |
| 8.3 | **SF-IM-06-04** : Recours → vérifier pré-remplissage | `RECOURS_CCE` ou `RECOURS_CGRA` pré-sélectionné selon détection IA |
| 8.4 | **SF-IA-03-10** : modifier vers RECOURS_GRACIEUX_PREFET (code FR sur contexte BE) | Warning "Incohérence IA" |
| 8.5 | **SF-IA-03-11** : Droit au travail → modifier titre BE | Warning si mismatch |
| 8.6 | Dashboard | Cards recours + droit travail |

**Features testées :** F-IM-05, F-IM-06, F-IM-07, F-IA-02, **SF-IM-05-04, SF-IM-06-04, SF-IA-03-09/10/11**

---

## Test 9 — Moreau (divorce amiable FR)

**Dossier :** `dossier-divorce-amiable-moreau/` (4 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 9.1 | Créer dossier, uploader 4 fichiers, analyser | pension_alimentaire, prestation_compensatoire, liquidation extraits |
| 9.2 | **SF-FA-05-04** : ouvrir Partage immobilier | Bouton "Importer depuis l'analyse IA" visible. Cliquer → panneau avec bien + prêt détectés. Sélectionner + Appliquer → champs remplis + 2 provenance notes |
| 9.3 | Modifier valeur vénale de +15 % | **SF-IA-03-08** : warning "Incohérence IA (valeur détectée)" |
| 9.4 | Revenir → Calculer | 350K€, prêt 120K€, 50/50, divorce → droit 1.1% = 2530€ |
| 9.5 | **SF-FA-06-04** : Calendrier garde | `ALTERNEE_FR` pré-sélectionné (mode détaillé IA) + note si pays opposé |
| 9.6 | **SF-IA-03-07** : modifier vers DVH_CLASSIQUE_FR | Warning IA_COARSE (catégorie alternée → non alternée) ou blocker IA si mode détaillé détecté |
| 9.7 | Résidence alternée → 182/183 jours |
| 9.8 | **SF-IA-03-06** : Checklist divorce (F-FA-07) | 7 étapes FR + 9 pièces. Cocher FR_CHOIX_AVOCATS en "FAIT" alors que l'IA détecte NON_COMPLIANT via F-96 → badge blocker |
| 9.9 | Pièce PRESENTE sur FR_ACTE_MARIAGE alors que pieces_manquantes IA l'indique manquante | Badge warning "Incohérence IA" |
| 9.10 | Progression 0% → cocher → progression augmente | Progress bar mise à jour |
| 9.11 | Dashboard | Cards soulte + garde + divorce progression + risk score |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-01, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-06/07/08**

---

## Test 10 — Dubois (divorce contentieux FR)

**Dossier :** `dossier-divorce-contentieux-dubois/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / FRANCE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 10.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 10.2 | **SF-FA-05-04** : Partage immobilier avec import IA | 280K€, prêt 90K€, 60/40 |
| 10.3 | **SF-IA-03-08** : saisir valeur manuelle très différente (+20 %) vs best-match IA | Warning visible |
| 10.4 | **SF-FA-06-04** : Calendrier garde DVH classique | `DVH_CLASSIQUE_FR` pré-sélectionné ou fallback selon IA |
| 10.5 | 249/116 jours | OK |
| 10.6 | Pension alimentaire + prestation compensatoire (existants) | Revenus 5200€ vs 1800€ |
| 10.7 | Dashboard | Cards partage + garde + pension |

**Features testées :** F-FA-05, F-FA-06, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-07/08**

---

## Test 11 — Vermeersch (divorce Belgique)

**Dossier :** `dossier-divorce-belgique-vermeersch/` (3 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 11.1 | Créer dossier, uploader 3 fichiers, analyser | OK |
| 11.2 | Partage immobilier (F-FA-05) | 420K€, prêt 150K€, 50/50, droit BE 1% |
| 11.3 | **SF-FA-06-04** : Calendrier garde → hébergement égalitaire BE | `ALTERNEE_BE` pré-sélectionné |
| 11.4 | **SF-IA-03-07** : modifier vers SECONDAIRE_ELARGI_BE | Warning (différents modes BE) |
| 11.5 | 182/183 jours | OK |
| 11.6 | **SF-IA-03-06** : Checklist divorce BE (6 étapes) | Cocher BE_REQUETE_CONJOINTE + F-96 NON_COMPLIANT → blocker |
| 11.7 | Dashboard | Cards soulte BE + garde + divorce |

**Features testées :** F-FA-05, F-FA-06, F-FA-07, F-IA-02, **SF-FA-05-04, SF-FA-06-04, SF-IA-03-06/07/08**

---

## Test 12 — De Smet (garde seule BE)

**Dossier :** `dossier-garde-seule-desmet/` (2 fichiers)
**Workspace :** DROIT_FAMILLE / BELGIQUE

| Étape | Action | Vérification |
|-------|--------|-------------|
| 12.1 | Créer dossier, uploader 2 fichiers, analyser | OK |
| 12.2 | **SF-FA-06-04** : Calendrier garde | `SECONDAIRE_BE` pré-sélectionné |
| 12.3 | **SF-IA-03-07** : modifier vers ALTERNEE_BE | Warning IA_COARSE (catégorie change) |
| 12.4 | 249/116 jours | OK |
| 12.5 | Pension alimentaire | Revenus 2900€ vs 2100€ |
| 12.6 | Dashboard | Cards garde + pension |

**Features testées :** F-FA-06, F-IA-02, **SF-FA-06-04, SF-IA-03-07**

---

## Tests transversaux (sur chaque dossier)

| Test | Vérification |
|------|-------------|
| **Référentiels admin** (F-REF-01) | Aller dans /referentials → les 9 nouveaux types visibles et éditables |
| **Édition référentiel** | Modifier une valeur → validation IA se déclenche |
| **Pré-remplissage IA** (F-IA-01) | Après analyse, les formulaires outils sont pré-remplis |
| **Dashboard** (F-IA-02) | En haut du dossier, cards adaptatives |
| **Isolation workspace** | Un dossier d'un workspace n'est pas visible depuis un autre |
| **Upsert** | Modifier les paramètres et relancer → résultat précédent remplacé |
| **GET existant** | Recharger la page → l'outil affiche le dernier résultat sauvegardé |
| **Cohérence F-IA-03 — alertes gelées** | Une fois le résultat d'un outil calculé/sauvegardé, modifier la valeur via "Modifier" ne doit pas déclencher de nouvelle alerte tant que le résultat est chargé |
| **Cohérence F-IA-03 — bandeau compteur** | Dans une section, si ≥ 1 alerte, bandeau "X incohérences (Y bloquantes)" en haut. Compteur blockers = 0 sur F-DT-07, F-DT-09 numérique, F-FA-05, F-FA-06 fallback, F-IM-05, F-IM-06, F-IM-07, F-FA-07 pièces. Compteur blockers > 0 sur F-DT-08 et F-FA-07 étapes |
| **Cohérence F-IA-03 — tooltip source** | Survol d'un badge : tooltip indique la source (F-96, Question IA, IA, PIECE_MANQUANTE, MULTI) + justification ou valeur attendue |
| **Provenance notes** | Sous les champs pré-remplis par IA : "Pré-rempli depuis l'analyse IA" visible. Disparaît dès modification manuelle |

---

## Matrice de couverture

### Features métier

| Feature | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 |
|---------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| F-DT-07 Ancienneté | ✓ | ✓ | ✓ | ✓ | | | | | | | | |
| F-DT-08 Licenciement | ✓ | | ✓ | ✓ | | | | | | | | |
| F-DT-09 Indemnités | ✓ | ✓ | ✓ | | | | | | | | | |
| F-IM-01 Checklist pièces | | | | | ✓ | | | | | | | |
| F-IM-05 Titre séjour | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| F-IM-06 Recours | | | | | | ✓ | | ✓ | | | | |
| F-IM-07 Droit travail | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| F-FA-05 Partage immo | | | | | | | | | ✓ | ✓ | ✓ | |
| F-FA-06 Calendrier garde | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| F-FA-07 Divorce checklist | | | | | | | | | ✓ | | ✓ | |
| F-IA-01 Pré-remplissage | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| F-IA-02 Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

### Subfeatures F-IA-03 + préalables (nouveauté)

| SF | Outil | T1 | T2 | T3 | T4 | T5 | T6 | T7 | T8 | T9 | T10 | T11 | T12 |
|----|-------|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| SF-IA-01-03 Prefill grille licenciement | F-DT-08 | ✓ | | ✓ | ✓ | | | | | | | | |
| SF-IA-03-01/02/03 Cohérence F-DT-08 | F-DT-08 | ✓ | | ✓ | ✓ | | | | | | | | |
| SF-IA-03-04 Cohérence F-DT-07 | F-DT-07 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-DT-09-04 Type rupture | F-DT-09 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-IA-03-05 Cohérence F-DT-09 | F-DT-09 | ✓ | ✓ | ✓ | | | | | | | | | |
| SF-IA-03-06 Cohérence F-FA-07 | F-FA-07 | | | | | | | | | ✓ | | ✓ | |
| SF-FA-06-04 Mode garde détaillé | F-FA-06 | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| SF-IA-03-07 Cohérence F-FA-06 | F-FA-06 | | | | | | | | | ✓ | ✓ | ✓ | ✓ |
| SF-FA-05-04 Sélecteur IA bien | F-FA-05 | | | | | | | | | ✓ | ✓ | | |
| SF-IA-03-08 Cohérence F-FA-05 | F-FA-05 | | | | | | | | | ✓ | ✓ | | |
| SF-IM-05-04 Normalisation titre | F-IM-05 | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
| SF-IA-03-09 Cohérence F-IM-05 | F-IM-05 | | | | | ✓ | | ✓ | ✓ | | | | |
| SF-IM-06-04 Prefill recours | F-IM-06 | | | | | | ✓ | | ✓ | | | | |
| SF-IA-03-10 Cohérence F-IM-06 | F-IM-06 | | | | | | ✓ | | ✓ | | | | |
| SF-IA-03-11 Cohérence F-IM-07 | F-IM-07 | | | | | ✓ | ✓ | ✓ | ✓ | | | | |
