# Mini-spec — F-192 / SF-192-03 Section "Stratégies retenues" en début d'export PDF synthèse

## Identifiant

`F-192 / SF-192-03`

## Feature parente

`F-192` — Propagation des pistes stratégiques retenues vers outils décisionnels + dashboard + autres blocs synthèse

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-192-03-pdf-export-pistes-retenues`

---

## Objectif

Ajouter une section dédiée « Stratégies retenues » au début de l'export PDF de la synthèse, en mettant en évidence les pistes stratégiques 🟢 Retenue (texte + base juridique + horizon + conditions + alignement avec les outils décisionnels). Le client (avocat) ouvre le PDF et voit en page 2 (juste après la page de garde) ce qu'il a tranché stratégiquement, avant le détail des faits / risques / timeline / etc.

---

## Comportement attendu

### Cas nominal

1. L'avocat clique « Exporter en PDF » sur l'écran synthèse (`SynthesisComponent`) — flux existant `PdfExportService.export(caseFile, synthesis)`.
2. Le service charge avant export les pistes 🟢 Retenue + leur alignement via `RetainedPisteAlignmentService.getForCaseFile(id)` (introduit en SF-192-02). Cache local utilisé si déjà en mémoire.
3. Si ≥ 1 piste RETAINED, le PDF inclut une nouvelle section « Stratégies retenues » insérée **juste après la page de garde** (avant Timeline / Faits / etc.) avec le contenu suivant :
   - **Titre de section** : « 🎯 Stratégies retenues » (icône `push_pin` Material si rendu PNG embarqué, sinon emoji unicode), couleur navy DESIGN_SYSTEM.md
   - **Pour chaque piste RETAINED**, un bloc avec :
     - Texte de la piste (Inter regular, taille 11)
     - Base juridique (JetBrains Mono, taille 9, italique) — si présente
     - Horizon temporel (Inter regular, taille 9) — si présent
     - Conditions (liste à puces, Inter regular, taille 9) — si présentes
     - Statut d'alignement avec l'outil cible :
       - `ALIGNED` → badge `✅ Stratégie alignée avec l'outil <label>` (couleur or DESIGN_SYSTEM.md)
       - `DIVERGENT` → badge `⚠️ Stratégie divergente avec l'outil <label>` (couleur navy + soulignement)
       - `NOT_ANALYZED` → badge `⏳ Outil <label> non encore analysé`
       - `NO_TARGET_TOOL` → pas de badge alignement
   - **Séparateur** entre les pistes (ligne navy fine)
4. Si **aucune** piste RETAINED → aucune section ajoutée au PDF (comportement actuel inchangé).
5. Le rendu utilise `pdfmake` (déjà importé dans `PdfExportService`), pas de nouvelle dépendance.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Endpoint `/retained-pistes-alignment` 404/500 | Section omise du PDF, log warn console, le reste du PDF est généré normalement (fail-open) |
| Timeout endpoint > 5 s | Section omise du PDF (fail-open) |
| Aucune piste RETAINED | Section omise (cas nominal) |
| Pistes RETAINED mais toutes en `NO_TARGET_TOOL` | Section incluse, listant les pistes sans badge alignement |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres exports PDF** : `exportChecklist`, `exportPrudhomeFiche`, `exportImmigrationChecklist`, `exportTribunalTravailFiche`, `exportRecoursImmigration` — tous présents dans `PdfExportService`. **Aucun ne consomme de pistes stratégiques** — ce sont des exports d'outils spécifiques (checklist procédurale, fiche prud'homale, requête tribunal travail). Ils restent inchangés V1. La section « Stratégies retenues » est exclusive à l'export synthèse globale `export(caseFile, synthesis)`.
- [x] **Autres pays** : pas de logique pays — la section est rendue pour FR + BE indifféremment.
- [x] **Autres domaines** : V1 hors scope (cohérent avec SF-192-01/02). Si une piste RETAINED Famille/Travail apparaît avec `NO_TARGET_TOOL`, elle est **quand même affichée** dans la section PDF (juste sans badge alignement). C'est acceptable V1 — l'avocat voit ses choix, même non encore propagés aux outils.
- [x] **Autres UI patterns** : section PDF suit la charte navy/or DESIGN_SYSTEM.md déjà appliquée dans les autres exports `PdfExportService`. Pas de nouveau pattern visuel à introduire.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript** : réutilise `RetainedPisteAlignment` introduit en SF-192-02
- [x] **Service** : extension de `PdfExportService.export(caseFile, synthesis, retainedPistes?)` — nouveau paramètre optionnel + nouvelle méthode privée `buildStrategiesRetenuesSection(retainedPistes)` qui retourne un `Content[]` pdfmake
- [x] **Composants Angular** : `SynthesisComponent` qui appelle `PdfExportService.export` — passe désormais aussi les pistes retenues via le nouveau paramètre
- [x] **Tests existants** : suite Jest `pdf-export.service.spec.ts` à étendre

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — extension d'un export PDF existant. Pas de nouvel outil décisionnel.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Nouveau bloc PDF « Stratégies retenues »** : pourrait être réutilisé V2 dans d'autres exports (export PDF du dashboard agrégé F-167 si jamais ajouté). Documenter le pattern dans la mini-spec pour réutilisation V2.
- [x] **Pas de nouveau service partagé** — extension du `PdfExportService` existant.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `PdfExportService.export(caseFile, synthesis)` (synthèse globale) | Oui | Intégré V1 — nouvelle section en début de doc |
| `PdfExportService.exportChecklist` | Non | Outil F-96 isolé — pas de pistes |
| `PdfExportService.exportPrudhomeFiche` | Non | Outil F-DT-04 isolé |
| `PdfExportService.exportImmigrationChecklist` | Non | Outil F-IM-01 isolé |
| `PdfExportService.exportTribunalTravailFiche` | Non | Outil F-DT-06 isolé |
| `PdfExportService.exportRecoursImmigration` | Non V1 | V2 si signal terrain (export individuel d'un recours pourrait afficher la piste retenue qui l'a motivé) |
| Composants Famille / Travail | Oui mais V2 (cohérent SF-192-01/02) | Pistes Famille/Travail affichées en V1 sans badge alignement (NO_TARGET_TOOL) |

### Décision

- [x] Étendu à la cible principale V1 (export synthèse globale)
- [x] SF parallèles : SF-192-01 backend + SF-192-02 frontend
- [x] V2 envisagé pour `exportRecoursImmigration` (extension par outil)

---

## Critères d'acceptation

- [ ] **CA-01** : sur un dossier avec ≥ 1 piste RETAINED, l'export PDF synthèse inclut une section « 🎯 Stratégies retenues » insérée juste après la page de garde
- [ ] **CA-02** : sur un dossier sans piste RETAINED, le PDF est identique au comportement actuel (aucune section ajoutée)
- [ ] **CA-03** : pour une piste avec `matchStatus = ALIGNED` et `toolIdCible = F-IM-05-arbre-decisionnel-titre`, le bloc affiche le badge `✅ Stratégie alignée avec l'outil TITRE DE SÉJOUR RECOMMANDÉ`
- [ ] **CA-04** : pour une piste avec `matchStatus = DIVERGENT`, le bloc affiche le badge `⚠️ Stratégie divergente avec l'outil <label>`
- [ ] **CA-05** : pour une piste avec `matchStatus = NOT_ANALYZED`, le bloc affiche le badge `⏳ Outil <label> non encore analysé`
- [ ] **CA-06** : pour une piste avec `matchStatus = NO_TARGET_TOOL`, aucun badge alignement n'est affiché (juste texte + base juridique + horizon + conditions)
- [ ] **CA-07** : conditions d'une piste affichées en liste à puces, Inter regular, taille 9
- [ ] **CA-08** : base juridique en JetBrains Mono italique taille 9 (cohérent DESIGN_SYSTEM.md)
- [ ] **CA-09** : palette navy/or strict — pas de rouge utilisé sauf pour `DIVERGENT` (cohérence avec usage rouge dans le reste du PDF)
- [ ] **CA-10 fail-open** : si l'endpoint backend timeout 5 s, l'export PDF se génère quand même sans la section, log warn console
- [ ] **CA-11** : tests Jest unitaires couvrent les 4 cas matchStatus + cas vide + cas erreur endpoint
- [ ] **CA-12** : le nom de fichier exporté reste inchangé (pas de suffixe `-strategies` — la section est inclue dans le PDF synthèse standard)

---

## Périmètre

### Hors scope (explicite)

- (a) Export PDF dédié aux seules pistes stratégiques (pas de bouton « Exporter mes stratégies » séparé)
- (b) Export Word / DOCX
- (c) Personnalisation par l'avocat de l'inclusion / exclusion de chaque piste dans le PDF (V2 si retour terrain)
- (d) Génération de schémas / charts (alignement avocat ↔ outil)
- (e) Inclusion des pistes 🔍 À étudier ou ❌ Écartée — V1 ne sort QUE les RETAINED (les autres restent dans la synthèse en ligne uniquement)
- (f) Pistes de domaines hors Immigration affichées AVEC alignement (V2 — pour V1, Famille/Travail apparaissent sans badge tool)

---

## Valeurs initiales

Aucune nouvelle entité créée. Section optionnelle conditionnée à la présence de pistes RETAINED.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées |
|-------|-------------|-------------|----------------------------|
| Texte piste affiché | Oui | Pas de troncature V1 (les textes sont déjà bornés par `AnalysisJsonTruncator` backend) | Texte libre |
| Base juridique | Non | Affichée si présente | Texte libre |
| Conditions affichées | Non | Liste à puces, max 10 par piste (au-delà → « + N autres conditions », rare en pratique) | Texte libre |
| Label outil | Oui (si toolIdCible présent) | 50 caractères | Lookup `TOOL_LABEL` du composant outil |

---

## Technique

### Composants Angular impactés

- `PdfExportService.export(caseFile, synthesis, retainedPistes?)` (étendu) — nouveau paramètre `retainedPistes?: RetainedPisteAlignment[]`
- `PdfExportService.buildStrategiesRetenuesSection(retainedPistes)` (nouvelle méthode privée) — retourne `Content[]` pdfmake
- `SynthesisComponent.exportToPdf()` (étendu) — appel `RetainedPisteAlignmentService.getForCaseFile(id)` avant `pdfExportService.export()`, passe le résultat
- `RetainedPisteAlignmentService` (réutilisé de SF-192-02)

### Lookup label outil

Pour afficher le badge `<label outil>`, lookup `TOOL_REGISTRY.get(toolIdCible)?.toolLabel` (la propriété `TOOL_LABEL` statique exposée par chaque composant outil — pattern SF-177-03b). Fallback : afficher le `toolIdCible` brut si lookup échoue (defensive).

### Migration

- [x] Aucune migration

---

## Plan de test

### Tests Jest

- [ ] `PdfExportServiceTest` — `export(caseFile, synthesis, [])` (pistes vides) → PDF généré sans section Stratégies retenues (vérifier absence du titre de section dans le content)
- [ ] `PdfExportServiceTest` — `export(caseFile, synthesis, [pisteAligned])` → PDF contient section + bloc piste + badge `✅ Stratégie alignée avec l'outil TITRE DE SÉJOUR RECOMMANDÉ`
- [ ] `PdfExportServiceTest` — `export(caseFile, synthesis, [pisteDivergent])` → PDF contient bloc + badge `⚠️ Stratégie divergente`
- [ ] `PdfExportServiceTest` — `export(caseFile, synthesis, [pisteNotAnalyzed])` → PDF contient bloc + badge `⏳ Outil non encore analysé`
- [ ] `PdfExportServiceTest` — `export(caseFile, synthesis, [pisteNoTargetTool])` → PDF contient bloc sans badge alignement
- [ ] `PdfExportServiceTest` — section insérée juste après page de garde (vérifier ordre dans le `content[]` pdfmake — index attendu)
- [ ] `PdfExportServiceTest` — conditions affichées en liste à puces (vérifier structure `ul` pdfmake)
- [ ] `PdfExportServiceTest` — base juridique en JetBrains Mono italique (vérifier prop `font: 'JetBrainsMono', italics: true`)
- [ ] `SynthesisComponentTest` — clic export → `RetainedPisteAlignmentService.getForCaseFile` appelé avant `pdfExportService.export`, résultat passé en 3ᵉ argument
- [ ] `SynthesisComponentTest` — endpoint timeout → export quand même appelé avec 3ᵉ argument `[]`, pas de blocage UI

### Isolation workspace

- [x] Non applicable côté frontend — couvert par SF-192-01 backend (endpoint isolé)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non touché
- [x] **Aucune préoccupation transversale** — extension fonctionnelle isolée

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `PdfExportService.export` | Nouveau paramètre optionnel — signature retro-compat | Tests Jest existants `pdf-export.service.spec.ts` à valider sans régression |
| `SynthesisComponent.exportToPdf` | Nouvel appel async avant export | Tests Jest synthesis à valider |

### Smoke tests E2E concernés

- [ ] Aucun smoke E2E nouveau — couverture Jest suffisante

---

## Dépendances

### Subfeatures bloquantes

- F-176 — Terminée (pistes existent)
- F-IA-04 — Terminée (TOOL_REGISTRY pour lookup label)
- F-177 SF-177-03b — Terminée (`TOOL_LABEL` statique sur composants outils)
- **SF-192-01 backend** — endpoint `/retained-pistes-alignment` requis
- **SF-192-02 frontend** — `RetainedPisteAlignmentService` + modèle TypeScript requis

### Questions ouvertes impactées

- [ ] Aucune

---

## Impact par domaine métier

- **Droit du travail** : V1 — pistes Travail RETAINED affichées dans la section PDF mais sans badge alignement (NO_TARGET_TOOL côté backend)
- **Droit immigration** : V1 couvert intégralement — alignement affiché pour F-IM-05 + F-IM-06
- **Droit famille** : V1 — idem Travail (RETAINED affiché sans badge alignement)

L'asymétrie V1 est volontaire et **acceptable** côté PDF : les pistes Famille/Travail RETAINED apparaissent quand même dans le doc — l'avocat voit ses choix, simplement sans le badge alignement. Cela évite de masquer les choix avocat dans le doc, ce qui serait bizarre du point de vue produit.

---

## Parité des domaines métier

Non applicable — extension PDF d'un mécanisme existant. Mais la décision V1 doit être **réévaluée** lors de SF-192 V2 (Famille/Travail) pour que les pistes de ces domaines reçoivent aussi le badge alignement.

---

## Notes et décisions

- **Décision 2026-05-06** : section insérée **après page de garde** (page 2) plutôt qu'en fin de doc, pour la mettre en évidence — c'est ce que l'avocat veut voir d'abord.
- **Décision 2026-05-06** : V1 affiche les pistes Famille/Travail SANS badge alignement plutôt que de les omettre — privilégie la visibilité des choix avocat sur la cohérence d'alignement (qui sera comblée V2).
- **Décision 2026-05-06** : pas de bouton « Exporter uniquement mes stratégies » séparé V1 — la section est inclue dans le PDF synthèse standard, simplifie l'UX.
- **Décision 2026-05-06** : pas de pistes 🔍 À étudier ou ❌ Écartée dans le PDF — uniquement RETAINED. Les autres restent dans la synthèse en ligne (l'avocat les voit dans l'app, pas dans le doc final qu'il pourrait communiquer au client).
- **Décision 2026-05-06** : fail-open strict — si endpoint timeout, le PDF se génère sans la section. L'avocat ne perd jamais son export.
