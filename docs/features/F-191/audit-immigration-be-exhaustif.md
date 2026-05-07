# Audit juridique exhaustif — Outils décisionnels Droit des étrangers Belgique (Immigration BE)

**Auteur** : LegalCase — automatique (audit F-191, complément à `audit-be-travail-exhaustif.md`)
**Date** : 2026-05-06
**Périmètre** : droit belge des étrangers uniquement (BE, hors Immigration FR, hors Travail BE et Famille BE).
**Méthode** : départ des **sources juridiques belges** (Loi 15/12/1980, AR 08/10/1981, AR 11/06/2018, Loi 18/12/2016, AR 02/09/2018, Code de la nationalité belge 28/06/1984, etc.). Pas de miroir FR. Les outils BE-only sans équivalent fonctionnel direct côté FR (9bis circonstances exceptionnelles, 9ter médical OE, 40ter parent d'enfant Belge, single permit, carte H Brexit, AESM tutelle MENA, recours CCE, détention centre fermé) sont valorisés à part.
**Sortie** : Tableau A (existant), Tableau B (audit exhaustif), Tableau C (audit F-166 Immigration BE — flags IA + bascule ALWAYS_ON → CONTEXTUAL), synthèse chiffrée + Top 10 priorité.

---

## 1. Contexte et avertissement méthodologique

L'écosystème Immigration BE a sa propre topologie. Contrairement au droit français des étrangers (CESEDA structuré par titres + procédures), le droit belge fonctionne autour :

1. d'une **loi cadre unique** — Loi 15/12/1980 sur l'accès au territoire, le séjour, l'établissement et l'éloignement des étrangers — déclinée article par article (art. 9, 9bis, 9ter, 10, 10bis, 10ter, 12, 12bis, 13, 40, 40bis, 40ter, 47/1, 48/1+, 49/2, 51, 52, 56, 57/6, 71, 74) ;
2. d'**annexes administratives standardisées** (Annexe 13 OQT, Annexe 13quinquies OQT + interdiction d'entrée, Annexe 14, Annexe 15, Annexe 26 demande asile, Annexe 35 attestation immatriculation, Annexes 8/8bis/9/9bis/9ter/40ter/41bis/49/50/51) que les avocats nomment dans leurs courriers et requêtes ;
3. d'une **juridiction administrative spécialisée unique** — le **Conseil du Contentieux des Étrangers (CCE)** créé par la loi du 15 septembre 2006 — qui reçoit la totalité des recours en annulation et en suspension contre les décisions de l'Office des Étrangers (OE), du Commissariat général aux réfugiés et apatrides (CGRA) et du Ministre ;
4. d'une **administration fédérale unique** — l'Office des Étrangers (OE), service public fédéral Intérieur — qui pour la plupart des procédures est la première instance de fait (et seule instance pour 9bis humanitaire qui est une compétence discrétionnaire ministérielle) ;
5. d'une **détention administrative en centre fermé** régie par les articles 71+ Loi Étrangers et contestable devant la chambre du conseil du tribunal correctionnel — pas devant un juge administratif.

Le miroir FR n'est pas opératoire :
- `OQTF` n'existe pas en BE (l'équivalent fonctionnel est **l'Annexe 13** = ordre de quitter le territoire, mais le délai, le mécanisme, le recours diffèrent radicalement) ;
- la CNDA n'existe pas — c'est le CGRA en première instance asile, le CCE en recours ;
- il n'y a pas de "tribunal administratif" BE pour le contentieux des étrangers — c'est le CCE (juridiction sui generis), et accessoirement le Conseil d'État BE en cassation administrative.

Toutes les références (loi, AR, articles) sont issues des connaissances générales du modèle. Les références dont le modèle n'est pas certain à 100 % sont annotées **"(à vérifier)"** — un avocat belge doit confirmer avant de seeder.

Les priorités utilisent l'échelle :

- **P1 — urgence procédurale** : un délai court irréversible (CCE 30 jours / 5 jours ouvrables extrême urgence, recours détention 5 jours, prolongation détention 2 mois, asile accélérée 15 jours) expose le client à perdre son droit.
- **P2 — fréquence haute** : situation rencontrée plusieurs fois par mois en consultation immigration (regroupement 40ter, 9bis, prorogation carte A étudiant).
- **P3 — spécificité BE** : pas d'équivalent FR direct → valeur produit pure (9bis circonstances exceptionnelles, 9ter médical OE, single permit, carte F membre UE, carte H Brexit, AESM tutelle MENA).
- **P4 — confort** : utile mais on peut différer sans perte de couverture.

---

## 2. Tableau A — Outils Immigration BE existants

Source : migration `105-seed-decision-tool-visibility-rules.xml` (seed initial F-IA-04), migration `106-adjust-decision-tool-visibility-rules.xml` (bascule F-IM-01 et F-IM-06 transversaux en ALWAYS_ON), plus les migrations `118` (annexe 13 BE), `123` (9bis et 9ter BE), `125` (40bis BE), `126` (40ter BE) qui ajoutent des règles `decision_tool_visibility_rules` Immigration BE après le seed initial. Croisement avec `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (vérifié 2026-05-06).

| tool_id | layer | trigger_field / trigger_value | country | Frontend câblé (TOOL_REGISTRY) | Situation juridique couverte (BE) |
|---|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | ALWAYS_ON | — | NULL (transversal FR+BE) | OUI | Arbre décisionnel pour orienter vers le bon titre BE (carte A, B, C, F, H, single permit, attestation immatriculation, annexe 35, annexe 15, regroupement 10bis/10ter/40bis/40ter). Logique partagée FR+BE — branchement par `country` + `nationalite_ue` + situation familiale. |
| `F-IM-07-droit-au-travail` | ALWAYS_ON | — | NULL (transversal) | OUI | Droit au travail selon type de titre BE — carte A toujours, carte B oui, carte C oui, carte F selon situation, attestation immatriculation/annexe 35 selon mention, 9bis non avant délivrance, 9ter selon. Référentiel `IMMIGRATION_WORK_RIGHTS` seedé. |
| `F-IM-01-checklist-pieces` | ALWAYS_ON | — | NULL (transversal) | OUI | Checklist pièces par titre BE — carte A, B, C, F, H, single permit, regroupement 10bis/10ter/40bis/40ter, 9bis, 9ter, asile (Annexe 26), recours CCE, etc. |
| `F-IM-06-recours` | ALWAYS_ON | — | NULL (transversal) | OUI | Générateur de recours — `RECOURS_CGRA` (asile 1er degré), `RECOURS_CCE` (recours en annulation/suspension contre OE et CGRA), `RECOURS_CE_BELGIQUE` (cassation administrative Conseil d'État BE) — 6 types seedés au total (3 FR + 3 BE). |
| `F-IM-08-annexe13-be` | ALWAYS_ON | — | BELGIQUE | OUI | Annexe 13 = OQT BE — calcul délais CCE 30 jours calendaires (annulation) + 5 jours ouvrables (extrême urgence). Loi 15/12/1980 art. 7 + 39/2 §2 + 39/82 §4 + AR 08/10/1981. Outil **single-country BELGIQUE**. |
| `F-IM-14-9bis-humanitaire-be` | ALWAYS_ON | — | BELGIQUE | OUI | 9bis humanitaire — séjour exceptionnel demandé depuis le territoire belge, base art. 9bis Loi 15/12/1980 + AR 17/05/2007. Faisceau d'indices : présence (≥3 ans), enracinement (familial, pro, scolarité enfants), absence menace ordre public. Scoring 0-100 pondéré, verdict ELEVEE/MOYENNE/FAIBLE. Délai instruction OE 3-24 mois. **BE-only** (équivalent FR L.435-1/-2 = AES couvert par F-IM-09). |
| `F-IM-14-9ter-medical-be` | ALWAYS_ON | — | BELGIQUE | OUI | 9ter médical — séjour pour raisons médicales graves art. 9ter Loi 15/12/1980. Conditions : maladie grave + impossibilité de traitement adéquat dans pays origine + défaut entraîne risque vie. Médecin OE évalue le rapport médical type. **BE-only** (différent du titre étranger malade FR L.425-9 — pas de présomption de gravité particulière en BE, examen sur dossier). |
| `F-IM-14-40bis-cohabitant-ue-be` | ALWAYS_ON | — | BELGIQUE | OUI | Carte F = membre famille citoyen UE (art. 40bis Loi 15/12/1980, transposition directive 2004/38/CE). Conjoint / partenaire enregistré / descendant -21 ans / ascendant à charge d'un citoyen UE résidant en BE. **BE-only** sur la mise en œuvre (FR a CARTE_A_FAMILLE pour membre UE mais procédure différente). |
| `F-IM-14-40ter-familial-belge-be` | ALWAYS_ON | — | BELGIQUE | OUI | 40ter = regroupement familial avec un Belge (art. 40ter Loi 15/12/1980). Conjoint, partenaire enregistré, descendants -21 ans, ascendants à charge d'un Belge. **BE-only** — ce régime est plus large et plus favorable que le 10ter (regroupement avec ressortissant tiers en séjour) car il s'applique à un national. **Pas d'équivalent FR direct** (FR a la carte VPF L.423-1 conjoint de Français, mais conditions de fond différentes). |

**Total effectif au 2026-05-06 : 9 outils Immigration BE** dont 5 **single-country BELGIQUE** (F-IM-08-annexe13-be + 4 outils F-IM-14) et 4 **transversaux FR+BE** (F-IM-01, F-IM-05, F-IM-06, F-IM-07).

Outils Immigration FR-only **explicitement non câblés sur Belgique** dans `decision_tool_visibility_rules` :
- `F-IM-08-oqtf-avec-delai-fr` / `F-IM-08-oqtf-sans-delai-fr` / `F-IM-08-referes-admin-fr` (CONTEXTUAL FR, type_procedure_detectee = OQTF_*) — pas de pendant BE direct (Annexe 13 BE est l'équivalent fonctionnel mais avec recours CCE différent du référé TA).
- `F-IM-09-aes-metiers-tension` / `F-IM-09-aes-famille` / `F-IM-09-aes-humanitaire` / `F-IM-09-aes-etudiant` (ALWAYS_ON FR) — l'AES est franco-français (circulaire Valls 28/11/2012 + L.435-1+/-2/-3 CESEDA). 9bis BE est la procédure équivalente fonctionnelle, déjà couverte.
- `F-IM-11-changement-statut` (ALWAYS_ON FR) — changement de statut CESEDA L.412-1. BE a son propre mécanisme de prorogation/changement de carte (art. 9 et 13 Loi Étrangers).
- `F-IM-12-asile-avance` (ALWAYS_ON FR) — Dublin III + accélérée + réexamen + apatride. **BE a strictement la même grille** (Règlement UE 604/2013 = Dublin commun, accélérée art. 57/6/1, réexamen art. 51/8, apatride loi 27/02/2019). **Pas de pendant BE seedé ⇒ MANQUE majeur F-191** (cf. Tableau B § 3.4).
- `F-IM-13-naturalisation` (ALWAYS_ON FR) — France-only Cciv. BE a son **Code de la nationalité belge** (loi 28/06/1984) avec voies distinctes (déclaration art. 12bis, naturalisation art. 19+, récupération art. 24+) → MANQUE.
- `F-IM-17-regime-algerien` (ALWAYS_ON FR) — accord franco-algérien 27/12/1968, sans aucun équivalent BE.
- `F-IM-19-mineurs` (ALWAYS_ON FR) — MNA (ordonnance JE art. 375 Cciv) + L.435-3 enfant né en France + DCEM + TIR. BE a son propre régime AESM (Admission Exceptionnelle au Séjour Mineur, loi 04/05/2007) + tutelle DGDE. **Pas de pendant BE seedé ⇒ MANQUE majeur F-191** (cf. Tableau B § 3.6).
- `F-IM-20-mesures-eloignement` (ALWAYS_ON FR) — interdiction du territoire L.541-1+, arrêté de reconduite. BE a interdiction d'entrée art. 74/11+ et expulsion (art. 20-22) → MANQUE.

L'inventaire montre une **forte couverture BE sur 9bis/9ter/40bis/40ter (F-IM-14) + Annexe 13** mais des **trous significatifs** sur asile, naturalisation, mineurs MENA, single permit, mesures d'éloignement (interdiction d'entrée, expulsion), détention, protection subsidiaire, protection temporaire (Ukraine), regroupement 10ter ressortissant tiers, prorogation/changement carte, étudiants Annexe 47, carte H Brexit, court séjour visa C contesté.

---

## 3. Tableau B — Audit juridique exhaustif Immigration BE

Une ligne = une situation juridique distincte qui mérite un outil décisionnel autonome (un outil = une situation, règle CLAUDE.md). Les outils déjà existants en Tableau A sont signalés **EXISTE**. Les autres sont **MANQUE** avec priorité.

### 3.1 — Titres de séjour ressortissants tiers (hors UE)

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | Arbre décisionnel orienté titre BE (carte A, B, C, F, H, single permit, regroupement) | Loi 15/12/1980 art. 9, 10, 13, 40 ; AR 08/10/1981 | Arbre décisionnel | EXISTE | — | Couvert. À auditer sur la complétude des branches single permit + carte H Brexit. |
| `F-IM-01-checklist-pieces` | Checklist pièces par type de titre BE | AR 08/10/1981 ; AR 11/06/2018 | Checklist | EXISTE | — | Couvert. À auditer sur AESM + apatride + protection subsidiaire (régimes peut-être pas seedés en pièces). |
| `carte-a-prorogation-be` | Prorogation carte A (séjour temporaire) — démarche 30-45 j avant expiration, motif persiste | Loi 15/12/1980 art. 13 ; AR 08/10/1981 art. 33 | Calculateur de délais + checklist conditions | MANQUE | **P2 P3 BE-only** | Très fréquent. Différent d'un renouvellement FR car la commune (administration locale) instruit en BE — pas la préfecture. |
| `carte-b-illimite-conditions` | Carte B (séjour illimité) — conditions 5 ans en règle, attache régulière | Loi 15/12/1980 art. 14 ; AR 08/10/1981 | Analyseur éligibilité | MANQUE | **P2 P3 BE-only** | Demande passage de carte A à B après 5 ans de carte A renouvelée. Différent de la carte de résident FR (procédures distinctes). |
| `carte-c-installation-conditions` | Carte C (installation = établissement, ex-CIRE) — 5 ans BE + conditions | Loi 15/12/1980 art. 14 ; AR 08/10/1981 | Analyseur éligibilité | MANQUE | P3 BE-only | Carte C a été remplacée par carte K/L UE résident longue durée pour une partie des cas (à vérifier). |
| `carte-k-l-ue-residence-longue-duree` | Statut résident longue durée UE (directive 2003/109/CE) | Loi 15/12/1980 art. 15bis ; transposition directive 2003/109/CE | Analyseur éligibilité | MANQUE | **P3 BE-only** | 5 ans séjour légal + ressources stables + assurance maladie + intégration. Distinct de la carte C BE classique. |
| `single-permit-autorisation-unique` | Single permit (autorisation unique travail+séjour) — procédure conjointe Région + OE | Loi 30/04/1999 ; AR 02/09/2018 ; loi spéciale (régionalisation politique migration travail 2014) | Checklist procédurale + analyseur éligibilité | MANQUE | **P1 P3 BE-only** | Procédure régionalisée — Région wallonne, flamande ou bruxelloise instruit la partie travail (autorisation occupation / arbeidskaart), OE instruit la partie séjour. Pas d'équivalent FR direct (autorisation de travail FR est centralisée DREETS). |
| `single-permit-renouvellement` | Renouvellement single permit — démarche au moins 60 j avant expiration | AR 02/09/2018 | Calculateur de délais + checklist | MANQUE | **P1 P3 BE-only** | Délai strict, sinon perte du séjour. |
| `carte-h-brexit` | Carte H — citoyens britanniques résidents avant 31/12/2020 | Accord retrait UK-UE 17/10/2019 ; loi 30/04/1999 transposition | Analyseur éligibilité + checklist | MANQUE | **P3 BE-only** | Spécifique Brexit. Inscription jusqu'au 31/12/2021 → carte H délivrée. Pas d'équivalent FR (FR a son propre régime brexit). |
| `carte-f-membre-famille-ue` | Carte F = membre famille UE (40bis) | Loi 15/12/1980 art. 40bis ; directive 2004/38/CE | Analyseur éligibilité | EXISTE (`F-IM-14-40bis-cohabitant-ue-be`) | — | Couvert. |
| `carte-fplus-rlue-membre-famille` | Carte F+ — séjour permanent membre famille UE après 5 ans | Loi 15/12/1980 art. 42quinquies ; directive 2004/38/CE | Analyseur éligibilité | MANQUE | P3 BE-only | F+ délivrée après 5 ans de carte F, distincte. |
| `attestation-immatriculation-annexe-35` | Annexe 35 = attestation immatriculation (titre provisoire pendant procédure) | AR 08/10/1981 | Information / checklist effets | MANQUE | P2 BE-only | Délivrée pendant l'instruction d'une demande (asile, regroupement, etc.). Effets sur droit travail variables. |
| `annexe-15-recepisse` | Annexe 15 = récépissé de demande (titre provisoire) | AR 08/10/1981 | Information / checklist effets | MANQUE | P3 BE-only | Différent de l'Annexe 35. À distinguer dans l'arbre décisionnel. |

### 3.2 — Regroupement familial

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `regroupement-40ter-belge` | Regroupement avec un Belge (conjoint, descendants -21, ascendants à charge) | Loi 15/12/1980 art. 40ter | Analyseur éligibilité | EXISTE (`F-IM-14-40ter-familial-belge-be`) | — | Couvert. **BE-only** (FR a VPF L.423-1 mais procédure différente). |
| `regroupement-40bis-membre-ue` | Regroupement membre famille UE (carte F) | Loi 15/12/1980 art. 40bis | Analyseur éligibilité | EXISTE (`F-IM-14-40bis-cohabitant-ue-be`) | — | Couvert. |
| `regroupement-10bis-conjoint-tiers` | Regroupement avec ressortissant tiers en séjour limité | Loi 15/12/1980 art. 10bis | Analyseur éligibilité + checklist | MANQUE | **P2 P3 BE-only** | Régime distinct du 40ter — couvre les cas où le regroupant n'est pas Belge mais résident BE. Conditions ressources / logement / assurance maladie strictes. |
| `regroupement-10ter-conjoint-tiers-illimite` | Regroupement avec ressortissant tiers en séjour illimité (carte B/C) | Loi 15/12/1980 art. 10 + 10ter | Analyseur éligibilité + checklist | MANQUE | **P2 P3 BE-only** | Différent du 10bis sur la durée et les conditions d'appréciation. **Pas d'équivalent FR direct** (FR a regroupement L.434-* moins éclaté). |
| `regroupement-conditions-ressources-1500` | Conditions ressources regroupement (1,5 × revenu intégration) | Loi 15/12/1980 art. 10 § 5 ; AR 17/05/2007 | Calculateur seuil ressources | MANQUE | **P2 P3 BE-only** | Seuil dur — 120 % RIS x 1,5 mensualisé. Souvent point bloquant en consultation. |
| `regroupement-condition-logement-conforme` | Conditions logement (attestation logement conforme commune) | Loi 15/12/1980 art. 10 ; AR 17/05/2007 | Checklist | MANQUE | P3 BE-only | Attestation délivrée par la commune (≠ règles FR DALO/préfectorales). |
| `regroupement-cohabitation-legale-be` | Regroupement partenaire enregistré (cohabitation légale art. 1475 CC BE) | Code civil BE art. 1475+ ; Loi 15/12/1980 art. 40ter / 10ter | Analyseur éligibilité | MANQUE | **P3 BE-only** | Cohabitation légale BE = équivalent fonctionnel partiel du PACS FR mais pas identique. Cas fréquent. |
| `regroupement-conjoint-violences` | Maintien droit séjour en cas violences conjugales — conjoint regroupé | Loi 15/12/1980 art. 11 § 2 al. 4 ; directive 2003/86/CE | Analyseur conditions | MANQUE | **P1 BE-only** | Protection conjoint étranger en cas de divorce ou cessation cohabitation pour violences. Critique. |
| `regroupement-mineur-non-accompagne-rejoignant` | Mineur étranger rejoignant un parent en BE | Loi 15/12/1980 art. 10 § 1, 4° ; CIRE / arrêtés | Analyseur éligibilité | MANQUE | P3 BE-only | À distinguer du MENA (mineur arrivé seul). |

### 3.3 — Procédures humanitaires et exceptionnelles

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `9bis-humanitaire-be` | 9bis circonstances exceptionnelles | Loi 15/12/1980 art. 9bis ; AR 17/05/2007 ; circulaire ministérielle Vandeurzen 19/07/2009 (régularisation 2009 historique) | Scoring + faisceau indices | EXISTE (`F-IM-14-9bis-humanitaire-be`) | — | Couvert. **BE-only** (différent de l'AES FR — pas une régularisation par le travail). |
| `9ter-medical-be` | 9ter procédure médicale | Loi 15/12/1980 art. 9ter ; AR 17/05/2007 ; AR 24/01/2011 (procédure médecin OE) | Analyseur éligibilité + checklist rapport médical | EXISTE (`F-IM-14-9ter-medical-be`) | — | Couvert. **BE-only** (différent du titre étranger malade FR L.425-9). |
| `9bis-circonstances-exceptionnelles-renforce` | 9bis avec scoring détaillé "circonstances exceptionnelles" — détaillé par type (santé, famille, intégration) | Loi 15/12/1980 art. 9bis ; jurisprudence CCE | Scoring détaillé | MANQUE | P3 BE-only | F-IM-14-9bis-humanitaire-be donne un scoring agrégé. Un outil "ventilation par catégorie de circonstances exceptionnelles" serait utile pour préparation argumentaire. |
| `9ter-rapport-medical-type-modele` | Rapport médical type 9ter — modèle structuré | AR 24/01/2011 art. 8 ; AM Office des Étrangers (modèle officiel) | Générateur de document type | MANQUE | **P2 P3 BE-only** | Le modèle est très formaliste — pathologie + traitement requis + indisponibilité dans pays origine + risque encouru. Outil générateur réduit fortement la dette de saisie. |
| `regularisation-2009-historique` | Régularisation 2009 (instruction Vandeurzen 19/07/2009 — historique) | Instruction ministérielle 19/07/2009 ; abrogée mais dossiers anciens en cours | Information / archive | MANQUE | P4 | Plus de demandes nouvelles, mais cas anciens encore traités en juridiction. Faible priorité. |
| `aesm-mineur-non-accompagne` | AESM = Admission Exceptionnelle au Séjour Mineur (mineur étranger non accompagné) | Loi 15/12/1980 art. 9bis + circulaire 15/09/2005 ; loi 04/05/2007 (tutelle MENA) | Analyseur éligibilité + checklist | MANQUE | **P1 P3 BE-only** | **MAJEUR** — pas d'équivalent FR direct. MENA reçoit une tutelle DGDE (Service des Tutelles SPF Justice) ; le projet de vie est étudié par OE pour AESM. Sans cet outil, pas de couverture des MENA en BE. |
| `mena-tutelle-dgde-procedure` | Tutelle MENA — désignation tuteur DGDE, projet de vie | Loi 04/05/2007 (loi tutelle MENA) ; AR 22/12/2003 | Checklist procédurale | MANQUE | **P1 P3 BE-only** | Liée à AESM mais procédure distincte (volet protection mineur, indépendamment du séjour). |
| `apatride-procedure-be` | Procédure apatride (reconnaissance + délivrance titre) | Loi 27/02/2019 sur statut apatride ; Convention NY 28/09/1954 | Analyseur éligibilité + checklist | MANQUE | **P3 BE-only** | Loi récente 2019 — procédure formelle reconnaissance apatridie auprès du tribunal de la famille (compétence judiciaire). Très spécialisé mais croissant. |

### 3.4 — Asile et protection internationale

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `demande-asile-cgra-procedure` | Demande asile premier dépôt CGRA — Annexe 26 | Loi 15/12/1980 art. 50, 51, 51/8 ; AR 11/07/2003 | Checklist procédurale | MANQUE | **P1 P3 BE-only** | Démarrage procédure asile : enregistrement OE → Annexe 26 → CGRA → audition. Outil critique — équivalent fonctionnel FR couvert par F-IM-12 mais procédure très différente. |
| `dublin-iii-be-determination` | Détermination État membre responsable (Règlement Dublin III 604/2013) | Règlement UE 604/2013 ; loi 15/12/1980 art. 51/5 | Arbre décisionnel | MANQUE | **P1** | Règlement européen identique aux 27. F-IM-12 FR couvre Dublin — un outil dédié BE serait utile pour les délais 11/12 mois (transfert) et conditions de prise en charge BE-spécifiques. |
| `asile-procedure-acceleree-be` | Procédure accélérée asile (15 j fond CGRA, recours 10 j) | Loi 15/12/1980 art. 57/6/1 ; AR 11/07/2003 | Calculateur de délais + analyseur cas | MANQUE | **P1 P3 BE-only** | Cas accélérés : pays origine sûr, demande tardive sans justification, demande après OQT, fraude. Délais courts vs procédure normale (~6-12 mois). |
| `asile-protection-subsidiaire-be` | Statut protection subsidiaire (loi 18/12/2016) | Loi 18/12/2016 (transposition directive 2011/95/UE) ; loi 15/12/1980 art. 48/4 | Analyseur éligibilité | MANQUE | **P2 P3 BE-only** | Critique — alternative quand réfugié refusé. Conditions : risque atteinte grave (peine mort, torture, conflit armé). |
| `protection-temporaire-ukraine-be` | Protection temporaire (Ukraine, directive 2001/55/CE activée 04/03/2022) | Décision UE 2022/382 ; loi 15/12/1980 art. 57/29+ | Checklist | MANQUE | **P2 P3 BE-only** | Activée 04/03/2022 pour Ukraine, prolongée annuellement. Toujours active à date 2026. Outil utile pour clarifier conditions, droit travail (immédiat), durée. |
| `asile-recours-cce-30-jours` | Recours CCE contre décision CGRA — annulation + extrême urgence | Loi 15/12/1980 art. 39/2 §1 ; loi 15/09/2006 | Calculateur de délais + générateur requête | MANQUE | **P1 P3 BE-only** | 30 jours calendaires recours fond, 5 jours ouvrables extrême urgence (si OQT exécutoire imminent). Couvert partiellement par F-IM-06 (générateur de recours générique) mais mérite outil dédié asile. |
| `asile-renvoi-cgra-reexamen` | Demande de réexamen asile (faits nouveaux) | Loi 15/12/1980 art. 51/8 | Analyseur éligibilité + checklist | MANQUE | **P3 BE-only** | Très technique — recevabilité conditionnée à éléments nouveaux non disponibles antérieurement. Souvent rejetée. |
| `asile-pays-origine-sur-be` | Liste pays origine sûrs BE | AR mis à jour annuellement (liste pays) | Information / vérification | MANQUE | P3 BE-only | Liste BE diffère de la liste FR. Liste mise à jour annuellement. |
| `asile-mineur-non-accompagne-procedure` | Demande asile MENA — procédure adaptée + tutelle | Loi 15/12/1980 art. 57/6/2 ; loi 04/05/2007 | Checklist procédurale | MANQUE | **P1 P3 BE-only** | Audition adaptée, tuteur obligatoire, projet de vie, considération supérieur intérêt enfant. Critique. |
| `asile-femme-traitement-genre` | Demande asile fondée sur le genre (MGF, mariage forcé, violences) | Loi 15/12/1980 art. 48/3 + jurisprudence CGRA / CCE | Checklist preuves | MANQUE | P3 BE-only | Domaine spécialisé — souvent argumentaire dédié. Faible priorité produit mais haute valeur métier. |

### 3.5 — Éloignement, OQT, détention, interdiction d'entrée

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `annexe-13-oqt-be` | Annexe 13 = OQT BE — calcul délais CCE 30 j / 5 j ouvrables | Loi 15/12/1980 art. 7 + 39/2 §2 + 39/82 §4 ; AR 08/10/1981 | Calculateur de délais + générateur recours | EXISTE (`F-IM-08-annexe13-be`) | — | Couvert. **BE-only**. |
| `annexe-13quinquies-oqt-interdiction-entree` | Annexe 13quinquies = OQT + interdiction d'entrée | Loi 15/12/1980 art. 74/11 ; AR 08/10/1981 | Analyseur durée IE + générateur recours | MANQUE | **P1 P3 BE-only** | IE 3 ans / 5 ans / 8 ans selon motif. Recours CCE possible. À distinguer de l'Annexe 13 simple. |
| `annexe-14-invitation-presenter-oe` | Annexe 14 = invitation à se présenter Office des Étrangers | Loi 15/12/1980 art. 7 ; AR 08/10/1981 | Information + checklist effets | MANQUE | **P1 P3 BE-only** | Préalable courant à un OQT. Comparution obligatoire. |
| `interdiction-entree-be-recours-art-74-12` | Interdiction d'entrée — recours en levée art. 74/12 | Loi 15/12/1980 art. 74/12 | Analyseur conditions levée + générateur requête | MANQUE | **P2 P3 BE-only** | Demande levée IE — possible après écoulement 2/3 délai, raisons humanitaires, etc. |
| `cce-recours-extreme-urgence-5j` | Recours en extrême urgence CCE — 5 jours ouvrables | Loi 15/12/1980 art. 39/82 §4 al. 2 + §4 al. 3 ; loi 15/09/2006 | Calculateur de délais (BelgianBusinessDays) + générateur requête | MANQUE (couvert partiellement par F-IM-08-annexe13-be) | **P1 P3 BE-only** | Outil dédié serait utile — l'extrême urgence vise tout acte exécutoire imminent (OQT, refus accès territoire, transfert Dublin). 5 jours ouvrables effectifs. |
| `cce-recours-annulation-30j` | Recours CCE en annulation — 30 jours calendaires | Loi 15/12/1980 art. 39/82 §4 al. 1 ; loi 15/09/2006 | Calculateur de délais + générateur requête | MANQUE (couvert partiellement par F-IM-06-recours) | **P1 P3 BE-only** | Outil pour les recours non-asile (refus titre, refus regroupement, refus 9bis). |
| `detention-centre-ferme-be` | Détention administrative centre fermé — conditions, prolongation 2 mois, recours chambre conseil | Loi 15/12/1980 art. 7 al. 3, 27, 29, 71+, 74/5 ; AR 02/08/2002 | Calculateur durée + checklist conditions | MANQUE | **P1 P3 BE-only** | **Critique** — détention max 2 mois, prolongeable selon procédures. Recours = chambre du conseil tribunal correctionnel (≠ JLD FR). Aucun équivalent FR direct (CRA FR avec JLD est différent). |
| `detention-recours-chambre-conseil-be` | Recours mise en liberté chambre du conseil — 5 jours | Loi 15/12/1980 art. 71+ ; CIC art. 21+ | Calculateur de délais + générateur requête | MANQUE | **P1 P3 BE-only** | Recours suspensif mise en liberté. À traiter en urgence absolue. |
| `expulsion-art-20-22-be` | Expulsion (art. 20-22 Loi Étrangers) — étranger en séjour de plus de 10 ans, motifs ordre public graves | Loi 15/12/1980 art. 20-22 ; AR 08/10/1981 | Analyseur conditions + générateur recours | MANQUE | **P3 BE-only** | Cas spécifique — distinct de l'OQT et de l'interdiction d'entrée. Procédure consultative préalable Commission Consultative des Étrangers (CCE — distincte du Conseil du Contentieux des Étrangers, attention à la confusion). |
| `transfert-dublin-be` | Décision transfert Dublin — recours suspensif CCE | Règlement UE 604/2013 ; loi 15/12/1980 art. 51/5 | Calculateur de délais + générateur recours | MANQUE | **P1 P3 BE-only** | Délais Dublin 6 mois transfert effectif (12-18 mois selon cas). Recours CCE possible, modalités spécifiques. |
| `non-refoulement-art-3-cedh-be` | Non-refoulement art. 3 CEDH — argumentaire risque pays | CEDH art. 3 ; jurisprudence CCE Chahal/CEDH | Checklist preuves | MANQUE | P3 BE-only | Argumentaire systématique en recours OQT/expulsion vers pays à risque. |

### 3.6 — Mineurs étrangers (MENA, AESM, mineurs régularisation)

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `mena-tutelle-dgde` | MENA — tutelle DGDE Service des Tutelles | Loi 04/05/2007 (tutelle MENA) ; AR 22/12/2003 | Checklist procédurale | MANQUE | **P1 P3 BE-only** | **MAJEUR** — désignation tuteur, projet de vie, suivi. Aucun équivalent FR direct (MNA FR = ordonnance JE, mécanisme différent). |
| `aesm-admission-exceptionnelle-mineur` | AESM = Admission Exceptionnelle Séjour Mineur | Loi 15/12/1980 art. 9bis adaptation MENA + circulaire 15/09/2005 | Analyseur éligibilité | MANQUE | **P1 P3 BE-only** | Outil dédié — distinct du 9bis adulte car critères d'appréciation adaptés (intégration scolaire, projet de vie, perspective autonomie). |
| `mineur-belge-naissance` | Enfant né en BE de parents en séjour irrégulier — droit séjour ? | Loi 15/12/1980 ; jurisprudence CEDH | Analyseur cas | MANQUE | P2 BE-only | Cas fréquent — l'enfant n'acquiert pas la nationalité BE par naissance (≠ FR L.435-3). |
| `prorogation-tuteur-mineur-be` | Prorogation tutelle MENA après 18 ans (jeune majeur) | Loi 04/05/2007 art. 18 | Calculateur conditions | MANQUE | P3 BE-only | Tutelle peut être prorogée si jeune majeur en formation. |
| `regroupement-mineur-rejoignant-be` | Mineur étranger rejoignant parent en BE | Loi 15/12/1980 art. 10 § 1, 4° | Analyseur éligibilité | MANQUE | P3 BE-only | À distinguer du MENA. Volet regroupement familial classique. |

### 3.7 — Citoyens UE et apparentés

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `citoyen-ue-droit-sejour-3-mois` | Citoyen UE — droit séjour 3 mois sans formalité | Loi 15/12/1980 art. 40 § 4 ; directive 2004/38/CE | Information | MANQUE | P4 BE-only | Cas simple. |
| `citoyen-ue-attestation-enregistrement` | Citoyen UE — attestation enregistrement après 3 mois (E ou EU+) | Loi 15/12/1980 art. 40 § 4 ; AR 08/10/1981 | Checklist | MANQUE | P3 BE-only | Distinct de la carte F (membre famille). |
| `citoyen-ue-fin-droit-sejour-charge-publique` | Fin droit séjour citoyen UE — devenir charge déraisonnable | Loi 15/12/1980 art. 42bis ; jurisprudence CJUE | Analyseur conditions | MANQUE | P3 BE-only | Cas contentieux — OE peut mettre fin au droit séjour si charge système aide sociale. |
| `carte-h-brexit-conditions` | Carte H Brexit — conditions résident avant 31/12/2020 | Accord retrait UK-UE 17/10/2019 ; loi 30/04/1999 | Analyseur éligibilité | MANQUE | **P3 BE-only** | Cas spécialisé Brexit — séparé de F-IM-14-40bis (membre famille UE classique). |

### 3.8 — Étudiants étrangers

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `etudiant-autorisation-sejour-art-58` | Autorisation séjour étudiant (Annexe 47) | Loi 15/12/1980 art. 58, 59, 60, 61 ; AR 08/10/1981 art. 99-103 | Checklist procédurale | MANQUE | **P2 P3 BE-only** | Procédure très formelle — Annexe 47 délivrée en consulat ou OE. Conditions ressources (~671 €/mois 2025), prise en charge. |
| `etudiant-prorogation-r58` | Prorogation autorisation étudiant — résultats académiques | Loi 15/12/1980 art. 61 ; AR 08/10/1981 art. 101 | Analyseur éligibilité + checklist | MANQUE | **P2 P3 BE-only** | Conditions résultats académiques minimaux. Refus fréquent en cas d'échecs répétés. |
| `etudiant-changement-statut-fin-etudes` | Changement statut étudiant → travail (single permit) à fin études | Loi 15/12/1980 ; AR 02/09/2018 | Analyseur transition | MANQUE | **P2 P3 BE-only** | Très fréquent — équivalent fonctionnel partiel du F-IM-11 FR mais procédure BE-spécifique (single permit). |
| `etudiant-aps-recherche-emploi` | Autorisation provisoire de séjour pour recherche emploi (post-études) | Loi 15/12/1980 ; AR 23/04/2018 (à vérifier) | Calculateur durée + checklist | MANQUE | P3 BE-only | Régime relativement récent — équivalent BE de l'APS recherche emploi FR. |
| `etudiant-ressources-tiers-garant` | Garant tiers étudiant — déclaration de prise en charge Annexe 32 | Loi 15/12/1980 art. 60 ; AR 08/10/1981 | Checklist | MANQUE | P3 BE-only | Annexe 32 — garant souvent nécessaire pour étudiant sans ressources propres. |
| `etudiant-droit-travail-be` | Droit au travail étudiant — 20h/semaine pendant études | AR 16/05/2003 ; AR 02/09/2018 | Information / calculateur heures | MANQUE | P3 BE-only | Limité à 20h/semaine périodes scolaires, plein temps vacances. À distinguer de F-IM-07 général. |

### 3.9 — Travail des étrangers (single permit + carte professionnelle)

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `single-permit-procedure-complete` | Single permit — procédure conjointe Région + OE | Loi 30/04/1999 ; AR 02/09/2018 | Checklist + arbre décisionnel | MANQUE (recoupe partiellement § 3.1) | **P1 P3 BE-only** | Critique. Régionalisation = compétence Région wallonne / flamande / bruxelloise différente. |
| `single-permit-conditions-emploi-tension` | Single permit métiers en pénurie — assouplissement | AR régional listes pénurie ; AR 02/09/2018 | Analyseur éligibilité | MANQUE | P3 BE-only | Liste pénurie publiée par Région. |
| `single-permit-cadre-haut-niveau` | Single permit cadre haut niveau (carte bleue UE) | Directive 2009/50/CE ; loi 30/04/1999 | Analyseur éligibilité | MANQUE | P3 BE-only | Salaire seuil élevé, profil hautement qualifié. Distinct du single permit standard. |
| `carte-professionnelle-independant-be` | Carte professionnelle — indépendant non-UE | Loi 19/02/1965 ; AR 03/02/2003 ; régionalisée | Checklist | MANQUE | P3 BE-only | Procédure régionalisée. Pour activités indépendantes (commerce, profession libérale). |
| `detachement-limosa-be` | Détachement intra-UE — déclaration LIMOSA | Loi 12/04/1965 ; directive 96/71/CE ; AR 19/03/2007 | Checklist conformité | MANQUE | P4 BE-only | Plus côté employeur. |
| `permis-saisonnier-be` | Permis travail saisonnier (90 jours max) | AR 02/09/2018 | Calculateur durée + checklist | MANQUE | P4 BE-only | Cas rare en consultation cabinet généraliste. |

### 3.10 — Naturalisation et acquisition de nationalité belge

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `nationalite-declaration-art-12bis` | Déclaration de nationalité art. 12bis (séjour 5/10 ans) | Code nationalité belge (loi 28/06/1984) art. 12bis | Analyseur éligibilité + checklist | MANQUE | **P2 P3 BE-only** | **MAJEUR** — voie principale d'acquisition pour migrants en BE. Conditions : 5 ans (séjour illimité + emploi/intégration) ou 10 ans (séjour illimité). Connaissance langue niveau A2. |
| `nationalite-naturalisation-art-19-21` | Naturalisation par décret art. 19-21 | Code nationalité belge art. 19-21 | Analyseur éligibilité | MANQUE | **P3 BE-only** | Voie discrétionnaire — Chambre des représentants statue. Très rare. |
| `nationalite-mineur-art-11-13` | Acquisition automatique mineur né en BE — art. 11, 13 | Code nationalité belge art. 11, 13 | Analyseur cas | MANQUE | P3 BE-only | 3e génération automatique, 2e génération sur déclaration parents. |
| `nationalite-conjoint-belge-art-16` | Acquisition par mariage avec un Belge — déclaration | Code nationalité belge art. 16 | Analyseur conditions | MANQUE | **P2 P3 BE-only** | 5 ans cohabitation + connaissance langue + intégration. Différent du régime FR (4 ans mariage avec un Français). |
| `nationalite-recouvrement-art-24` | Récupération nationalité belge perdue | Code nationalité belge art. 24+ | Analyseur cas | MANQUE | P4 | Cas rare. |
| `nationalite-perte-art-22-23` | Perte nationalité belge | Code nationalité belge art. 22-23 | Information / vérification | MANQUE | P4 | Cas rare. |
| `nationalite-deux-nationalites` | Compatibilité double nationalité — pays origine | Variable selon pays origine | Information | MANQUE | P4 | Compétence de l'État d'origine, pas BE. |

### 3.11 — Procédures et juridictions

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `cce-recours-annulation-procedure` | Recours CCE en annulation — procédure complète | Loi 15/12/1980 art. 39/2, 39/56 à 39/85 ; loi 15/09/2006 | Calculateur délais + générateur requête | MANQUE (partiel via F-IM-06) | **P1 P3 BE-only** | **MAJEUR** — outil dédié procédure CCE annulation. Délai 30 j calendaires. Mémoire en réplique 8 jours. Audience publique. |
| `cce-recours-suspension-procedure` | Recours CCE en suspension (référé administratif) | Loi 15/12/1980 art. 39/82 ; loi 15/09/2006 | Calculateur délais + générateur requête | MANQUE | **P1 P3 BE-only** | Recours suspensif — conditions urgence + risque préjudice grave. À distinguer de la suspension extrême urgence. |
| `cce-recours-extreme-urgence-procedure` | Recours CCE en extrême urgence — 5 j ouvrables | Loi 15/12/1980 art. 39/82 §4 al. 2-3 | Calculateur délais (BelgianBusinessDays) | MANQUE | **P1 P3 BE-only** | Cas urgentissime — OQT exécutoire imminent, transfert Dublin imminent. Audience souvent dans les 48h. |
| `cce-pourvoi-conseil-etat-be` | Pourvoi cassation administrative Conseil d'État BE — 30 jours | Lois coord. CE 12/01/1973 art. 14, 30 ; loi 15/12/1980 art. 39/2 | Calculateur délais + checklist | MANQUE (partiel via F-IM-06 RECOURS_CE_BELGIQUE) | **P3 BE-only** | Cassation administrative contre arrêts CCE. Procédure spécialisée. |
| `oe-recours-gracieux-be` | Recours gracieux Office des Étrangers | Pratique administrative + circulaire OE | Information / générateur courrier | MANQUE | P3 BE-only | Pas de recours gracieux formel en BE — pratique administrative possible. À distinguer du recours gracieux préfectoral FR. |
| `cgra-procedure-asile` | Procédure CGRA (1ère instance asile) | Loi 15/12/1980 art. 50, 51, 57/6 | Checklist procédurale | MANQUE | P2 BE-only | Audition CGRA — préparation client. Outil checklist questions types. |
| `competence-juridiction-be-immigration` | Compétence juridiction BE immigration — décrochage CCE / CE / TPI | Loi 15/12/1980 ; CIC ; loi 15/09/2006 | Arbre décisionnel | MANQUE | P3 BE-only | Évite déclinatoires : CCE pour OE/CGRA, chambre conseil pour détention, TPI famille pour apatride. |
| `delais-be-jours-ouvrables-vs-calendaires` | Calculateur délais ouvrables BE (extrême urgence 5j) vs calendaires (annulation 30j) | Loi 15/12/1980 art. 39 ; AR 11/06/2018 | Calculateur | EXISTE PARTIEL (`BelgianBusinessDaysCalculator` utilisé dans annexe 13) | P3 BE-only | À étendre comme service partagé. |

### 3.12 — Court séjour et visas

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `visa-c-court-sejour-90j` | Visa C court séjour 90 jours / 180 jours (Schengen) | Code Visas UE 810/2009 ; loi 15/12/1980 art. 12 | Information / vérification | MANQUE | P4 | Compétence consulaire. Refus visa C contestable. |
| `visa-c-refus-recours` | Refus visa C — recours CCE possible | Loi 15/12/1980 art. 12bis ; jurisprudence CCE | Calculateur délais + analyseur cas | MANQUE | **P3 BE-only** | Délai 60 jours recours CCE depuis notification refus. |
| `visa-d-long-sejour-procedure` | Visa D long séjour — procédure consulaire | Loi 15/12/1980 art. 9 ; AR 08/10/1981 | Checklist | MANQUE | P3 BE-only | Préalable à la plupart des cartes A/B/F/H. |
| `regularisation-overstay-be` | Régularisation après dépassement visa C — 9bis | Loi 15/12/1980 art. 9bis | Couvert par 9bis humanitaire | EXISTE (via `F-IM-14-9bis-humanitaire-be`) | — | Couvert. |

### 3.13 — Autres situations spécifiques

| tool_id proposé | Situation juridique BE | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `victime-traite-etres-humains-be` | Victime traite des êtres humains — titre séjour spécifique | Loi 15/12/1980 art. 61/2+ ; circulaire 26/09/2008 | Analyseur éligibilité + checklist | MANQUE | **P3 BE-only** | Régime protecteur spécifique — partenariat avec centres spécialisés (PAG-ASA, Sürya, Payoke). |
| `victime-violences-conjugales-be` | Maintien droit séjour victime violences conjugales | Loi 15/12/1980 art. 11 § 2 al. 4 | Analyseur conditions | MANQUE (recoupe § 3.2) | **P1 P3 BE-only** | Critique — voir § 3.2 `regroupement-conjoint-violences`. |
| `regroupement-cite-mariage-blanc` | Soupçon mariage blanc — annulation regroupement | Loi 15/12/1980 art. 11 § 2 al. 1 ; jurisprudence CCE | Analyseur risque | MANQUE | P3 BE-only | Outil défensif si OE conteste. |
| `attestation-immatriculation-effets-be` | Annexe 35 — droits, durée, effets | AR 08/10/1981 art. 71/3 ; jurisprudence CCE | Information | MANQUE | P3 BE-only | Souvent confondu avec Annexe 15 — utile de clarifier dans l'arbre. |
| `naturalisation-langue-niveau-a2-be` | Test connaissance langue (A2) pour 12bis | Loi 28/06/1984 ; AR 14/01/2013 | Checklist preuves | MANQUE | P3 BE-only | Preuve : diplôme niveau A2, certificat NT2 (Pays-Bas), DELF, etc. |
| `integration-cours-civique-region` | Parcours intégration régionalisé — Wallonie / Flandre / Bruxelles | Décrets régionaux 2014-2016 | Information / arbre régional | MANQUE | P3 BE-only | **Régionalisé** — Wallonie : Parcours d'Intégration ; Flandre : Inburgering ; Bruxelles : Bruxelles-Inburgering. Différenciation à coder. Préalable parfois à 12bis. |

---

## 4. Tableau C — Audit F-166 Immigration BE (flags IA + bascule ALWAYS_ON → CONTEXTUAL)

### 4.1 — Constat F-166 Immigration BE

F-166 (`docs/features/F-166/`) a été livrée pour Travail FR uniquement (8 outils basculés ALWAYS_ON → CONTEXTUAL via 8 booleans IA). **Immigration BE n'a pas été touchée.**

État actuel Immigration BE en `decision_tool_visibility_rules` (vérifié 2026-05-06) :

| tool_id | Layer actuel | trigger_field actuel | trigger_value actuel | Affichage actuel |
|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | ALWAYS_ON | — | — | Affiché systématiquement (transversal FR+BE). |
| `F-IM-07-droit-au-travail` | ALWAYS_ON | — | — | Affiché systématiquement. |
| `F-IM-01-checklist-pieces` | ALWAYS_ON | — | — | Affiché systématiquement. |
| `F-IM-06-recours` | ALWAYS_ON | — | — | Affiché systématiquement. |
| `F-IM-08-annexe13-be` | ALWAYS_ON | — | — | Affiché systématiquement BE. |
| `F-IM-14-9bis-humanitaire-be` | ALWAYS_ON | — | — | Affiché systématiquement BE. |
| `F-IM-14-9ter-medical-be` | ALWAYS_ON | — | — | Affiché systématiquement BE. |
| `F-IM-14-40bis-cohabitant-ue-be` | ALWAYS_ON | — | — | Affiché systématiquement BE. |
| `F-IM-14-40ter-familial-belge-be` | ALWAYS_ON | — | — | Affiché systématiquement BE. |

**5 outils Immigration BE sont aujourd'hui en ALWAYS_ON** (Annexe 13 + 4 outils F-IM-14). Sur tout dossier Immigration BE, l'avocat voit systématiquement les 5 outils, même quand le dossier ne déclenche pas la situation correspondante (par exemple : un dossier de prorogation carte A étudiant n'a aucun rapport avec 9ter médical ni 40ter regroupement familial avec Belge — pourtant les 5 outils s'affichent). C'est exactement le problème pour lequel F-166 a été créée côté Travail FR.

### 4.2 — Flags IA Immigration BE proposés (équivalents de F-166 SF-166-01)

Pour permettre la bascule ALWAYS_ON → CONTEXTUAL des 5 outils Immigration BE, le prompt Sonnet domain `DROIT_IMMIGRATION` doit produire 5 booleans dans `immigration_extracted_data` quand `country = BELGIQUE`.

| Flag JSON proposé | Outil concerné | Critère de détection (résumé) | Indices textuels |
|---|---|---|---|
| `procedure_9bis_envisagee` | `F-IM-14-9bis-humanitaire-be` | Indice qu'une demande 9bis humanitaire est envisagée, en cours ou pertinente | "9bis", "circonstances exceptionnelles", "régularisation humanitaire", "demande de séjour exceptionnel", présence prolongée + enracinement |
| `procedure_9ter_medicale_detectee` | `F-IM-14-9ter-medical-be` | Indice d'une procédure médicale 9ter | "9ter", "raisons médicales graves", "rapport médical OE", "maladie grave", "indisponibilité traitement pays origine", certificats médicaux |
| `regroupement_40ter_detecte` | `F-IM-14-40ter-familial-belge-be` | Regroupement avec un Belge | "40ter", "conjoint belge", "cohabitant légal belge", "ascendant à charge belge", acte mariage Belge, lien familial Belge |
| `regroupement_40bis_detecte` | `F-IM-14-40bis-cohabitant-ue-be` | Regroupement membre famille UE (carte F) | "40bis", "carte F", "membre famille UE", "conjoint citoyen UE", "directive 2004/38", lien familial citoyen UE |
| `oqt_annexe13_detectee` | `F-IM-08-annexe13-be` | OQT BE notifié | "Annexe 13", "ordre de quitter le territoire", "OQT", "obligation de quitter", notification OE, délai départ |

Tous les flags sont par défaut `false` quand le critère n'est pas détecté, jamais `null`. Pattern strictement aligné sur SF-166-01.

### 4.3 — Bascule ALWAYS_ON → CONTEXTUAL (équivalent SF-166-02)

| `tool_id` | Layer actuel | Layer cible | `trigger_field` | `trigger_value` | priority |
|---|---|---|---|---|---|
| `F-IM-08-annexe13-be` | ALWAYS_ON | CONTEXTUAL | `oqt_annexe13_detectee` | `true` | 59 |
| `F-IM-14-9bis-humanitaire-be` | ALWAYS_ON | CONTEXTUAL | `procedure_9bis_envisagee` | `true` | 64 |
| `F-IM-14-9ter-medical-be` | ALWAYS_ON | CONTEXTUAL | `procedure_9ter_medicale_detectee` | `true` | 65 |
| `F-IM-14-40bis-cohabitant-ue-be` | ALWAYS_ON | CONTEXTUAL | `regroupement_40bis_detecte` | `true` | 66 |
| `F-IM-14-40ter-familial-belge-be` | ALWAYS_ON | CONTEXTUAL | `regroupement_40ter_detecte` | `true` | 67 |

UUID namespace proposé : `f1a04001-0000-0000-0000-eeee30000XXX` (suit la convention F-165 / F-166).

**Outils transversaux non concernés** :
- `F-IM-05-arbre-decisionnel-titre` reste **ALWAYS_ON** — c'est l'outil structurant tout dossier immigration (point d'entrée diagnostique). Pas de bascule.
- `F-IM-01-checklist-pieces` reste **ALWAYS_ON** — toute procédure immigration nécessite des pièces. Outil utile dès l'arrivée du dossier.
- `F-IM-06-recours` reste **ALWAYS_ON** — outil de recours générique (préfet, TA, CGRA, CCE, CE BE) toujours utile selon le contexte. Bascule éventuelle plus fine selon `type_recours_code` déjà existant — à laisser en l'état (cas similaire à F-DT-04 fiche prud'homale Travail FR qui reste ALWAYS_ON).
- `F-IM-07-droit-au-travail` reste **ALWAYS_ON** — question récurrente posée systématiquement par l'avocat à chaque dossier immigration.

### 4.4 — Flags IA Immigration BE supplémentaires (futurs outils MANQUE)

Si les outils MANQUE du Tableau B sont livrés en backlog, voici les flags IA correspondants à prévoir en parallèle :

| Outil futur (Tableau B) | Flag IA proposé | Preuves textuelles |
|---|---|---|
| `single-permit-procedure-complete` | `single_permit_envisage` | "single permit", "autorisation unique", "permis travail BE", "arbeidskaart", "occupation Région" |
| `carte-h-brexit` | `carte_h_brexit_detectee` | "Brexit", "ressortissant britannique avant 2021", "carte H", "accord retrait UK-UE" |
| `cce-recours-annulation-procedure` | `recours_cce_envisage` | "CCE", "Conseil du Contentieux des Étrangers", "recours en annulation 30 jours" |
| `cce-recours-extreme-urgence-procedure` | `recours_cce_extreme_urgence` | "extrême urgence", "5 jours ouvrables", "CCE référé", "OQT exécutoire imminent" |
| `detention-centre-ferme-be` | `detention_centre_ferme_detectee` | "centre fermé", "127bis", "Caricole", "Vottem", "Holsbeek", "Merksplas", "détention administrative" |
| `aesm-admission-exceptionnelle-mineur` | `mineur_non_accompagne_be_detecte` | "MENA", "mineur étranger non accompagné", "tutelle DGDE", "Service des Tutelles" |
| `asile-protection-subsidiaire-be` | `subsidiaire_envisagee` | "protection subsidiaire", "art. 48/4", "atteinte grave", "conflit armé", "directive 2011/95" |
| `protection-temporaire-ukraine-be` | `protection_temporaire_ukraine_detectee` | "Ukraine", "protection temporaire", "directive 2001/55", "57/29" |
| `asile-procedure-acceleree-be` | `procedure_acceleree_asile_be_detectee` | "procédure accélérée", "57/6/1", "pays sûr", "demande tardive", "fraude" |
| `dublin-iii-be-determination` | `transfert_dublin_envisage` | "Dublin", "604/2013", "État membre responsable", "transfert", "reprise en charge" |
| `nationalite-declaration-art-12bis` | `naturalisation_be_envisagee` | "12bis", "déclaration de nationalité", "5 ans séjour", "code nationalité belge", "intégration A2" |
| `etudiant-changement-statut-fin-etudes` | `changement_statut_etudiant_travail_be` | "fin études", "changement statut", "single permit post-études", "diplôme BE" |
| `interdiction-entree-be` | `interdiction_entree_be_detectee` | "interdiction d'entrée", "Annexe 13quinquies", "74/11", "IE 3 ans", "IE 5 ans" |
| `expulsion-art-20-22-be` | `expulsion_be_envisagee` | "expulsion", "art. 20", "art. 21", "art. 22", "ordre public grave" |
| `regroupement-conjoint-violences` | `violences_conjugales_regroupement_detectees` | "violences conjugales", "maintien droit séjour", "art. 11 §2 al. 4", "rupture cohabitation pour violences" |

Ces flags sont à intégrer au prompt Sonnet `DROIT_IMMIGRATION` BE en parallèle de la livraison des outils correspondants (pas de flag orphelin sans outil).

---

## 5. Synthèse chiffrée

### 5.1 — État actuel Immigration BE

| Catégorie | Nombre |
|---|---|
| Outils Immigration BE actifs (Tableau A) | **9** (5 BE-only + 4 transversaux) |
| Outils Immigration FR actifs (sans pendant BE) | 7 (F-IM-08 OQTF FR, F-IM-09 AES, F-IM-11 changement statut, F-IM-12 asile avancé, F-IM-13 naturalisation, F-IM-17 régime algérien, F-IM-19 mineurs, F-IM-20 mesures éloignement) |
| Outils Immigration BE manquants (Tableau B § 3.1 à § 3.13) | **~75** situations distinctes recensées |

### 5.2 — Top 10 manquants Immigration BE (par priorité)

| Rang | tool_id proposé | Section | Priorité | Justification |
|---|---|---|---|---|
| 1 | `aesm-admission-exceptionnelle-mineur` + `mena-tutelle-dgde` | § 3.6 | P1 P3 BE-only | MENA = catégorie de droit BE entière non couverte. Tutelle DGDE + projet de vie + AESM. Aucun équivalent FR direct. Volume non négligeable en cabinet. |
| 2 | `single-permit-procedure-complete` | § 3.9 / § 3.1 | P1 P3 BE-only | Procédure travail+séjour BE central depuis 2018. Régionalisée. Aucun équivalent FR. Très demandé. |
| 3 | `nationalite-declaration-art-12bis` | § 3.10 | P2 P3 BE-only | Voie principale d'acquisition nationalité belge — équivalent F-IM-13 FR. Très fréquent. |
| 4 | `cce-recours-annulation-procedure` + `cce-recours-extreme-urgence-procedure` | § 3.11 | P1 P3 BE-only | Outils de procédure CCE dédiés (au-delà du générateur générique F-IM-06). Délais 30 j calendaires / 5 j ouvrables. Risque forclusion élevé. |
| 5 | `detention-centre-ferme-be` + `detention-recours-chambre-conseil-be` | § 3.5 | P1 P3 BE-only | Détention administrative — recours chambre du conseil 5 jours. Aucun équivalent FR direct. Critique en urgence. |
| 6 | `regroupement-10bis-conjoint-tiers` + `regroupement-10ter-conjoint-tiers-illimite` | § 3.2 | P2 P3 BE-only | Compléter F-IM-14-40ter (Belge) par les régimes ressortissant tiers. Très fréquent. |
| 7 | `asile-protection-subsidiaire-be` + `dublin-iii-be-determination` + `asile-procedure-acceleree-be` | § 3.4 | P1 P3 BE-only | Volet asile non couvert (alors que F-IM-12 FR couvre Dublin/accélérée/réexamen/apatride). Trou majeur. |
| 8 | `annexe-13quinquies-oqt-interdiction-entree` + `interdiction-entree-be-recours-art-74-12` | § 3.5 | P1 P3 BE-only | OQT + IE distinct de l'OQT simple. IE 3/5/8 ans. Recours en levée. |
| 9 | `etudiant-prorogation-r58` + `etudiant-changement-statut-fin-etudes` | § 3.8 | P2 P3 BE-only | Très fréquent — étudiant chinois / africain en BE qui passe vers travail. Critère résultats académiques. |
| 10 | `protection-temporaire-ukraine-be` | § 3.4 | P2 P3 BE-only | Régime actif depuis 2022, prolongé annuellement. Toujours actif 2026. Cas de masse. |

### 5.3 — Outils BE-only sans équivalent FR (preuve d'indépendance topologique)

L'audit identifie au moins **20 outils BE-only** sans correspondance FR directe — preuve que le miroir FR n'aurait pas suffi :

1. `9bis-humanitaire-be` (différent de l'AES FR — pas une régularisation par le travail) — EXISTE
2. `9ter-medical-be` (différent du titre étranger malade FR — pas de présomption gravité) — EXISTE
3. `40ter-familial-belge-be` (regroupement avec un Belge — différent VPF FR) — EXISTE
4. `aesm-admission-exceptionnelle-mineur` (MENA + tutelle DGDE — aucun équivalent FR direct)
5. `mena-tutelle-dgde` (tutelle Service des Tutelles SPF Justice — différent des MNA FR)
6. `single-permit-procedure-complete` (autorisation unique régionalisée — pas d'équivalent VLS-TS direct FR)
7. `carte-h-brexit` (ressortissants UK avant 2021 — régime BE spécifique)
8. `carte-f-membre-famille-ue` / `carte-fplus-rlue-membre-famille` (régimes carte F BE — différents membres famille UE FR) — EXISTE pour 40bis
9. `cce-recours-annulation-procedure` / `cce-recours-extreme-urgence-procedure` / `cce-recours-suspension-procedure` (CCE = juridiction sui generis BE, pas TA FR)
10. `detention-centre-ferme-be` / `detention-recours-chambre-conseil-be` (recours chambre du conseil — pas JLD FR)
11. `regroupement-cohabitation-legale-be` (cohabitation légale art. 1475 CC BE — différent PACS FR)
12. `nationalite-declaration-art-12bis` (Code nationalité belge — différent Cciv FR)
13. `nationalite-naturalisation-art-19-21` (décret Chambre des représentants — différent décret FR)
14. `victime-traite-etres-humains-be` (régime PAG-ASA / Sürya / Payoke spécifique)
15. `protection-temporaire-ukraine-be` (transposition spécifique BE de la directive 2001/55)
16. `etudiant-prorogation-r58` / `etudiant-changement-statut-fin-etudes` (régime BE spécifique étudiants)
17. `regroupement-conjoint-violences` (art. 11 §2 al. 4 BE — protection spécifique)
18. `delais-be-jours-ouvrables-vs-calendaires` (calculateur BE-spécifique — utilisé par annexe 13)
19. `transfert-dublin-be` (modalités CCE BE-spécifiques — différent recours TA FR)
20. `regularisation-2009-historique` (instruction Vandeurzen — historique BE-only)

---

## 6. Découpages à éclater (recommandations méthodologiques)

Plusieurs outils proposés au Tableau B mélangent des situations qui devraient être éclatées par souci de lisibilité métier (invariant CLAUDE.md "un outil = une situation") :

### 6.1 — `F-IM-14-9bis-humanitaire-be` actuel
Couvre la procédure 9bis générique, mais les **circonstances exceptionnelles** elles-mêmes ont une jurisprudence riche (santé, enfants scolarisés en BE, atteintes art. 8 CEDH, présence prolongée, intégration). Spin-off envisageable :
- `9bis-circonstances-medicales` — quand les circonstances exceptionnelles sont médicales mais hors 9ter (par ex. accompagnant proche malade)
- `9bis-enfants-scolarises-be` — circonstance exceptionnelle dominante : scolarisation longue d'enfants en BE
- `9bis-presence-prolongee-be` — séjour prolongé > 5 ans + enracinement

Décision : laisser **F-IM-14-9bis-humanitaire-be** comme outil "scoring agrégé" et n'éclater **que** si le retour utilisateur le demande. Spin-offs en backlog sans urgence.

### 6.2 — Recours CCE
Aujourd'hui dispersés : F-IM-06 (générateur générique) + F-IM-08-annexe13-be (calcul délais Annexe 13 + recours). Manque :
- `cce-recours-annulation-procedure` (procédure complète avec mémoire en réplique)
- `cce-recours-suspension-procedure` (référé administratif)
- `cce-recours-extreme-urgence-procedure` (5 jours ouvrables)

Ces 3 outils sont distincts (procédures, délais, conditions différents) — ne pas fusionner.

### 6.3 — Étudiants
Régime étudiant BE comporte plusieurs situations : autorisation initiale (Annexe 47), prorogation (R.58), APS post-études, droit travail 20h. Découpage en 4 outils distincts plutôt qu'un seul.

### 6.4 — Naturalisation BE
Code nationalité belge a 4 voies distinctes (déclaration 12bis, naturalisation 19+, mineur 11/13, mariage 16). Ne **pas** fusionner — chaque voie a ses propres conditions. Pattern à appliquer : 4 outils distincts (parité avec F-IM-13 FR qui a 6 voies).

### 6.5 — Regroupement familial
3 régimes distincts : 40ter (Belge — déjà couvert), 40bis (citoyen UE — déjà couvert), 10/10bis/10ter (ressortissant tiers — MANQUE). Ne pas fusionner les 10/10bis/10ter en un seul outil — distinguer "tiers en séjour limité" et "tiers en séjour illimité" car conditions différentes.

---

## 7. Hors périmètre / honnêteté méthodologique

### 7.1 — Références à vérifier par avocat BE

Plusieurs références dans ce tableau sont annotées implicitement "à vérifier" en pratique :
- la version courante de l'AR 02/09/2018 single permit (modifications successives possibles)
- la portée exacte de la circulaire 19/07/2009 Vandeurzen (régularisation 2009 — abrogée mais cas anciens)
- le délai exact recours visa C devant CCE (60 jours évoqué — à confirmer art. exact)
- le seuil exact ressources regroupement familial (120 % RIS × 1,5 — varie chaque année)
- la liste pays origine sûrs BE (mise à jour annuelle, ne pas hardcoder dans le code — pointer vers AR ou table)
- l'AR 23/04/2018 APS recherche emploi étudiant — référence à confirmer (peut-être autre date)
- la dernière version de la loi 27/02/2019 apatride (potentielles modifications)

Un avocat belge spécialisé doit valider les outils avant seed.

### 7.2 — Volets non couverts par cet audit

- **Régionalisation politique migration travail** (compétences Région wallonne, flamande, bruxelloise depuis 2014) — nécessite un volet spécifique pour single permit.
- **Parcours d'intégration régionalisés** (Wallonie, Flandre Inburgering, Bruxelles) — partiellement abordé § 3.13 mais mériterait approfondissement.
- **Citoyens UE radiation Carte E / EU+** — § 3.7 traité partiellement.
- **Rétention en zone de transit aéroport** (zone Z Bruxelles-National) — non traitée. Régime spécifique mais cas rare.
- **Cas particuliers diplomatiques / fonctionnaires internationaux** (immunités) — non traités. Ressort du protocole, pas du droit ordinaire.
- **Procédures DVZ flamand / OE francophone** — terminologie linguistique non traitée (DVZ = Office des Étrangers en flamand, même institution).
- **Loi sur le séjour des Roms / minorités spécifiques** — non couverte. Sujet très rare et potentiellement traité via 9bis humanitaire.

### 7.3 — Pas de miroir FR forcé

L'audit a explicitement évité de miroir le tableau B Immigration FR. Certains outils FR n'ont délibérément pas d'équivalent BE listé :
- `F-IM-09-aes-metiers-tension` / `aes-famille` / `aes-humanitaire` / `aes-etudiant` (FR) → en BE, l'équivalent fonctionnel pertinent est **9bis** (humanitaire) ou **single permit** (travail) — pas un miroir 1:1.
- `F-IM-17-regime-algerien` (FR — accord franco-algérien 1968) → aucun équivalent BE.
- `F-IM-11-changement-statut` (FR — CESEDA L.412-1) → l'équivalent BE est diffusé entre prorogation carte A, single permit (changement vers travail), 9bis (changement de motif).
- `F-IM-19-mineurs` (FR — MNA + L.435-3 + DCEM + TIR) → l'équivalent BE est **MENA + AESM + tutelle DGDE** — situation BE-only.

### 7.4 — Conseil du Contentieux des Étrangers (CCE) vs Conseil consultatif des Étrangers
**Attention à l'homonymie** : "CCE" peut désigner deux organismes distincts en BE :
1. **Conseil du Contentieux des Étrangers** (CCE — juridiction administrative — loi 15/09/2006 — recours OE/CGRA) — celui visé par cet audit.
2. **Commission consultative des Étrangers** (CCE — organisme consultatif préalable expulsion art. 20-22) — différent.

Aucune confusion dans cet audit, mais le code et la documentation doivent rester explicites (utiliser "Conseil du Contentieux" et non "CCE" seul).

---

## 8. Conclusion

L'écosystème Immigration BE compte aujourd'hui **9 outils décisionnels actifs** (Tableau A) sur un périmètre théorique d'environ **75 situations distinctes** justifiant un outil dédié (Tableau B). Le déficit principal est sur les outils **BE-only sans équivalent FR** (MENA / AESM / tutelle DGDE, single permit régionalisé, recours CCE annulation/suspension/extrême urgence, détention centre fermé + recours chambre du conseil, naturalisation Code 28/06/1984, regroupement 10bis/10ter ressortissant tiers, asile protection subsidiaire BE, Dublin BE, accélérée BE, protection temporaire Ukraine, étudiants Annexe 47/R.58/APS, interdiction d'entrée Annexe 13quinquies). Ces outils ne peuvent pas être obtenus par "miroir" depuis le FR — ils nécessitent un travail juridique BE original.

Le **Top 10** prioritaire mélange :
- **6 urgences procédurales** (recours CCE 30 j / 5 j ouvrables, détention centre fermé 5 j, asile dublin, OQT + IE, prorogation carte A étudiant),
- **3 outils à forte spécificité BE-only** (MENA + AESM + tutelle DGDE, single permit régionalisé, naturalisation Code 28/06/1984),
- **1 outil à fréquence haute liée à un régime de masse actif** (protection temporaire Ukraine).

**Volet F-166 Immigration BE (Tableau C)** : 5 outils Immigration BE actuellement en ALWAYS_ON gagneraient à basculer en CONTEXTUAL via 5 nouveaux booleans IA (`procedure_9bis_envisagee`, `procedure_9ter_medicale_detectee`, `regroupement_40bis_detecte`, `regroupement_40ter_detecte`, `oqt_annexe13_detectee`). Pattern strictement aligné sur F-166 Travail FR (SF-166-01 prompts + SF-166-02 migration). Les 4 outils transversaux (`F-IM-05`, `F-IM-01`, `F-IM-06`, `F-IM-07`) restent en ALWAYS_ON car structurants tout dossier immigration. Cette feature jumelle BE de F-166 (à nommer p. ex. `F-191-01` ou `F-166-bis`) résorberait la dette de symétrie avant l'expansion par les outils MANQUE du Tableau B.

Pour valider cet audit et le transformer en backlog, l'utilisateur doit :
1. confirmer la pertinence du **Top 10** (ou en ajuster l'ordre) — démarrage suggéré par MENA/AESM (P1 BE-only) + single permit (P1 BE-only) qui ferment les deux gros trous fonctionnels ;
2. faire valider les références juridiques marquées "à vérifier" par un avocat BE spécialisé en droit des étrangers ;
3. décider si chaque outil MANQUE sera livré en SF dédiée (ex. SF dédiée AESM) ou en feature jumelle structurée (ex. F-IM-21 BE étendue regroupement = SF1 10bis, SF2 10ter, SF3 cohabitation légale, SF4 conjoint violences) ;
4. trancher la priorité de **F-166 BE Immigration (Tableau C)** : avant ou après l'expansion outils MANQUE ? (Recommandation : avant, car bascule peu coûteuse + clarifie l'UX du panel F-IA-04 sur les dossiers BE).

Cette base prépare une vague de features F-IM-21 → F-IM-40 (et plus) cohérente avec l'invariant "un outil = une situation métier" et fidèle à la topologie juridique BE.
