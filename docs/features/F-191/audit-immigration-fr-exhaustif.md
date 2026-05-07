# Audit juridique exhaustif — Outils décisionnels Droit français des étrangers (Immigration FR)

**Auteur** : LegalCase — automatique (audit F-191, partie Immigration FR)
**Date** : 2026-05-06
**Périmètre** : droit français des étrangers — CESEDA (Code de l'entrée et du séjour des étrangers et du droit d'asile), accord franco-algérien 1968, Code civil articles 17 à 33-2 (nationalité), conventions internationales (Convention Genève 1951, Convention New York 1989). **Hors périmètre** : Belgique (couvert par migrations 118/123/125/126 + F-IM-14, audit séparé), droit du travail, droit de la famille.
**Méthode** : départ des **sources juridiques françaises** (CESEDA + Cciv + lois récentes), pas du miroir BE. Les outils FR-only (préfecture, OFPRA, CNDA, CRA, JLD, AES, ANEF, recours OQTF 48 h-30 j, IRTF, IAT) sont valorisés à part. Le miroir BE n'est pas exigé pour Immigration FR (les deux pays ont des architectures distinctes — F-IM-14 traite BE).
**Sortie** : Tableau A (existant), Tableau B (audit exhaustif), Tableau C (audit F-166 Immigration FR — quels ALWAYS_ON devraient devenir CONTEXTUAL), synthèse chiffrée et Top 10 priorité.

---

## 1. Contexte et avertissement méthodologique

### 1.1 — Pourquoi un audit Immigration FR maintenant

Côté droit du travail, F-166 (PR #718 + migrations 193 et 199) a introduit 8 flags IA `*_detecte` extraits par Sonnet 3.5 (`rappel_salaire_detecte`, `travail_dissimule_detecte`, `clause_non_concurrence_detectee`, `statut_protege_detecte`, `transaction_envisagee`, `at_mp_detecte`, `urgence_procedurale`, `contestation_are_envisagee`). Ces flags pilotent la conversion `ALWAYS_ON → CONTEXTUAL` pour 8 outils Travail FR. Résultat : un avocat travaillistique ne voit plus 27 outils par défaut mais 3 outils essentiels (F-DT-03, F-DT-04, F-DT-07), les 24 autres apparaissant uniquement quand l'IA a détecté la situation.

Côté Immigration FR, **aucun équivalent F-166** n'existe au 2026-05-06. La migration 105 + 106 ne sait afficher conditionnellement que sur `type_titre_sejour_code` (F-IM-01) et `type_recours_code` (F-IM-06) ; les migrations 116/117/146 utilisent `type_procedure_detectee` pour F-IM-08 OQTF / référés. Tout le reste est ALWAYS_ON. Conséquence : un avocat ouvrant un dossier immigration FR voit **les 14 outils Immigration FR par défaut** (4 transversaux + 10 FR ALWAYS_ON), même s'il s'agit d'un simple renouvellement APS sans rétention, sans OQTF, sans asile, sans MNA. C'est exactement le pattern dont F-166 a sauvé Travail.

L'objectif de cet audit :
1. **Étape 1** — inventorier les outils Immigration FR existants (tableau A).
2. **Étape 2** — produire l'audit juridique exhaustif (tableau B) à partir des sources CESEDA, lois récentes (Collomb 2018, Darmanin 2024), procédures OFPRA/CNDA/préfecture/JLD/CRA, et identifier les manquants prioritaires.
3. **Étape 3** — proposer les flags IA Immigration FR qui permettront de basculer les ALWAYS_ON les plus volumineux vers CONTEXTUAL (équivalent F-166 pour Immigration).
4. **Étape 4** — synthèse chiffrée + Top 10 + plan d'éclatement.

### 1.2 — Avertissements

- Toutes les références CESEDA (livre, titre, article) sont issues des connaissances générales du modèle. Le CESEDA a été recodifié en 2021 (ordonnance 2020-1733 du 16/12/2020 entrée en vigueur 01/05/2021) ; la nouvelle numérotation est utilisée systématiquement (ex. L. 423-1 pour la VPF, ancien L. 313-11 7°). Les références dont le modèle n'est pas certain à 100 % sont annotées **"(à vérifier)"** — un avocat en droit des étrangers doit confirmer avant de seeder.
- La loi Darmanin du 26/01/2024 « pour contrôler l'immigration, améliorer l'intégration » a ajouté l'AES travail métiers en tension (L. 435-3 nouveau), durci les conditions de l'AVS, raccourci le délai de recours OQTF 30 j, supprimé l'AME pour les irréguliers dans certains cas. Ses dispositions sont signalées « loi 2024 ».
- **Pas de logique miroir BE** dans cet audit (consigne explicite). Quand un outil FR n'a pas d'équivalent BE pertinent, c'est simplement noté pour information.

### 1.3 — Échelle de priorité

- **P1 — urgence procédurale** : un délai court irréversible expose le client à perdre son droit (recours OQTF 48 h sans délai / 15 j détention / 30 j avec délai ; recours CNDA 1 mois ; recours JLD 24 h).
- **P2 — fréquence haute** : situation rencontrée plusieurs fois par mois par tout avocat en droit des étrangers (renouvellement, AES, regroupement familial, refus visa).
- **P3 — spécificité FR** : pas d'équivalent BE pertinent ou complexité forte (régime algérien 1968, Mayotte, Saint-Martin, statuts dérogatoires, AES métiers tension loi 2024).
- **P4 — confort** : utile mais on peut différer sans perte de couverture immédiate.

### 1.4 — Définition d'un « outil décisionnel »

Conforme à la règle CLAUDE.md « un outil = une situation juridique distincte ». Sept niveaux de profondeur :
1. Checklist (F-IM-01)
2. Générateur de document (F-IM-06)
3. Calculateur (délais, indemnités, durée séjour)
4. Arbre décisionnel (F-IM-05)
5. Scoring / analyseur de validité (F-IM-08)
6. Comparateur / fourchettes
7. Détection d'événement déclencheur (asile, rétention, AES, urgence)

---

## 2. Tableau A — Outils Immigration FR existants

Source : migrations Liquibase 105 (seed initial), 106 (passage CONTEXTUAL→ALWAYS_ON pour F-IM-01/06), 116, 117, 119-122, 146, 170-174, 176, 191 (réalignement IDs), 192 (restauration). Croisement avec `TOOL_REGISTRY` dans `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (lignes 548-1290 pour les entrées F-IM-*).

**Synthèse 2026-05-06** : 17 outils Immigration FR câblés en tout — 4 transversaux country=NULL ALWAYS_ON (s'appliquent FR + BE), 10 FR-explicit ALWAYS_ON, 3 FR-explicit CONTEXTUAL (F-IM-08 OQTF + référés).

| tool_id | layer | trigger_field / trigger_value | Frontend câblé (TOOL_REGISTRY) | Situation juridique couverte |
|---|---|---|---|---|
| `F-IM-01-checklist-pieces` | ALWAYS_ON (transversal NULL) | — | OUI (`ImmigrationChecklistSectionComponent`) | Checklist pièces par type de titre. Référentiel `ImmigrationPieceReferentiel` 16 titres FR (VLS_TS_ETUDIANT, VLS_TS_SALARIE, CST_SALARIE, CARTE_PLURIANNUELLE, CARTE_RESIDENT, APS, CST_VPF, RECEPISSE_ASILE) + 8 BE (CARTE_A_TRAVAIL, CARTE_A_ETUDES, CARTE_A_FAMILLE, CARTE_B, CARTE_C, PERMIS_UNIQUE, ANNEXE_15, ATTESTATION_IMMATRICULATION). Persistée DB `immigration_piece_checks`. F-IM-21 ajoute critères binaires de validité. |
| `F-IM-05-arbre-decisionnel-titre` | ALWAYS_ON (transversal NULL) | — | OUI (`ImmigrationTitleDecisionSectionComponent`) | Arbre de décision orientant vers un type de titre (16 FR + 8 BE) selon nationalité, motif, situation familiale (F-IM-18), durée. Référentiel CESEDA L. 421+ / L. 423+ / L. 425+ / L. 426+ + Loi BE 15/12/1980. Pré-fill IA + F-IA-03 sur MOTIF, NATIONALITE. |
| `F-IM-06-recours` | ALWAYS_ON (transversal NULL) | — | OUI (`ImmigrationRecoursSectionComponent`) | Générateur de recours pré-rempli (gracieux préfet, contentieux TA, CNDA pour FR ; CGRA, CCE, CE pour BE). 6 types codes recours. Visa des textes, délais calculés depuis la notification, pièces à joindre. Pré-fill IA + F-IA-03. |
| `F-IM-07-droit-au-travail` | ALWAYS_ON (transversal NULL) | — | OUI (`ImmigrationWorkRightSectionComponent`) | Détermination automatique droit au travail selon le titre : APT / autorisation incluse (FR), permis unique (BE). Obligations employeur (vérification préfecture FR, Dimona BE). |
| `F-IM-08-oqtf-avec-delai-fr` | CONTEXTUAL FR | `type_procedure_detectee = OQTF_AVEC_DELAI` | OUI (`oqtf-avec-delai-section`) | OQTF avec délai de départ volontaire 30 j — recours TA L. 614-5, suspension automatique, motifs de contestation. **FR-only**. |
| `F-IM-08-oqtf-sans-delai-fr` | CONTEXTUAL FR | `type_procedure_detectee = OQTF_SANS_DELAI` | OUI (`oqtf-sans-delai-section`) | OQTF sans délai (urgence absolue) — recours TA 48 h, audience JLD si rétention, motifs de contestation. **P1 FR-only**. |
| `F-IM-08-referes-admin-fr` | CONTEXTUAL FR | `type_procedure_detectee = OQTF_AVEC_DELAI` | OUI (`referes-admin-section`) | Référés-suspension L. 521-1 + référé-liberté L. 521-2 CJA. 2 scores parallèles. Palette rouge dominant urgence 48 h. **P1 FR-only**. |
| `F-IM-09-aes-metiers-tension` | ALWAYS_ON FR | — | OUI (`aes-metiers-tension-section`) | Admission exceptionnelle au séjour métiers en tension — L. 435-3 (nouveau, loi 2024). Liste métiers tension par région. **P3 FR-only loi 2024**. |
| `F-IM-09-aes-famille` | ALWAYS_ON FR | — | OUI (`aes-famille-section`) | AES motif familial — L. 435-1 + circulaire Valls 28/11/2012. Critères : 5 ans présence, enfant scolarisé, conjoint régulier. **FR-only**. |
| `F-IM-09-aes-humanitaire` | ALWAYS_ON FR | — | OUI (`aes-humanitaire-section`) | AES motif humanitaire — L. 435-1 + circulaire Valls. Critères : 10 ans présence, intégration, considérations humanitaires exceptionnelles. **FR-only**. |
| `F-IM-09-aes-etudiant` | ALWAYS_ON FR | — | OUI (`aes-etudiant-section`) | AES motif études — circulaire Valls + L. 435-1. Anciens étudiants en situation irrégulière, parcours universitaire FR. **FR-only**. |
| `F-IM-11-changement-statut` | ALWAYS_ON FR | — | OUI (composant dédié) | Changement de statut CESEDA L. 412-1 + R. 5221+ — 12 transitions (étudiant→salarié, salarié→VPF L. 423-1/-7/-8, etc.). Critères durée restante + SMIC × 1,5 + casier vierge. Pré-fill IA `titreActuel`, `dureeRestanteSurTitreActuelMois` depuis `dateExpirationTitre`, F-IA-03 sur TITRE_ACTUEL. **FR-only** (BE = backlog jumeau). |
| `F-IM-12-asile-avance` | ALWAYS_ON FR | — | OUI (`asile-avance-section`) | 5 dispositifs CESEDA Livre V + Règlement UE 604/2013 : Dublin III, procédure accélérée L. 531-24+, réexamen L. 531-42+, apatridie L. 561-1+, protection subsidiaire L. 512+. Pré-fill IA + F-IA-03 sur DISPOSITIF_ASILE. **FR-only**. |
| `F-IM-13-naturalisation` | ALWAYS_ON FR | — | OUI (`naturalisation-section`) | Cciv 6 voies : DECRET 21-15 (5 ans résidence), MARIAGE L. 21-2 (4 ans union), ASCENDANT L. 21-13-1, MINEUR 22-1, REINTEGRATION 24-1+, OPPOSITION. Verdict + chips critères non remplis + documents à fournir. F-IA-03 sur VOIE_NATURALISATION. **FR-only** (BE = CNB art. 12bis+, backlog). |
| `F-IM-17-regime-algerien` | ALWAYS_ON FR | — | OUI (composant dédié) | Régime algérien — accord franco-algérien du 27/12/1968 modifié (5 voies CRA = certificat de résidence algérien). Régime dérogatoire au CESEDA. **P3 FR-only**. |
| `F-IM-19-mineurs` | ALWAYS_ON FR | — | OUI (`mineurs-immigration-section`) | 4 dispositifs : MNA (mineur non accompagné — ordonnance JE art. 375 Cciv ; ASE), L. 435-3 enfant né en France, DCEM (document de circulation pour étranger mineur — R. 321-3), TIR (titre d'identité républicain — R. 321-7). Bandeau bloquant majorité. **FR-only**. |
| `F-IM-20-mesures-eloignement` | ALWAYS_ON FR | — | OUI (`mesures-eloignement-section`) | 3 dispositifs distincts de l'OQTF : EXPULSION (préfectorale/ministérielle/sécurité État L. 631-1+), IRTF (interdiction de retour sur le territoire français L. 612-6+), IAT (interdiction administrative du territoire L. 222-1+). Verdict VALIDE/CONTESTABLE/NUL + délais recours adaptés. F-IA-03. **FR-only**. |

**Total au 2026-05-06 : 17 outils Immigration FR seedés et câblés frontend** (14 ALWAYS_ON + 3 CONTEXTUAL). Sur les 14 ALWAYS_ON, **4 sont transversaux country=NULL** (s'appliquent aussi BE) et **10 sont FR-explicit**.

### 2.1 — Constat F-166 sur Immigration FR

Sur les 17 outils ci-dessus :

- Le seul mécanisme CONTEXTUAL utilisé (F-IM-08) repose sur **`type_procedure_detectee`** (extraction explicite de la procédure dans `ImmigrationExtractedData`).
- Aucun flag boolean `*_detecte` du type F-166 (Travail) n'existe. Les 10 ALWAYS_ON FR-explicit s'affichent **par défaut sur tout dossier Immigration FR**, peu importe le contexte (renouvellement, MNA, OQTF, asile, naturalisation, AES — tout coexiste).
- L'avocat reçoit donc **par défaut en immigration FR : 4 transversaux + 10 FR-explicit + 0-3 contextuels OQTF = 14 cards minimum**. Comparable au scénario Travail FR pré-F-166 (27 cards). Ergonomie médiocre.
- 3 outils particulièrement bruyants en ALWAYS_ON : `F-IM-19-mineurs` (le client n'est pas mineur dans 95 % des dossiers), `F-IM-20-mesures-eloignement` (pas d'éloignement dans 80 % des dossiers), `F-IM-12-asile-avance` (pas d'asile dans 90 % des dossiers d'avocats généralistes).

C'est l'angle d'attaque de l'**Étape 3** (audit F-166 Immigration FR).

---

## 3. Tableau B — Audit juridique exhaustif Immigration FR

Une ligne = une situation juridique distincte qui mérite un outil décisionnel autonome (règle CLAUDE.md). Les outils déjà existants en Tableau A sont signalés **EXISTE**. Les autres sont **MANQUE** avec priorité.

L'audit suit la structure du CESEDA recodifié 2021 (livres I à IX) plus les régimes spéciaux (Cciv nationalité, accord franco-algérien, conventions internationales, juridictions).

### 3.1 — Livre I CESEDA — Entrée et séjour de courte durée (visa C)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `visa-c-court-sejour-eligibilite` | Conditions visa Schengen 90 j (C) — code visas UE 810/2009 + L. 311+ CESEDA | Règlement UE 810/2009 ; L. 311-1+ CESEDA | Checklist + arbre décisionnel | MANQUE | **P3** | Pas couvert. Demande visa court séjour à l'ambassade. Inclure motifs refus visa. |
| `recours-refus-visa-crrv` | Recours contre refus de visa devant la CRRV (Commission de recours contre les décisions de refus de visa d'entrée en France) | L. 312-1+ CESEDA ; D. 312-3 ; arrêté 10/03/2011 | Générateur recours + checklist | **MANQUE** | **P1** | Délai 2 mois post-notification refus. Préalable obligatoire à la saisine TA Nantes (compétence exclusive). Très demandé. **Couvre un délai court irréversible**. À ne pas confondre avec F-IM-06-recours (généraliste). |
| `crrv-vs-ta-nantes-strategie` | Choix entre CRRV (préalable) et saisine TA Nantes pour contestation refus visa | L. 312-1 ; CE jurisprudence Conseil d'État | Comparateur stratégique | MANQUE | P3 | Spécifique FR (TA Nantes a la compétence territoriale exclusive pour refus visa). |
| `vls-ts-validation-ofii-FR` | Validation VLS-TS auprès de l'OFII (visa long séjour valant titre de séjour) — démarche en ligne ANEF dans les 3 mois | R. 311-3 ; R. 311-13+ | Checklist + délais | **MANQUE** | **P1** | Délai 3 mois après entrée en France. Si non validé → titre invalide → irrégulier au bout de 3 mois. **Très demandé pour conjoints, étudiants, salariés détachés**. Pas couvert. |

### 3.2 — Livre II CESEDA — Droit au séjour (titres de séjour)

#### 3.2.1 — Titre travailleur (L. 421+)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | Orientation vers titre adapté (incl. salarié, étudiant, VPF, etc.) | L. 421+ ; L. 422+ ; L. 423+ ; L. 425+ ; L. 426+ | Arbre décisionnel | EXISTE | — | Couvert. |
| `F-IM-01-checklist-pieces` | Checklist pièces par type de titre (16 codes) | Référentiel `ImmigrationPieceReferentiel` | Checklist | EXISTE | — | Couvert. |
| `F-IM-21` (référentiel) | Critères binaires de validité dossier par type de titre | F-IM-21 PRs #714/715/716 | Référentiel + validation | EXISTE | — | Couvert (livré 2026-04-30). Pattern miroir F-DT-08. |
| `passeport-talent-l421-9-eligibilite` | Passeport Talent — 10 sous-catégories (chercheur, salarié qualifié, entrepreneur, innovant, investisseur, profession artistique, grands concours, renommée, famille, salarié en mission) | L. 421-9+ ; F-IM-10 (sous-catégories) | Arbre décisionnel + checklist | **MANQUE séparément** | P3 | F-IM-10 a livré le référentiel des sous-catégories ABSORBÉ dans `immigration-title-decision-section`. **Découper en sous-cards cliquables** est reporté V2 (F-IM-10 mini-spec). À envisager comme outil dédié si demande métier. |
| `salarie-detache-ict-eligibilite` | Salarié détaché ICT (intra-corporate transfer) — L. 421-26+ | L. 421-26 à L. 421-31 (à vérifier numérotation) ; directive UE 2014/66 | Arbre décisionnel + checklist | **MANQUE** | P3 | Cas spécifique grands groupes. |
| `salarie-saisonnier-eligibilite` | Travailleur saisonnier — L. 421-32+ (à vérifier) | L. 421-32+ ; AR | Arbre décisionnel | MANQUE | P3 | Régimes spécifiques agriculture, hôtellerie. |
| `commercant-profession-non-salariee-eligibilite` | Commerçant / profession non salariée — L. 421-5+ (à vérifier) | L. 421-5 (à vérifier) | Arbre décisionnel | MANQUE | P4 | Pas une priorité — peu demandé. |
| `salarie-eligibilite-renouvellement` | Renouvellement carte salarié — autorisation de travail, contrat, ressources L. 433-1 | L. 433-1+ ; R. 5221-20 | Checklist + critères validité | **MANQUE séparément** | **P2** | Renouvellement = cas le plus fréquent. Aujourd'hui couvert partiellement par F-IM-01 + F-IM-21. Mériterait outil dédié (date dépôt 2 mois avant expiration, justificatifs, etc.). |
| `autorisation-travail-employeur-l421-1` | Autorisation de travail employeur — démarche préalable (L. 5221+ Code du travail) | L. 5221-1 à L. 5221-12 Code travail ; L. 421-1 CESEDA | Checklist employeur + obligations | MANQUE | P2 | Côté employeur (recrutement étranger). Touché partiellement par F-IM-07. |

#### 3.2.2 — Titre vie privée et familiale (L. 423+)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `vpf-conjoint-francais-l423-1` | VPF conjoint de Français — communauté de vie + 6 mois mariage | L. 423-1 | Critères binaires | EXISTE (F-IM-21) | — | Couvert dans F-IM-21 IM21_PIECES_MARIAGE_FR + IM21_COMMUNAUTE_VIE_FR. |
| `vpf-parent-enfant-francais-l423-7` | VPF parent d'enfant français — contribution à l'entretien et à l'éducation | L. 423-7 ; L. 423-8 | Analyseur validité + critères | EXISTE (F-IM-21) | — | Couvert dans F-IM-21. |
| `regroupement-familial-r421` | Regroupement familial — préfecture, OFII, conditions ressources + logement | L. 434-1+ ; R. 434-1+ | Arbre décisionnel + checklist | **MANQUE** | **P2** | Aujourd'hui dans F-IM-15 (calendrier) + F-IM-01 (pièces) mais **pas d'outil dédié à l'éligibilité** (ressources SMIC, logement décent, durée séjour 18 mois, etc.). Très demandé. |
| `regroupement-familial-conditions-ressources` | Calcul ressources regroupant + smic + ASF + APL | L. 434-7 ; R. 434-30 | Calculateur ressources | MANQUE | **P2** | Sous-outil utile : calcul détaillé des ressources prises en compte (PAS APL, PAS RSA, etc.). |
| `regroupement-sur-place-derogation` | Régularisation regroupement sur place (déjà en France) — dérogation procédure | R. 434-7 ; jurisprudence CE | Analyseur dérogation | MANQUE | P3 | Cas de dérogation à la procédure normale (membre de famille déjà sur place). |
| `vpf-liens-personnels-familiaux-l423-23` | VPF liens personnels et familiaux — preuve résidence habituelle 5 ans, intensité liens | L. 423-23 (ancien L. 313-11 7°) | Analyseur validité + scoring | **MANQUE** | **P2** | Très demandé. Regroupe ce qu'on appelait « 10 ans / 7° ». Nécessite démonstration. Critères jurisprudentiels denses (conjoint, enfants, parents, scolarité, soin, intégration). |
| `vpf-jeune-majeur-l423-22` | VPF jeune majeur — 16-21 ans entré mineur, scolarisé | L. 423-22 (ancien L. 313-11 2° bis) | Analyseur validité | MANQUE | P3 | Cas spécifique transition majorité. |
| `pacs-vpf-eligibilite` | VPF PACS conclu en France — 1 an PACS + intensité communauté | L. 423-23 (jurisprudence PACS) | Analyseur validité | MANQUE | P3 | Cas distinct du mariage. |

#### 3.2.3 — Titre étranger malade (L. 425-9)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `etranger-malade-l425-9-eligibilite` | Titre étranger malade — état de santé nécessitant soins indisponibles dans le pays d'origine | L. 425-9 (ancien L. 313-11 11°) | Analyseur validité + checklist OFII | **MANQUE** | **P2** | OFII (collège médical) émet l'avis. Procédure très spécifique. **Demande forte** depuis durcissement loi 2018 (basculement OFPRA→OFII). Pas couvert. Doit être CONTEXTUAL avec flag IA `etranger_malade_detecte`. |
| `etranger-malade-recours-ofii` | Recours contre avis défavorable OFII collège médical | Jurisprudence CE ; R. 425-12 (à vérifier) | Générateur recours | MANQUE | **P2** | Recours TA contre la décision préfecture qui s'appuie sur l'avis OFII. Doctrine spécifique. |

#### 3.2.4 — Titre victime de traite, violences conjugales (L. 425-1, L. 425-6)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `victime-traite-l425-1-eligibilite` | Titre victime de traite des êtres humains | L. 425-1 (ancien L. 316-1) | Analyseur validité + checklist | **MANQUE** | **P2** | Plainte + collaboration enquête + identification OCRTEH. Très spécifique. Doit être CONTEXTUAL avec flag IA `victime_traite_detectee`. |
| `victime-violences-conjugales-l425-6` | Titre victime de violences conjugales — protection L. 425-6 (ancien L. 316-3) | L. 425-6 ; ordonnance protection JAF Cciv 515-9 | Analyseur validité + checklist | **MANQUE** | **P1** | Très urgent. Délai protection courte. Doit être CONTEXTUAL avec flag IA `victime_violences_conjugales_detectee`. |
| `proxenetisme-mariage-force-titre-l425-2` | Titre victime proxénétisme / mariage forcé — L. 425-2 (à vérifier) | L. 425-2 | Analyseur validité | MANQUE | P3 | Cas connexes traite. |

#### 3.2.5 — Titre étudiant (L. 422+)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `etudiant-eligibilite-l422-1` | Titre étudiant — inscription enseignement supérieur, ressources, attestation | L. 422-1 ; R. 422-1 | Critères validité + checklist | EXISTE (F-IM-21 + F-IM-01) | — | Critères binaires couverts F-IM-21. |
| `etudiant-changement-statut-salarie` | Étudiant → salarié en fin d'études — APS L. 422-10 + changement | L. 422-10 (APS) ; L. 422-11 ; F-IM-11 | Arbre transition | EXISTE (F-IM-11 partie) | — | Couvert par F-IM-11 changement-statut. |
| `aps-recherche-emploi-l422-10` | Autorisation provisoire de séjour recherche d'emploi étudiant — 12 mois | L. 422-10+ | Checklist + critères | **MANQUE séparément** | P3 | Aujourd'hui dans F-IM-01 mais mérite outil dédié (durée 12 mois, ressources demandées, justificatif diplôme). |
| `etudiant-conjoint-vie-privee-l422` | Famille étudiant — conjoint d'étudiant L. 422-? (à vérifier) | À vérifier | Arbre décisionnel | MANQUE | P4 | Cas peu fréquent. |

### 3.3 — Livre III CESEDA — Règles communes délivrance / renouvellement / retrait

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `delivrance-titre-anef-procedure` | Procédure dématérialisée ANEF (administration numérique étrangers en France) — pannes, délais, recours | R. 311-2-2 (à vérifier) ; arrêté 27/04/2021 | Checklist + procédure recours panne | **MANQUE** | **P2** | ANEF est obligatoire pour la majorité des titres. **Pannes ANEF récurrentes** → recours pour faute. Très demandé. |
| `recepisse-vs-attestation-prolongation` | Récépissé (autorise séjour + travail) vs Attestation de prolongation d'instruction (pas de droit travail) | R. 311-4+ ; circulaires INTV | Analyseur droits attachés | **MANQUE** | **P2** | Confusion fréquente client/employeur. Pas couvert. |
| `renouvellement-deposer-2-mois-avant` | Délai de dépôt renouvellement (2 mois avant expiration), conséquence retard | R. 433-1 ; jurisprudence | Calculateur délai + analyse retard | **MANQUE** | **P1** | Délai irréversible. Si déposé après → risque interruption droits. Pattern miroir F-DT-03 (prescription). À CONTEXTUAL avec flag IA `renouvellement_envisage`. |
| `retrait-titre-fraude-l412-7` | Retrait de titre pour fraude / mariage gris | L. 412-7 (à vérifier) ; jurisprudence | Analyseur validité + recours | **MANQUE** | P2 | Cas contentieux denses (mariage gris, fausses ressources). |
| `f-im-21-criteres-binaires-validite` | Critères binaires validité dossier (IM21_REGULARITE, IM21_DELAI_DEPOT, IM21_PIECES_MARIAGE, IM21_RESSOURCES, IM21_COMMUNAUTE_VIE, IM21_CONVENTION_ACCUEIL) | F-IM-21 PRs #714/715/716 | Référentiel + validation | EXISTE | — | Couvert. |

### 3.4 — Livre IV CESEDA — Admission exceptionnelle au séjour (AES)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-09-aes-metiers-tension` | AES travail métiers en tension — loi 2024 L. 435-3 | L. 435-3 (loi 26/01/2024) | Analyseur éligibilité | EXISTE | — | Couvert. Aujourd'hui ALWAYS_ON → **devrait être CONTEXTUAL** (Étape 3) avec flag `aes_metiers_tension_eligible_detecte`. |
| `F-IM-09-aes-famille` | AES motif familial — circulaire Valls 2012 + L. 435-1 | L. 435-1 ; circulaire Valls | Analyseur éligibilité | EXISTE | — | Couvert. Aujourd'hui ALWAYS_ON → **devrait être CONTEXTUAL** avec flag `aes_familial_eligible_detecte`. |
| `F-IM-09-aes-humanitaire` | AES motif humanitaire — L. 435-1 | L. 435-1 ; circulaire Valls | Analyseur éligibilité | EXISTE | — | Couvert. Aujourd'hui ALWAYS_ON → **devrait être CONTEXTUAL** avec flag `aes_humanitaire_eligible_detecte`. |
| `F-IM-09-aes-etudiant` | AES motif études | Circulaire Valls | Analyseur éligibilité | EXISTE | — | Couvert. **Devrait être CONTEXTUAL** avec flag `aes_etudiant_eligible_detecte`. |
| `aes-l435-2-soins` | AES motif santé / soins (avant L. 425-9) | L. 435-2 (à vérifier) ; circulaire | Analyseur éligibilité | MANQUE | P3 | Voie résiduelle si étranger malade L. 425-9 inéligible. |
| `aes-criteres-presence-prouvee` | Calcul présence prouvée (5 ans / 10 ans / 3 ans selon motif AES) | Circulaire Valls 28/11/2012 ; L. 435-1 | Calculateur présence | **MANQUE** | **P2** | Outil transverse aux 4 AES. Calcul du nombre d'années avec preuves (RIB, factures, témoignages). Très demandé. |

### 3.5 — Livre V CESEDA — Asile

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-12-asile-avance` | Dublin III, accélérée, réexamen, apatridie, protection subsidiaire | L. 521+ ; L. 531+ ; L. 561+ ; Règlement UE 604/2013 | Calculateur 5 dispositifs | EXISTE | — | Couvert. Aujourd'hui ALWAYS_ON → **devrait être CONTEXTUAL** avec flag `asile_envisage_detecte` ou `procedure_asile_detectee`. |
| `demande-asile-ofpra-introduction` | Introduction demande asile à l'OFPRA — récépissé, GUDA, ADA | L. 521-1+ ; L. 521-7 ADA | Checklist + procédure | **MANQUE** | **P2** | Procédure spécifique : passage GUDA (guichet unique demande asile), ADA (allocation demandeur d'asile), récépissé. Pas couvert. |
| `procedure-asile-acceleree-l531-24` | Procédure accélérée OFPRA — pays origine sûr, fraude, menace ordre public | L. 531-24 à L. 531-31 | Analyseur validité contestation procédure | **MANQUE séparément** | **P1** | Délai recours CNDA réduit à 15 j (au lieu de 1 mois). Très contentieux. Aujourd'hui dans F-IM-12 mais mériterait outil dédié. |
| `recours-cnda-1mois` | Recours CNDA contre décision OFPRA — délai 1 mois | L. 532-1+ ; L. 532-19 | Générateur recours + délais | EXISTE (F-IM-06 partie) | — | Couvert dans F-IM-06 type RECOURS_CNDA. |
| `cnda-aide-juridictionnelle` | AJ devant CNDA — demande, délais, conditions | L. 532-29 (à vérifier) ; loi 91-647 du 10/07/1991 | Checklist + procédure | MANQUE | P2 | Outil dédié AJ CNDA. |
| `dublin-iii-determination-etat-responsable` | Détermination État membre responsable — règlement Dublin III 604/2013 | Règlement UE 604/2013 art. 3-23 | Arbre décisionnel | EXISTE (F-IM-12 partie) | — | Couvert dans F-IM-12. |
| `dublin-iii-recours-transfert` | Recours contre arrêté de transfert Dublin — L. 572-1+ | L. 572-1+ ; CJUE | Générateur recours + délais | **MANQUE séparément** | **P1** | Délai 7 j ! Urgence absolue. Très contentieux. Aujourd'hui dans F-IM-12 mais mérite outil dédié. |
| `cessation-protection-l512-2` | Cessation / fin protection asile — clause de cessation, refus renouvellement | L. 511-3 (cessation) ; L. 512-2 ; L. 561-2 | Analyseur validité | MANQUE | P3 | Cas plus rare. |
| `apatridie-l561-1` | Demande apatridie — OFPRA, conditions Convention NY 1954 | L. 561-1+ ; Convention NY 28/09/1954 | Arbre décisionnel + checklist | EXISTE (F-IM-12 partie) | — | Couvert dans F-IM-12. |
| `protection-subsidiaire-l512-1` | Protection subsidiaire vs statut réfugié — critères différents | L. 512-1 ; jurisprudence CNDA | Comparateur statuts | EXISTE (F-IM-12 partie) | — | Couvert dans F-IM-12. |

### 3.6 — Livre VI CESEDA — Recours et contentieux des étrangers

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-08-oqtf-avec-delai-fr` | Recours OQTF avec délai 30 j L. 614-5 | L. 614-5 | Analyseur validité OQTF + recours | EXISTE | — | Couvert. Trigger `OQTF_AVEC_DELAI`. |
| `F-IM-08-oqtf-sans-delai-fr` | Recours OQTF sans délai 48 h L. 614-1 | L. 614-1+ | Analyseur validité + recours urgent | EXISTE | — | Couvert. **P1**. |
| `F-IM-08-referes-admin-fr` | Référé-suspension L. 521-1 + référé-liberté L. 521-2 | L. 521-1, L. 521-2 CJA | 2 scores parallèles | EXISTE | — | Couvert. **P1**. |
| `recours-jld-retention-24h` | Recours devant le Juge des libertés et de la détention contre placement en rétention | L. 741+ ; L. 743+ | Générateur recours + délais 24 h | **MANQUE** | **P1** | Délai 48 h pour saisir JLD après notification placement. Audience 5 j. Procédure très spécifique. **FR-only**. Pas couvert. |
| `assignation-residence-l731` | Assignation à résidence (alternative rétention) — recours | L. 731-1+ ; L. 732-1+ | Analyseur validité + recours | **MANQUE** | **P2** | Mesure intermédiaire entre rétention et libération. Recours TA. Pas couvert. |
| `f-im-20-mesures-eloignement-expulsion` | Expulsion préfectorale / ministérielle / sécurité État | L. 631-1+ ; L. 632-1+ | Analyseur validité + recours | EXISTE (F-IM-20 partie) | — | Couvert dans F-IM-20 dispositif EXPULSION. |
| `f-im-20-irtf` | IRTF (interdiction de retour) | L. 612-6+ | Analyseur validité + durée | EXISTE (F-IM-20 partie) | — | Couvert. |
| `f-im-20-iat` | IAT (interdiction administrative du territoire) | L. 222-1+ | Analyseur validité + recours | EXISTE (F-IM-20 partie) | — | Couvert. |
| `interdiction-territoire-judiciaire-itf` | ITF (interdiction du territoire français) prononcée par juge pénal | C. pén. 131-30+ ; L. 631-3 CESEDA | Analyseur validité + recours | **MANQUE** | **P2** | Distinct de l'expulsion (administratif) et de l'IRTF (administratif). ITF = peine prononcée par juge pénal. **FR-only**. Pas couvert. |
| `appel-caa-cassation-ce` | Appel CAA contre jugement TA OQTF + cassation Conseil d'État | L. 614-? CESEDA + CJA | Calculateur délais + recours | **MANQUE** | **P2** | Délai appel CAA 1 mois (sauf OQTF sans délai = 15 j). Cassation CE 2 mois. Pas couvert. |
| `obligation-quitter-territoire-categories` | 7 catégories d'OQTF (entrée irrégulière, fraude, refus titre, retrait, etc.) — détermination catégorie | L. 611-1 1° à 7° | Arbre décisionnel | **MANQUE** | **P2** | F-IM-08 ne sait pas distinguer la catégorie L. 611-1 1° vs 5° vs 7°. La catégorie change la stratégie de défense. |
| `procedure-detention-cra-conditions` | Conditions placement en CRA (centre de rétention administrative) — durée max 90 j | L. 741-1+ ; L. 742-1+ | Analyseur validité + durée maxi | MANQUE | P2 | Conditions strictes. Recours JLD. |
| `signalement-sis-radiation` | Signalement SIS (système d'info Schengen) — radiation, recours | Règlement UE 1860/2018 ; L. 312-3 | Procédure radiation | MANQUE | P3 | Cas connexe IRTF. |

### 3.7 — Régimes spéciaux

#### 3.7.1 — Régime algérien (accord 27/12/1968)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-17-regime-algerien` | 5 voies CRA (certificat de résidence algérien) — accord franco-algérien | Accord 27/12/1968 modifié 2001 + 2004 | Calculateur 5 voies | EXISTE | — | Couvert. **P3 FR-only**. Aujourd'hui ALWAYS_ON → **pourrait être CONTEXTUAL** avec flag `nationalite_algerienne` (lecture nationalité dans `ImmigrationExtractedData`). |

#### 3.7.2 — Régimes Tunisie / Maroc / Sénégal et autres accords bilatéraux

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `regime-tunisien-accord-1988` | Régime tunisien — accord franco-tunisien 17/03/1988 modifié | Accord franco-tunisien 17/03/1988 + avenants | Arbre décisionnel | **MANQUE** | **P3 FR-only** | Régime spécifique commerçant, étudiant, salarié. Pas aussi distinct que algérien (CRA), mais quelques particularités. |
| `regime-marocain-convention-1983` | Régime marocain — convention franco-marocaine 09/10/1983 | Convention 09/10/1983 | Arbre décisionnel | **MANQUE** | P4 FR-only | Plus marginal. |
| `regime-senegalais-accord-2006` | Régime sénégalais — accord franco-sénégalais 23/09/2006 (gestion concertée) | Accord 23/09/2006 | Arbre décisionnel | MANQUE | P4 FR-only | Très spécifique. |

#### 3.7.3 — Mineurs et jeunes majeurs

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-19-mineurs` | MNA / L. 435-3 enfant né France / DCEM / TIR | Cciv 375 ; L. 435-3 ; R. 321-3 ; R. 321-7 | Calculateur 4 dispositifs | EXISTE | — | Couvert. **Devrait être CONTEXTUAL** avec flag `mineur_non_accompagne_detecte` ou `client_mineur_detecte`. |
| `mna-evaluation-age-isemi` | Évaluation âge MNA — entretien social ASE / examens osseux | Cciv 388 ; arrêté 17/11/2016 (à vérifier) | Analyseur procédure + recours | **MANQUE** | **P2 FR-only** | Très contentieux. Refus d'évaluer âge → recours JE. |
| `tutelle-mna-age-juge-enfants` | Tutelle MNA — saisine juge des enfants ASE 18 mois | Cciv 375 ; circulaire Taubira 31/05/2013 | Procédure tutelle | **MANQUE** | **P3 FR-only** | Procédure dédiée mineurs isolés. |
| `aje-aide-juridictionnelle-mineur` | Aide juridictionnelle pour mineur étranger | Loi 91-647 art. 4 | Checklist | MANQUE | P3 | Procédure simplifiée AJ pour mineur. |

#### 3.7.4 — Outre-mer et territoires spécifiques

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `regime-mayotte-titre` | Régime particulier Mayotte — titre Mayotte non valable hexagone | Ordonnance 2014-464 ; L. 832-1+ CESEDA | Arbre décisionnel + obligations | **MANQUE** | **P3 FR-only** | Régime dérogatoire majeur. Très spécifique. **Pas couvert**. |
| `regime-saint-martin-titre` | Régime Saint-Martin / Saint-Barthélemy | L. 832-1+ ; L. 833-1+ (à vérifier) | Arbre décisionnel | MANQUE | P4 FR-only | Cas marginal mais existant. |
| `regime-guyane-banga-bouedo` | Régime Guyane — frontière Brésil/Suriname, contrôles spécifiques | L. 832-1+ ; L. 836-1+ (à vérifier) | Arbre décisionnel | MANQUE | P4 FR-only | Cas marginal. |
| `regime-mayotte-aide-medicale` | AME Mayotte (AME ≠ hexagone) | Code action sociale ; L. 832-1+ | Analyseur droits sociaux | MANQUE | P4 | Connexe. |

#### 3.7.5 — UE / EEE / Suisse

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `ue-eee-suisse-droit-sejour` | Citoyens UE/EEE/Suisse — droit séjour automatique 3 mois + 5 ans | Directive 2004/38 CE ; L. 233+ ; L. 234+ | Arbre décisionnel + checklist | **MANQUE** | **P2** | Régime totalement distinct. Pas de titre obligatoire ; mais « attestation » pour démarches. **Pas couvert**. |
| `ue-membre-famille-non-ue` | Membre famille UE non européen — carte « membre famille citoyen UE » | Directive 2004/38 art. 5+10 ; L. 234-1+ | Arbre décisionnel | MANQUE | P3 | Cas spécifique conjoint marocain d'un Français vs marocain d'un Italien (Directive UE plus favorable). |
| `brexit-britanniques-titre-special` | Britanniques pré-Brexit — titre « accord retrait UE » | Accord retrait UE 31/01/2020 ; L. 234-9 (à vérifier) | Checklist | MANQUE | P4 | Stock client diminuant. |

#### 3.7.6 — Carte de résident (L. 426+)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `carte-resident-l426-1-conditions` | Carte de résident 10 ans — 5 ans titre VPF/salarié + intégration + ressources | L. 426-1+ | Critères validité | MANQUE | **P2** | Forte demande. F-IM-21 partiellement couvert mais mérite outil dédié (5 ans + intégration). |
| `carte-resident-permanente-renouvellement` | Carte résident permanente (post-2015) — durée illimitée | L. 426-1 ; L. 426-3 | Conditions + retrait | MANQUE | P3 | Cas limité. |
| `carte-resident-conjoint-francais-3-ans` | Carte de résident conjoint Français — 3 ans VPF | L. 426-? (à vérifier) | Critères validité | MANQUE | **P2** | Délai 3 ans VPF mariage. |
| `carte-resident-parent-enfant-francais-l426-5` | Carte résident parent enfant français | L. 426-5 (à vérifier) | Critères validité | MANQUE | P3 | Cas spécifique. |

### 3.8 — Nationalité (Cciv art. 17 à 33-2)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `F-IM-13-naturalisation` | 6 voies Cciv (DECRET 21-15, MARIAGE L. 21-2, ASCENDANT 21-13-1, MINEUR 22-1, REINTEGRATION 24-1+, OPPOSITION) | Cciv 21-1+ | Calculateur voies | EXISTE | — | Couvert. **Pourrait être CONTEXTUAL** avec flag `naturalisation_envisagee_detectee`. |
| `declaration-nationalite-mariage-21-2` | Déclaration nationalité par mariage — 4 ans union, communauté de vie | Cciv 21-2 ; décret 93-1362 | Checklist + procédure | EXISTE (F-IM-13 partie) | — | Couvert dans F-IM-13. |
| `declaration-nationalite-anterior-france` | Déclaration nationalité antérieure France (ascendant français) | Cciv 21-13 | Checklist | EXISTE (F-IM-13 partie) | — | Couvert. |
| `recours-refus-naturalisation-tribunal-judiciaire` | Recours refus déclaration nationalité — Tribunal Judiciaire | Cciv 26-3 ; CPC | Générateur recours | **MANQUE** | **P2** | Distinct du recours administratif (refus décret). Procédure judiciaire. Pas couvert. |
| `recours-refus-naturalisation-decret-tribunal-administratif` | Recours refus naturalisation par décret — TA Nantes | L. 213-2 (à vérifier) ; CJA | Générateur recours | **MANQUE** | **P2** | Distinct du recours déclaration (TJ). Pas couvert. |
| `decheance-nationalite-cciv-25` | Déchéance nationalité — Cciv 25 (cas limités) | Cciv 25 + 25-1 | Analyseur validité | MANQUE | P3 | Cas rare mais sensible (terrorisme). |
| `apatridie-cciv-21-23-bis` | Apatride né en France | Cciv 21-23 ; L. 561-1 CESEDA | Critères | MANQUE | P3 | Connexe F-IM-12. |

### 3.9 — Asile et protections subsidiaires (Livre V approfondi)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `asile-conventionnel-criteres-genève-1951` | Statut réfugié Convention Genève 1951 — 5 motifs persécution (race, religion, nationalité, opinion politique, groupe social) | Convention 28/07/1951 art. 1A ; L. 511-1 | Arbre décisionnel + scoring | EXISTE (F-IM-12 partie) | — | Couvert. |
| `cnda-procedures-orales-ecrites` | Procédures CNDA — audience publique vs huis clos vs ordonnance | L. 532-1+ ; règlement procédure CNDA | Procédure | MANQUE | P3 | Cas confort avocat. |
| `ada-allocation-demandeur-asile-l553` | Allocation demandeur d'asile — montant, conditions | L. 553-1+ ; OFII | Calculateur | MANQUE | P3 | Cas social. |
| `protection-temporaire-ukrainiens` | Protection temporaire UE 2001/55/CE — Ukrainiens | Directive 2001/55 ; décision UE 2022/382 | Checklist + droits | MANQUE | P3 (conjoncturel) | Stock diminuant. |
| `cessation-protection-clauses` | Clause cessation Convention Genève art. 1C | Convention Genève art. 1C ; L. 511-3 | Analyseur validité | MANQUE | P4 | Cas rare. |

### 3.10 — Séjour irrégulier et régularisation

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `regularisation-irreguliere-strategie` | Régularisation séjour irrégulier — quelle voie : AES, mariage, regroupement, asile | L. 435-1+ | Comparateur stratégique | **MANQUE** | **P1** | Outil de stratégie maître. Aujourd'hui éclaté en 4 outils F-IM-09 sans vue d'ensemble. Pattern miroir F-IM-05 mais pour les irréguliers. **Très demandé**. |
| `delit-sejour-irregulier-l823-1` | Délit séjour irrégulier — sanctions (depuis abrogation art. L. 621-1 ancien) | L. 823-1+ (à vérifier) ; jurisprudence Cass. crim. | Information + sanctions | MANQUE | P3 | Cas pénal. |
| `aide-au-sejour-irregulier-l622-1` | Aide au séjour irrégulier — exemptions humanitaires | L. 823-1+ ; loi 2012 délit solidarité | Analyseur exemptions | MANQUE | P4 | Cas rare. |

### 3.11 — Travailleurs et permis spécifiques (zone hors L. 421+)

| tool_id proposé | Situation juridique | Source juridique | Type d'outil | Statut | Priorité | Notes |
|---|---|---|---|---|---|---|
| `permis-travail-employeur-recours-refus` | Recours employeur refus autorisation de travail (DGEFP / DDETS) | R. 5221-20 Code travail ; loi 2024 | Générateur recours | MANQUE | **P2** | Distinct de F-IM-07 (côté étranger). Côté employeur. **Demandé**. |
| `controle-employeur-emploi-irregulier-l8253` | Contrôle URSSAF / inspection travail — emploi étranger sans titre | L. 8253-1+ Code travail ; L. 8211-1 | Analyseur sanctions | MANQUE | P3 | Sanctions pénales + financières. |
| `taxe-ofii-employeur` | Taxe OFII employeur — recouvrement | R. 5221-20 ; arrêté annuel | Calculateur taxe | MANQUE | P4 | Cas RH. |

---

### 3.12 — Vue synthétique Tableau B — chiffres

- Total situations juridiques inventoriées : **~110** (toutes priorités confondues).
- **Existent et couverts** : 17 outils en place + 6-8 sous-outils déjà absorbés dans F-IM-12 / F-IM-13 / F-IM-20.
- **Manquants P1** (urgence procédurale) : **9** (recours CRRV refus visa, validation VLS-TS OFII, renouvellement 2 mois avant, victime violences L. 425-6, dublin transfert recours 7 j, JLD rétention 24/48 h, procédure asile accélérée 15 j, régularisation stratégique, recours JLD).
- **Manquants P2** (fréquence haute) : **~22** (regroupement familial, étranger malade L. 425-9, victime traite L. 425-1, VPF liens personnels L. 423-23, ANEF procédure pannes, récépissé/attestation prolongation, CRRV/TA Nantes stratégie, présence prouvée AES, recours OFII collège médical, OFPRA introduction, AJ CNDA, IRTF/expulsion catégorie OQTF, ITF judiciaire, appel CAA + cassation CE, recours refus naturalisation TJ + TA, MNA évaluation âge, carte résident L. 426-1, UE/EEE/Suisse, etc.).
- **Manquants P3** (spécificité FR) : **~25** (Mayotte, Saint-Martin, Guyane, accords franco-tunisien/marocain/sénégalais, pacs VPF, jeune majeur, AES soins, déchéance nationalité, etc.).
- **Manquants P4** (confort) : ~10.

---

## 4. Tableau C — Audit F-166 Immigration FR (ALWAYS_ON → CONTEXTUAL)

### 4.1 — Constat

Sur les 17 outils Immigration FR câblés (Tableau A) :

- **3 CONTEXTUAL** déjà : F-IM-08-oqtf-avec-delai-fr / F-IM-08-oqtf-sans-delai-fr / F-IM-08-referes-admin-fr (déclencheurs sur `type_procedure_detectee`).
- **14 ALWAYS_ON** (4 transversaux NULL + 10 FR explicit). Tous s'affichent par défaut.

Parmi ces 14 ALWAYS_ON, lesquels devraient être CONTEXTUAL ? Critère : **est-ce qu'un avocat en immigration générale rencontre cette situation dans plus ou moins de 30 % de ses dossiers ?** Si moins de 30 %, ALWAYS_ON est un bruit visuel ; si plus, ALWAYS_ON est légitime.

| Outil | % dossiers concernés (estimation) | Verdict | Flag IA proposé | Source d'extraction (preuves textuelles attendues dans le dossier) |
|---|---|---|---|---|
| `F-IM-05-arbre-decisionnel-titre` | ~80 % (toute première consultation client + tout renouvellement) | **GARDER ALWAYS_ON** | — | Outil de cadrage initial. |
| `F-IM-01-checklist-pieces` | ~80 % (tout dossier de demande / renouvellement) | **GARDER ALWAYS_ON** | — | Outil de production indispensable. |
| `F-IM-06-recours` | ~30 % (dossier + recours en cas de refus) | **CANDIDAT CONTEXTUAL** | `recours_envisage_detecte` ou `decision_administrative_contestable_detectee` | Présence d'une décision administrative négative dans les pièces : refus titre, refus visa, refus regroupement, refus naturalisation, refus AES. **Preuve dans le doc** : « rejette votre demande », « refuse le séjour », « refuse l'admission », « décline », « ne saurait faire droit ». À débattre — l'outil sert aussi à anticiper. Compromis possible : garder ALWAYS_ON. |
| `F-IM-07-droit-au-travail` | ~50 % (tout titre permettant de travailler) | **GARDER ALWAYS_ON** | — | Information transversale demandée par tout employeur. |
| `F-IM-09-aes-metiers-tension` | ~5 % (loi 2024, niche métiers tension) | **OUI CONTEXTUAL** | `aes_metiers_tension_eligible_detecte` | Métier client dans liste métiers tension de la région + ressources contrat travail. **Preuve** : nom du métier (ex. « cuisinier », « aide-soignant », « ouvrier BTP ») + contrat ≥ 12 mois + ancienneté présence ≥ 3 ans. |
| `F-IM-09-aes-famille` | ~10 % | **OUI CONTEXTUAL** | `aes_familial_eligible_detecte` | Présence ≥ 5 ans + enfant scolarisé + conjoint régulier. **Preuve** : certificats scolarité enfants, copie titre conjoint, justificatifs présence. |
| `F-IM-09-aes-humanitaire` | ~5 % | **OUI CONTEXTUAL** | `aes_humanitaire_eligible_detecte` | Présence ≥ 10 ans + intégration prouvée + considérations exceptionnelles. **Preuve** : certificats domicile 10 ans, attestations intégration, situation médicale/familiale. |
| `F-IM-09-aes-etudiant` | ~5 % | **OUI CONTEXTUAL** | `aes_etudiant_eligible_detecte` | Ancien étudiant en situation irrégulière + parcours universitaire FR. **Preuve** : attestations universités FR, refus renouvellement étudiant. |
| `F-IM-11-changement-statut` | ~15 % | **OUI CONTEXTUAL** | `changement_statut_envisage_detecte` | Titre actuel + projet de changement (ex. étudiant → salarié). **Preuve** : « contrat de travail », « offre d'emploi », « passage en VPF », « mariage avec Français pendant études ». |
| `F-IM-12-asile-avance` | ~10 % (asile = niche dans cabinet généraliste) | **OUI CONTEXTUAL** | `procedure_asile_detectee` ou `asile_envisage_detecte` | Récépissé OFPRA, ADA, GUDA mentionnés ; OFPRA/CNDA en correspondance ; nationalité d'un pays à risque. **Preuve** : « OFPRA », « CNDA », « demandeur d'asile », « réfugié », « protection subsidiaire », « Dublin », « pays sûr ». |
| `F-IM-13-naturalisation` | ~10 % (jamais en début de carrière du client) | **OUI CONTEXTUAL** | `naturalisation_envisagee_detectee` | 5 ans titre + intention naturalisation. **Preuve** : « naturalisation », « décret 21-15 », « déclaration mariage », « stage de naturalisation », « entretien préfecture nationalité », ou simplement présence ≥ 5 ans titre + Français dans la famille. |
| `F-IM-17-regime-algerien` | ~3 % (niche nationalité) | **OUI CONTEXTUAL** | `nationalite_algerienne` (déjà extrait dans `ImmigrationExtractedData.nationalite`) | Pas besoin de nouveau flag — utiliser `nationalite = "Algérienne"`. **Preuve** : passeport algérien, certificat de résidence algérien CRA, accord 1968. |
| `F-IM-19-mineurs` | ~5 % (cas spécifique mineurs) | **OUI CONTEXTUAL** | `client_mineur_detecte` ou `mineur_non_accompagne_detecte` | Date naissance < 18 ans OU MNA mentionné. **Preuve** : âge du client < 18 ans (déjà dans `ImmigrationExtractedData.dateNaissance`), « mineur isolé », « MNA », « ASE », « ordonnance JE », « tutelle ». |
| `F-IM-20-mesures-eloignement` | ~5 % (expulsion/IRTF/IAT spécifiques OQTF) | **OUI CONTEXTUAL** | `mesure_eloignement_detectee` | Expulsion / IRTF / IAT mentionné. **Preuve** : « arrêté d'expulsion », « IRTF », « interdiction administrative du territoire », « IAT », « ITF », « urgence sécurité État ». |

### 4.2 — Synthèse Tableau C

**Outils ALWAYS_ON Immigration FR à conserver tels quels** (parce que rencontrés > 30 % des dossiers) :

- F-IM-01-checklist-pieces ✓
- F-IM-05-arbre-decisionnel-titre ✓
- F-IM-07-droit-au-travail ✓
- F-IM-06-recours ⚠ (à débattre — plutôt 30 % donc à la limite ; recommandation : **garder ALWAYS_ON** par défaut, sécurité juridique).

**Outils ALWAYS_ON à basculer en CONTEXTUAL** (10 outils) :

| Outil | Flag IA à introduire | Type | Priorité F-166-IM |
|---|---|---|---|
| F-IM-09-aes-metiers-tension | `aes_metiers_tension_eligible_detecte` | boolean | P1 |
| F-IM-09-aes-famille | `aes_familial_eligible_detecte` | boolean | P1 |
| F-IM-09-aes-humanitaire | `aes_humanitaire_eligible_detecte` | boolean | P1 |
| F-IM-09-aes-etudiant | `aes_etudiant_eligible_detecte` | boolean | P1 |
| F-IM-11-changement-statut | `changement_statut_envisage_detecte` | boolean | P2 |
| F-IM-12-asile-avance | `procedure_asile_detectee` | boolean | P1 |
| F-IM-13-naturalisation | `naturalisation_envisagee_detectee` | boolean | P2 |
| F-IM-17-regime-algerien | (déjà extrait via `nationalite`) — passer trigger_field='nationalite' / trigger_value='Algérienne' | string | P2 |
| F-IM-19-mineurs | `client_mineur_detecte` (ou `dateNaissance < today - 18 ans`) | boolean | P1 |
| F-IM-20-mesures-eloignement | `mesure_eloignement_detectee` | boolean | P1 |

**Effet attendu** :

- **Avant** : avocat ouvre dossier renouvellement APS → voit 4 transversaux + 10 FR-explicit + 0-3 OQTF = **14 cards par défaut**.
- **Après** : avocat ouvre dossier renouvellement APS sans flag détecté → voit 4 transversaux ALWAYS_ON + 0 contextuels = **4 cards par défaut**. Si l'IA détecte AES famille (présence 5 ans + enfant scolarisé) → 1 card AES famille apparaît.
- **Réduction du bruit** : -71 % (de 14 cards à 4 par défaut), au même niveau que F-166 Travail FR.

### 4.3 — Flags supplémentaires utiles non liés à un outil ALWAYS_ON existant

Ces flags pourraient être ajoutés en parallèle pour piloter l'apparition d'**outils MANQUE** créés en suite :

| Flag IA | Sert à afficher (à terme) | Niveau d'urgence flag |
|---|---|---|
| `etranger_malade_detecte` | Outil L. 425-9 santé (P2 manquant) | Haute |
| `victime_traite_detectee` | Outil L. 425-1 (P2 manquant) | Haute |
| `victime_violences_conjugales_detectee` | Outil L. 425-6 (P1 manquant) | Critique |
| `regroupement_familial_envisage` | Outil regroupement R. 421+ (P2 manquant) | Haute |
| `procedure_acceleree_asile_detectee` | Outil procédure accélérée 15 j (P1 manquant) | Critique |
| `dublin_transfert_detecte` | Outil recours Dublin 7 j (P1 manquant) | Critique |
| `retention_administrative_detectee` | Outil JLD rétention 24/48 h (P1 manquant) | Critique |
| `assignation_residence_detectee` | Outil assignation résidence (P2 manquant) | Haute |
| `cas_humanitaire_detecte` | Outil pied décision humanitaire (P3) | Moyenne |
| `vie_privee_familiale_detectee` | Sous-outil L. 423-23 (P2 manquant) | Haute |
| `nationalite_algerienne` | F-IM-17 régime algérien (déjà disponible) | — |
| `nationalite_ue_eee_suisse` | Outil UE/EEE/Suisse (P2 manquant) | Haute |
| `mineur_non_accompagne_detecte` | F-IM-19 + sous-outil MNA évaluation âge | Haute |
| `refus_visa_detecte` | Outil CRRV (P1 manquant) | Critique |
| `irreguliere_5_ans_detectee` / `irreguliere_10_ans_detectee` | AES famille / humanitaire | Haute |
| `metier_en_tension_detecte` | F-IM-09-aes-metiers-tension | Haute |
| `oqtf_categorie_l611_1_X` | Outil catégorie OQTF (P2 manquant) | Haute |

### 4.4 — Arbitrage sur F-IM-06-recours

F-IM-06-recours est aujourd'hui ALWAYS_ON. Cette analyse pencherait pour CONTEXTUAL avec `decision_administrative_contestable_detectee`. Mais 3 contre-arguments :

1. La **culture client** : un client va voir son avocat précisément parce qu'il vient de recevoir un refus → 80-90 % des dossiers contiennent une décision contestable.
2. La **sécurité juridique** : si l'IA ne détecte pas la décision (mauvaise OCR, doc scanné), l'outil disparaît silencieusement → l'avocat peut rater le délai. Pattern à éviter sur outils de recours.
3. **F-IM-06 est l'outil de production maître** (générateur de recours) — le rendre invisible par défaut accroît le risque produit.

**Recommandation** : conserver ALWAYS_ON. Documenter dans la mini-spec F-166-IM pourquoi ce choix d'asymétrie avec F-IM-09 (qui sera bien CONTEXTUAL).

---

## 5. Synthèse — Top 10 manquants prioritaires

### 5.1 — Top 10 outils Immigration FR à créer en priorité (P1 + P2)

| Rang | Outil proposé | Priorité | Source | Pourquoi maintenant |
|---|---|---|---|---|
| 1 | `recours-jld-retention-24h` | **P1** | L. 741+, L. 743+ CESEDA | Délai 48 h irréversible — tout cabinet ayant un client en CRA. **FR-only**. |
| 2 | `dublin-iii-recours-transfert-7j` | **P1** | L. 572+ ; Règlement UE 604/2013 | Délai 7 j, urgence absolue. À éclater de F-IM-12. |
| 3 | `recours-refus-visa-crrv` | **P1** | L. 312-1+ ; D. 312-3 | Délai 2 mois préalable obligatoire avant TA Nantes. **FR-only**. |
| 4 | `victime-violences-conjugales-l425-6` | **P1** | L. 425-6 | Très urgent. Lié à ordonnance protection JAF Cciv 515-9. |
| 5 | `vls-ts-validation-ofii-FR` | **P1** | R. 311-3+ | Délai 3 mois post-arrivée. Très demandé conjoints, étudiants. |
| 6 | `regularisation-irreguliere-strategie` | **P1** | L. 435-1+ | Outil de stratégie maître irréguliers. Forte valeur produit. |
| 7 | `etranger-malade-l425-9-eligibilite` | **P2** | L. 425-9 | Procédure OFII spécifique. Très demandé. |
| 8 | `regroupement-familial-r421` | **P2** | L. 434-1+ ; R. 434-1+ | Cas fréquent. Pas couvert d'outil dédié éligibilité. |
| 9 | `vpf-liens-personnels-familiaux-l423-23` | **P2** | L. 423-23 | Forte demande. Critères jurisprudentiels denses. |
| 10 | `oqtf-categorie-l611-1` | **P2** | L. 611-1 1° à 7° | Détermine la stratégie de défense. F-IM-08 ne distingue pas la catégorie. |

### 5.2 — Top priorités F-166 Immigration (réduction bruit visuel)

Si on ne pouvait livrer que 4 flags F-166 Immigration : `procedure_asile_detectee`, `mesure_eloignement_detectee`, `client_mineur_detecte`, `aes_metiers_tension_eligible_detecte`. Couvre 4 ALWAYS_ON très bruyants et représente 80 % du gain ergonomique.

### 5.3 — Découpages à éclater (outils existants trop gros)

- **F-IM-12-asile-avance** (5 dispositifs). Mériterait d'être éclaté en **3 outils** : (1) F-IM-12a Dublin III + recours transfert, (2) F-IM-12b Procédure accélérée + réexamen + cessation, (3) F-IM-12c Apatridie + protection subsidiaire vs réfugié. Justifié par P1 sur Dublin et P1 sur procédure accélérée.
- **F-IM-20-mesures-eloignement** (3 dispositifs). Pourrait rester groupé (3 dispositifs = 3 sous-écrans) mais ITF judiciaire (manquant) est à ajouter.
- **F-IM-09-aes** (4 voies). Déjà éclaté en 4 outils — bonne décomposition. **Confirmer** que la 5e voie « AES soins L. 435-2 » est bien marginale (vs L. 425-9 santé).
- **F-IM-13-naturalisation** (6 voies dans 1 outil). Pourrait éclater **recours refus** en 2 outils dédiés : (1) recours TJ pour refus déclaration, (2) recours TA Nantes pour refus décret. Manquant Top 10.

### 5.4 — Hors périmètre / honnêteté méthodologique

Cet audit présente plusieurs limites assumées :

1. **Articles CESEDA non vérifiés exhaustivement**. Plusieurs articles cités « (à vérifier) » nécessitent confirmation par un avocat en droit des étrangers. La recodification 2021 a renuméroté massivement (L. 313-11 7° → L. 423-23 par exemple) ; risque d'incohérence sur quelques renvois.

2. **Loi Darmanin 2024 partiellement intégrée**. La loi du 26/01/2024 modifie ~50 articles CESEDA. L. 435-3 (AES métiers tension) et durcissement OQTF 30 j sont intégrés. Les autres modifications (suppression AME pour irréguliers cas, durcissement RCC, etc.) sont à scanner.

3. **Pas de comparaison BE**. Consigne explicite. Les outils FR-only signalés comme tels (préfecture, ANEF, OFII collège médical, JLD, CRA, AES, ADA, MNA ASE) ne le sont qu'à titre d'information.

4. **Pas d'analyse jurisprudentielle quantitative**. Certains outils (VPF L. 423-23, étranger malade) reposent sur jurisprudence dense (CE, CAA). Une SF future pourrait livrer un référentiel jurisprudentiel CE/CNDA.

5. **Pas de seed `legal_referentials`**. Cet audit produit des `tool_id` candidats. La création effective demande pour chaque outil : (a) une mini-spec backend, (b) une migration `legal_referentials` avec description SF-140-03, (c) une entrée `decision_tool_visibility_rules`, (d) un composant frontend, (e) une entrée `TOOL_REGISTRY`, (f) une mise à jour `KNOWN_FRONTEND_TOOL_IDS` du test d'intégrité, (g) prefill IA + F-IA-03.

6. **Le périmètre F-IM-21** (déjà livré) couvre les critères binaires de validité dossier — il faut s'assurer que les nouveaux outils manquants (étranger malade L. 425-9, victime traite L. 425-1, regroupement familial R. 421+) ajoutent leurs propres `IM21_*` codes au référentiel.

7. **Audit F-166 Immigration BE non couvert ici** (consigne : Immigration FR uniquement). Un audit F-166 BE séparé est à prévoir pour les 4 outils BE F-IM-14 (9bis, 9ter, 40bis, 40ter) et F-IM-08-annexe13-be, qui sont actuellement tous ALWAYS_ON BE.

---

## 6. Plan d'action recommandé (synthèse opérationnelle)

### 6.1 — Vague immédiate (P1 — délais procéduraux critiques)

1. **F-IM-22 Recours JLD rétention 24/48 h** (FR-only). 1 SF backend + 1 SF frontend. Flag IA : `retention_administrative_detectee`.
2. **F-IM-23 Dublin transfert recours 7 j** (extraire de F-IM-12). 1 SF. Flag IA : `dublin_transfert_detecte`.
3. **F-IM-24 Recours CRRV refus visa** (FR-only). 1 SF backend + 1 SF frontend. Flag IA : `refus_visa_detecte`.
4. **F-IM-25 Validation VLS-TS OFII 3 mois**. 1 SF.
5. **F-IM-26 Victime violences conjugales L. 425-6**. 1 SF backend + 1 SF frontend. Flag IA : `victime_violences_conjugales_detectee`.

### 6.2 — Vague F-166 Immigration FR (réduction bruit visuel)

6. **F-IM-27 SF-IM-27-01 backend prompts + flags IA Immigration**. Ajouter 10 booleans dans `ImmigrationExtractedData` : `aes_metiers_tension_eligible_detecte`, `aes_familial_eligible_detecte`, `aes_humanitaire_eligible_detecte`, `aes_etudiant_eligible_detecte`, `changement_statut_envisage_detecte`, `procedure_asile_detectee`, `naturalisation_envisagee_detectee`, `client_mineur_detecte`, `mesure_eloignement_detectee`, plus utilisation de `nationalite_algerienne`. Pattern miroir SF-166-01.
7. **F-IM-27 SF-IM-27-02 migration ALWAYS_ON → CONTEXTUAL**. 10 INSERT/UPDATE dans `decision_tool_visibility_rules` pour les 10 outils ALWAYS_ON FR-explicit. Pattern miroir SF-166-02 (migration 199).

### 6.3 — Vague stratégie produit (P1-P2 valeur métier)

8. **F-IM-28 Régularisation séjour irrégulier — comparateur stratégique** (4 voies + manquantes). 1 SF.
9. **F-IM-29 Étranger malade L. 425-9 + recours OFII**. 2 SF.
10. **F-IM-30 Regroupement familial R. 421+ éligibilité**. 2 SF.
11. **F-IM-31 VPF liens personnels L. 423-23**. 2 SF.
12. **F-IM-32 OQTF catégorie L. 611-1 1°-7°**. 1 SF.

### 6.4 — Vagues ultérieures

- **Outre-mer** (Mayotte, Saint-Martin, Guyane). 3-5 SF.
- **Régimes bilatéraux** (Tunisie, Maroc, Sénégal). 3 SF.
- **MNA évaluation âge + tutelle JE**. 2 SF.
- **Naturalisation recours TJ + TA**. 2 SF.
- **UE/EEE/Suisse droit séjour**. 2 SF.
- **Détails procédure ANEF / récépissé / attestation**. 2 SF.

---

## 7. Conclusion

L'audit identifie 17 outils Immigration FR câblés au 2026-05-06 contre **~110 situations juridiques distinctes** méritant un outil. La couverture actuelle représente **~15 % du périmètre théorique exhaustif**, avec une concentration sur les piliers (titre, recours, OQTF, AES, asile, naturalisation, mineurs, éloignement).

**Deux chantiers parallèles** se dégagent :

1. **F-IM-22 à F-IM-26 (P1 délais procéduraux)** — combler les manquants critiques où un délai court irréversible expose à la perte du droit. Priorité absolue. ~6 SF.
2. **F-IM-27 (F-166-IM)** — basculer 10 outils ALWAYS_ON en CONTEXTUAL via 10 nouveaux flags IA. Ergonomie identique à ce que F-166 a fait pour Travail. **Gain : -71 % de bruit visuel par défaut**. ~2 SF.

Ces deux chantiers sont **indépendants et parallélisables**. F-IM-27 (réduction bruit) peut démarrer immédiatement car il ne crée aucun nouvel outil — il consomme uniquement des flags IA en plus du seed existant. F-IM-22-26 sont des features pleines (backend + frontend) sur le pattern canonique F-155 + F-IA-03.

**Risque méthodologique principal** : la recodification CESEDA 2021 + loi Darmanin 2024 imposent de re-scanner systématiquement les références « ancien L. 313-11 X° » avant de seeder. Un avocat en droit des étrangers doit valider les `legal_referentials.description` SF-140-03 de chaque nouvel outil créé.
