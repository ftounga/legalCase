# Mini-spec — F-138 / SF-F-138-01 Extension référentiel CCN FR + CP BE

## Identifiant

`F-138 / SF-F-138-01`

## Feature parente

`F-138` — Couverture CCN FR étendue (> 49) et CP BE étendues (> 17)
(entrée backlog V8 dans `docs/PRODUCT_SPEC.md`, à ne pas confondre avec l'ancienne F-138 V6 « Audit structurel des types de barèmes » — Terminée 2026-04-20.)

## Statut

`ready`

## Date de création

2026-04-26

## Branche Git

`feat/SF-F-138-01-extension-ccn-cp`

---

## Objectif

Étendre le référentiel `legal_referentials` (`CONVENTION_BAREMES`) en ajoutant **50 CCN FR** au-delà des 49 déjà seedées par F-129/F-136 et **10 nouvelles CP BE** au-delà des 17 existantes, et compléter `CONVENTION_PREAVIS` (utilisé par F-DT-25) avec **8 CCN FR supplémentaires** afin de couvrir ~95-98 % du volume prud'homal réellement rencontré sur le terrain — sans nouvelle logique métier (pure data).

---

## Comportement attendu

### Cas nominal

Après merge :

- La table `legal_referentials` contient :
  - **+50 entries** `referential_type = 'CONVENTION_BAREMES'` / `country = 'FRANCE'` / `is_system = true` (clés `IDCC_xxxx`).
  - **+10 entries** `referential_type = 'CONVENTION_BAREMES'` / `country = 'BELGIQUE'` / `is_system = true` (clés `CPxxx`).
  - **+8 entries** `referential_type = 'CONVENTION_PREAVIS'` / `country = 'FR'` / `is_system = true` (CCN les plus consultées avec matrice de préavis renseignée).
- Total CCN FR couvertes pour `CONVENTION_BAREMES` : 49 + 50 = **99**.
- Total CP BE couvertes pour `CONVENTION_BAREMES` : 17 + 10 = **27**.
- Total CCN FR couvertes pour `CONVENTION_PREAVIS` : 3 + 8 = **11**.
- `LegalReferentialService.getConventionBareme("IDCC_3248")` (et toute autre clé) reste fonctionnel — aucune entrée existante n'est modifiée ni écrasée.
- `LegalReferentialService.getConventionPreavis("IDCC_1597", IndemnitePreavisFonction.OUVRIER, 36)` retourne désormais une `ConventionPreavis` non-nulle (cas Bâtiment, ouvrier, 3 ans d'ancienneté).
- `LegalReferentialService.getConventionPreavis("IDCC_1979", IndemnitePreavisFonction.EMPLOYE, 12)` retourne désormais la durée HCR.
- Les outils décisionnels qui consomment ces référentiels (F-DT-07 Ancienneté, F-DT-25 Indemnité préavis FR) bénéficient automatiquement de la couverture étendue, sans modification de code.
- L'admin peut override par workspace via `PUT /api/v1/referential/{id}` (F-110), comportement inchangé.
- La page « Guides & barèmes » du frontend (F-110) liste automatiquement les nouvelles entries.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Migration rejouée (idempotence) | Liquibase détecte `changeSet` déjà appliqué → no-op | — |
| Collision UUID avec une entry existante | Migration échoue → rollback Liquibase → CI rouge | — |
| Description manquante sur une entry `is_system=true` | Test `LegalReferentialDescriptionIntegrityIT` échoue en CI | — |
| `getConventionBareme` appelé sur un IDCC non listé | Retourne null (comportement existant, inchangé) | — |
| `getConventionPreavis` appelé sur un IDCC non couvert | Retourne null → calculateur F-DT-25 bascule sur source `LEGALE` (comportement existant) | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Seeding référentiel FR `CONVENTION_BAREMES` (49 CCN existantes) | Étendu — pas de modification, uniquement ajout de 50 nouvelles | Intégré dans cette SF |
| Seeding référentiel BE `CONVENTION_BAREMES` (17 CP existantes) | Étendu — pas de modification, uniquement ajout de 10 nouvelles | Intégré dans cette SF |
| Seeding référentiel FR `CONVENTION_PREAVIS` (3 CCN existantes seulement) | Étendu — ajout de 8 CCN à fort effectif pour faire converger la couverture avec `CONVENTION_BAREMES` | Intégré dans cette SF |
| `CONVENTION_PREAVIS` BE | Non applicable — préavis BE = formule Claeys / CCT 109 (cf. note migration 134) | N/A |
| Normalizer `ConventionCodeNormalizer` | Patterns `IDCC_\d{4}` et `CP\d{2,4}` déjà supportés (SF-129-01) — aucune extension nécessaire | Vérifié |
| Fallback statique Java | Supprimé par SF-129-03 — DB seule source de vérité | N/A |
| Cohérence Admin UI (F-110) | Les nouvelles entries apparaissent automatiquement dans la page « Guides & barèmes » | Vérifié |
| Cohérence ReferentialCheckService (cron 6 mois) | Le cron parcourt toutes les entries — les nouvelles y participeront automatiquement | N/A |
| Outil décisionnel métier | Cette SF ne crée ni ne modifie d'outil décisionnel — uniquement données. Les outils F-DT-07 et F-DT-25 bénéficient automatiquement de l'extension | N/A |
| Tests backend | `LegalReferentialServiceTest` mock le repository (couvert pour la logique). Le `@SpringBootTest` `LegalReferentialDescriptionIntegrityIT` rejoue la migration sur H2 et garantit la présence de `description` sur chaque INSERT `is_system=true` | Couverture suffisante + tests ajoutés sur 5 IDCC clés pour `getConventionPreavis` |
| Description SF-140-03 | Chaque INSERT inclut explicitement la colonne `description` en langage avocat | Intégré |
| F-IA-04 visibility rules | Aucun nouvel outil décisionnel — règles de visibilité existantes inchangées | N/A |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s) — non applicable
- [x] Backlog V*N* : la suite de l'extension (vague 3+ : ~50 IDCC supplémentaires pour atteindre 150) reste tracée dans F-138 si besoin futur — non prioritaire
- [x] Non applicable aux autres cibles — outils décisionnels non modifiés, comportement préservé

---

## Liste des 50 CCN FR ajoutées

Sélection basée sur le top effectif salariés couverts (sources DARES 2024 / kali-data SocialGouv) en excluant les 49 déjà seedées par F-129 (migrations 086/087) et F-136-02 (migration 090).

Valeurs `congesLegauxJours` = 25 (minimum légal CT). Congés supplémentaires d'ancienneté et primes d'ancienneté renseignés uniquement quand documentés par la CCT — sinon laissés vides (pattern conservateur SF-129-01). Les cas d'enrichissement plus fin sont laissés au plan d'enrichissement progressif déjà appliqué pour les vagues précédentes (SF-129-02, SF-136-02).

| # | IDCC | Label court | Secteur / motivation |
|---|------|-------------|----------------------|
| 1 | 1000 | Cabinets d'avocats | Tertiaire juridique — usage métier direct |
| 2 | 1619 | Cabinets dentaires | Santé libérale |
| 3 | 1734 | Audiovisuel (entreprises techniques) | Média / production |
| 4 | 2150 | Personnels des sociétés anonymes et fondations d'HLM | Logement social |
| 5 | 2205 | Notariat | Tertiaire juridique |
| 6 | 2121 | Édition | Industrie culturelle |
| 7 | 0018 | Industries textiles | Industrie historique |
| 8 | 0303 | Couture parisienne | Mode haute couture |
| 9 | 1631 | Hôtellerie de plein air | HCR sous-secteur |
| 10 | 1612 | Mareyeurs-expéditeurs | Pêche / commerce |
| 11 | 0184 | Imprimeries de labeur et industries graphiques | Industrie graphique |
| 12 | 0247 | Industries de la maroquinerie, articles de voyage | Cuir |
| 13 | 0489 | Industries laitières | Agro-alimentaire |
| 14 | 0500 | Entreprises de courtage d'assurances | Assurance distribution |
| 15 | 0635 | Cabinets ou entreprises d'expertise en automobile | Automobile expert |
| 16 | 0653 | Cabinets ou entreprises de géomètres-experts | Tertiaire technique |
| 17 | 0700 | Industries textiles artificielles et synthétiques | Industrie textile |
| 18 | 0731 | Quincaillerie, fournitures industrielles | Commerce de gros sous-secteur |
| 19 | 1077 | Personnel des entreprises agréées de service à la personne (PSP) | Service à la personne |
| 20 | 1182 | Personnel des organismes de tourisme | Tourisme |
| 21 | 1247 | Couture parisienne (annexe employés) | Mode |
| 22 | 1311 | Industries de la fabrication des ciments | Industrie BTP |
| 23 | 1396 | Industries charcutières | Agro-alimentaire |
| 24 | 1408 | Industries de la fabrication de la chaux | Industrie minérale |
| 25 | 1411 | Manutention ferroviaire et travaux connexes | Transports |
| 26 | 1413 | Personnel intérimaire des ETT (intérim) | Travail temporaire — usage prud'homal majeur |
| 27 | 1486 | (déjà présent — exclu) | — |
| 28 | 1499 | Fabrication et commerce des produits à usage pharmaceutique, parapharma | Pharmacie distribution |
| 29 | 1505 | (déjà présent — exclu) | — |
| 30 | 1543 | Boyauderie | Agro-alimentaire |
| 31 | 1555 | Industries de fabrication mécanique du verre | Industrie verre |
| 32 | 1558 | Caoutchouc | Industrie chimique |
| 33 | 1564 | Cartonnage | Industrie emballage |
| 34 | 1577 | Cinq branches industries alimentaires diverses (5 BIAD) | Agro-alimentaire |
| 35 | 1631 | (cf. ligne 9, exclu doublon) | — |
| 36 | 1686 | Commerce et de la réparation de l'horlogerie-bijouterie | Commerce détail |
| 37 | 1740 | Distributeurs conseils hors domicile (DCHD) | Commerce |
| 38 | 1747 | Activités industrielles de boulangerie-pâtisserie | Agro-alimentaire |
| 39 | 1760 | Restauration ferroviaire | Transports / restauration |
| 40 | 1794 | Conserveries coopératives et SICA | Agro-alimentaire coopératives |
| 41 | 1816 | Pompes funèbres | Services |
| 42 | 1875 | Industries de la sérigraphie | Industrie graphique |
| 43 | 1909 | Organismes de tourisme social et familial | Tourisme |
| 44 | 1922 | Radiodiffusion | Média |
| 45 | 1944 | Personnel navigant des essais et réceptions (aviation) | Aviation |
| 46 | 1947 | Commerces de quincaillerie, fournitures pour l'industrie | Commerce de gros |
| 47 | 2104 | Thermalisme | Tourisme santé |
| 48 | 2128 | Mutualité | Mutuelles santé |
| 49 | 2156 | Grands magasins et magasins populaires | Commerce détail |
| 50 | 2174 | Industries et commerces de la récupération | Recyclage |
| 51 | 2247 | Entreprises de courtage de marchandises | Commerce |
| 52 | 2335 | Personnel des agences générales d'assurances | Assurance |
| 53 | 2344 | Sociétés d'expertises et d'évaluations | Tertiaire |
| 54 | 2378 | Boulangerie-pâtisserie : entreprises industrielles | Agro-alimentaire |
| 55 | 2603 | Salariés en portage salarial | Forme d'emploi récente |
| 56 | 2691 | Entreprises d'architecture | Tertiaire technique |
| 57 | 2785 | Personnel sédentaire des entreprises de navigation | Maritime |
| 58 | 2847 | Production audiovisuelle | Média |

Le tableau ci-dessus liste 58 IDCC candidats ; les 8 derniers (lignes 51-58) compensent les exclusions (lignes 27, 29, 35) et les éventuels doublons découverts au moment de la rédaction. **Cible exacte = 50 nouvelles entries après dédoublonnage avec migrations 086/087/090** (ligne 1486 et 1505 retirées d'office).

> Source `source_ref` homogène avec SF-129-01 : `IDCC NNNN — Légifrance / kali-data` ; lorsqu'un barème conventionnel additionnel est enrichi (rare dans cette vague), la référence d'article CCN est ajoutée.

## Liste des 10 CP BE ajoutées

Sélection basée sur le volume d'employés (sources SPF Emploi + UCM/FGTB) en excluant les 17 CP déjà présentes (CP 100, 111, 118, 121, 124, 200, 201, 207, 209, 220, 226, 302, 306, 311, 322, 330, 337).

| # | Code | Label court | Secteur |
|---|------|-------------|---------|
| 1 | CP218 | CP 218 — Employés (CPNAE non explicitement couverts par autre CP) | CPNAE résiduelle employés (très large) |
| 2 | CP312 | CP 312 — Grands magasins | Grande distribution |
| 3 | CP313 | CP 313 — Pharmacies | Distribution pharmaceutique |
| 4 | CP327 | CP 327 — Entreprises de travail adapté et ateliers sociaux | Inclusion professionnelle |
| 5 | CP152 | CP 152 — Enseignement libre subventionné (ouvriers) | Enseignement |
| 6 | CP140 | CP 140 — Transport et logistique | Transport routier marchandises/voyageurs |
| 7 | CP149 | CP 149 — Secteurs connexes à la métallurgie | Métallurgie sous-secteurs |
| 8 | CP304 | CP 304 — Spectacle | Industrie culturelle |
| 9 | CP318 | CP 318 — Services d'aides familiales et seniors | Aide à domicile |
| 10 | CP329 | CP 329 — Secteur socio-culturel | Associations / culture |

Valeurs `congesLegauxJours` = 20 (loi du 28/06/1971 — base belge). Congés supplémentaires renseignés quand documentés par la CCT sectorielle, sinon vides (pattern conservateur SF-129-04).

## Liste des 8 CCN FR ajoutées à `CONVENTION_PREAVIS`

Pour combler le gap entre `CONVENTION_BAREMES` (49 → 99 entries après cette SF) et `CONVENTION_PREAVIS` (3 entries seulement avant cette SF), nous ajoutons les 8 CCN les plus consultées sur les dossiers prud'homaux. La structure JSON suit exactement le format SF-DT-25-01 (matrice `fonctions × tranches d'ancienneté → mois`).

| # | IDCC | CCN | Note préavis |
|---|------|-----|--------------|
| 1 | 1597 | Bâtiment Ouvriers (> 10 sal.) | Préavis OUVRIER 1 mois après 6 mois, 2 mois après 24 mois (art. 9.1) |
| 2 | 2609 | Bâtiment ETAM | Préavis EMPLOYE 1 mois (< 2 ans), 2 mois (≥ 2 ans) ; AGENT_MAITRISE 2 mois ; CADRE 3 mois |
| 3 | 1702 | Travaux publics ouvriers | Idem barèmes BTP, mêmes tranches que IDCC 1597 |
| 4 | 1979 | HCR | EMPLOYE 8 jours (< 6 mois), 1 mois (6 mois–2 ans), 2 mois (≥ 2 ans) ; CADRE 3 mois |
| 5 | 2216 | Commerce de détail alimentaire | EMPLOYE 1/2/2 mois suivant L.1234-1, CADRE 3 mois (art. 4.5) |
| 6 | 1979 | (déjà ligne 4) | — |
| 7 | 0573 | Commerce de gros | EMPLOYE 1/2 mois ; AGENT_MAITRISE 2 mois ; CADRE 3 mois |
| 8 | 1996 | Pharmacie d'officine | EMPLOYE 1/2/2 mois ; CADRE 3 mois |
| 9 | 1672 | Sociétés d'assurances | EMPLOYE 1/2/2 mois ; CADRE 3 mois (art. 73) |
| 10 | 1518 | ÉCLAT (animation) | EMPLOYE 1/2 mois ; CADRE 3 mois |

(Compte de 8 distincts après suppression du doublon ligne 6.)

---

## Critères d'acceptation

- [ ] La migration `163-extend-ccn-fr-cp-be-referentiel.xml` est créée, idempotente Liquibase, et passe `mvn clean compile` sans erreur de validation XML.
- [ ] La table `legal_referentials` contient au moins **+50** entries `CONVENTION_BAREMES / FRANCE` non présentes avant la migration.
- [ ] La table `legal_referentials` contient au moins **+10** entries `CONVENTION_BAREMES / BELGIQUE` non présentes avant la migration.
- [ ] La table `legal_referentials` contient au moins **+8** entries `CONVENTION_PREAVIS / FR` non présentes avant la migration.
- [ ] Aucune entry préexistante (49 IDCC FR + 17 CP BE + 3 CCN PREAVIS) n'est modifiée ni écrasée — vérifié par grep des UUID utilisés.
- [ ] Chaque nouvelle entry porte une colonne `description` non vide en langage avocat.
- [ ] Le test `LegalReferentialDescriptionIntegrityIT` reste vert.
- [ ] Au moins **5 nouveaux tests unitaires** mockés vérifient `LegalReferentialService.getConventionPreavis(...)` pour les CCN ajoutées les plus consultées (Bâtiment 1597 ouvrier, HCR 1979 employé, Métallurgie 3248 ouvrier — déjà présent, Bâtiment ETAM 2609 cadre, Pharmacie 1996 employé).
- [ ] L'isolation workspace est préservée : toutes les nouvelles entries ont `workspace_id IS NULL` + `is_system = true` (référentiel système).

---

## Périmètre

### Hors scope (explicite)

- Aucun changement de logique métier (les services `LegalReferentialService.getConvention*` ne sont pas modifiés).
- Aucun changement frontend.
- Aucun changement de schéma de la table `legal_referentials`.
- L'enrichissement des nouvelles CCN avec barèmes complets (congés supp + primes) au-delà des minimums légaux est **hors scope** — il pourra être réalisé dans une vague 3 d'enrichissement (pattern SF-129-02 / SF-136-02), tracée dans F-138 ou successeur si nécessaire.
- Pas d'ajout de `CONVENTION_PREAVIS` côté Belgique (préavis BE = formule Claeys / CCT 109, hors périmètre F-DT-25).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| workspace_id | `NULL` | Référentiel système |
| is_system | `true` | Référentiel système |
| is_active | `true` | Activé par défaut |
| legal_domain | `'DROIT_DU_TRAVAIL'` | Constant pour ce type |
| referential_type | `'CONVENTION_BAREMES'` ou `'CONVENTION_PREAVIS'` | Selon entry |
| country | `'FRANCE'` ou `'BELGIQUE'` ou `'FR'` (CONVENTION_PREAVIS conserve le format historique migration 134) | Selon entry |
| value_json | JSON conforme aux schémas existants | Cf. patterns 086/087/089/134 |
| description | Texte descriptif obligatoire | SF-140-03 |
| updated_at | `NOW()` | Liquibase |
| updated_by | `NULL` | Migration système |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Unicité |
|-------|-------------|--------|---------|
| id (UUID) | Oui | UUID v4 | Unique global |
| entry_key | Oui | `IDCC_NNNN` (4 chiffres zero-padded) ou `CPNNN` | Unique par (workspace_id, type) |
| value_json | Oui | JSON conforme au schéma du type | — |
| description | Oui | Texte non vide en langage avocat | — |
| label | Oui | Texte court | — |

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Les endpoints existants exposent automatiquement les nouvelles entries :
- `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL&type=CONVENTION_BAREMES`
- `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL&type=CONVENTION_PREAVIS`
- `PUT /api/v1/referential/{id}` (override workspace, F-110)

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | INSERT (~68 entries) | 50 FR `CONVENTION_BAREMES` + 10 BE `CONVENTION_BAREMES` + 8 FR `CONVENTION_PREAVIS` |

### Migration Liquibase

- [x] Oui — `163-extend-ccn-fr-cp-be-referentiel.xml`
- [ ] Non applicable

### Composants Angular

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `LegalReferentialServicePreavisExtensionTest` (nouveau) — 5 tests mockés sur `getConventionPreavis` :
  - IDCC 1597 / OUVRIER / 36 mois → 2 mois
  - IDCC 1979 / EMPLOYE / 12 mois → 1 mois (HCR ≥ 6 mois & < 24 mois)
  - IDCC 2609 / CADRE / 0 mois → 3 mois (préavis cadre Bâtiment ETAM dès embauche)
  - IDCC 1996 / EMPLOYE / 24 mois → 2 mois (pharmacie ≥ 2 ans)
  - IDCC 9999 / EMPLOYE / 12 mois → null (CCN inconnue, fallback nominal)

### Tests d'intégration

- [ ] `LegalReferentialDescriptionIntegrityIT` (existant) reste vert après application de la migration.
- [ ] Sanity : le context Spring se charge sans erreur Liquibase (test `@SpringBootTest` sur n'importe quel IT existant suffit).

### Isolation workspace

- [x] Non applicable — référentiel système (workspace_id IS NULL). L'isolation workspace est testée par les tests existants `ReferentialControllerIT` (modifications par workspace via override).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché (ajout d'entries système)
- [ ] Plans / limites — non touché
- [ ] Navigation / routing — non touché
- [x] **Aucune préoccupation transversale** — pure extension de données

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|----------------------|-----------------|------------------------|
| `IndemnitePreavisService` | Comportement enrichi (plus de CCN couvertes) — pas de régression possible car le service tombe sur fallback `LEGALE` quand la CCN n'est pas trouvée | `IndemnitePreavisCalculatorTest` existant reste vert |
| `AncienneteCalculator` (F-DT-07) | Comportement enrichi — plus de barèmes disponibles, mêmes valeurs neutres pour les CCN minimales | Tests existants restent verts |
| Page « Guides & barèmes » F-110 | Affiche les nouvelles entries automatiquement | Visuel — non bloquant |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (pure data, pas de chemin critique d'intégration impacté).

---

## Dépendances

### Subfeatures bloquantes

- F-129 (référentiel CCN initial) — done
- F-136-02 (vague 2 enrichissement) — done
- SF-DT-25-01 (création CONVENTION_PREAVIS) — done

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Numérotation F-138** : il existe deux entrées F-138 dans `PRODUCT_SPEC.md` :
  - F-138 V6 « Audit structurel des types de barèmes » — Terminée 2026-04-20.
  - F-138 V8 « Couverture CCN FR étendue (> 49) et CP BE étendues (> 17) » — Backlog → cette SF.
  Cette SF couvre la seconde. Le mini-spec adopte le préfixe `SF-F-138-01` (au lieu de `SF-138-01`) pour distinguer visuellement la nouvelle famille — précision à apporter au PRODUCT_SPEC dans l'étape 6 post-merge si l'équipe décide de renommer la nouvelle entrée F-138bis ou F-156.
- **Pas de chgt schéma** : la colonne `description` existe déjà depuis migration 093.
- **Country `FR` vs `FRANCE`** : conservé tel que la migration 134 (préavis = `FR`) et 086/087 (barèmes = `FRANCE`). Le repository `findSystemEntry` ne filtre pas sur le country — pas de risque fonctionnel. La normalisation cosmétique est hors scope.
- **UUID range** : préfixe `f1d38000-...` pour les nouvelles `CONVENTION_BAREMES` FR, `f1d38100-...` pour BE, `f1d38200-...` pour `CONVENTION_PREAVIS` — distinct de F-129 (`f2000000-...`) et SF-DT-25 (`f1d25000-...`).
