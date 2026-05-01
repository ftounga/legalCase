# Mini-spec — F-177 / SF-177-12 Badge "Pré-rempli IA" sur les cards du panel décisionnel — infra + pilote 4 outils Immigration FR

> Constat terrain (dossier "Immigration Chen 5", staging, 2026-05-02) : après
> analyse + enrichie, l'avocat sait que plusieurs outils ont été pré-remplis
> par l'IA (Checklist Pièces Immigration, Titre de Séjour Recommandé,
> Changement de Statut, Droit au Travail) mais aucune card ne le signale.
> Les badges sont à l'intérieur des composants, invisibles avant clic.
> F-177 avait spec'é cette mise en évidence (axe 2 "3 badges discrets en
> coin de card") mais SF-177-11 a marqué les badges hors scope (incrément
> "SF-177-12+"). Cette SF active le badge `auto_awesome` "pré-rempli IA"
> via le pattern miroir des `static TOOL_LABEL`/`TOOL_ICON`.

---

## Identifiant

`F-177 / SF-177-12`

## Feature parente

`F-177` — Refonte panel F-IA-04 (cards verdict + modal)

## Statut

`draft`

## Date de création

2026-05-02

## Branche Git

`feat/SF-177-12-prefill-count-cards`

---

## Objectif

Activer **côté card** le badge `auto_awesome` "Pré-rempli par l'IA (N champs)"
sur les outils du panel F-IA-04 — via un nouveau static `getPrefillCount(...)`
exposé par chaque composant outil (pattern miroir `TOOL_LABEL` / `TOOL_ICON`).
Cette SF livre **l'infra** + **4 composants pilotes Immigration FR** demandés
par l'utilisateur. Les autres domaines suivront en SF-177-13+ symétriques aux
SF-177-03/03b/05/07.

---

## Comportement attendu

### Cas nominal

1. Chaque composant outil instrumenté expose une méthode statique :
   ```ts
   static getPrefillCount(input: PrefillCountInput): number
   ```
   où `PrefillCountInput` est un objet `{ aiData?, procedureChecks?, aiQuestions?, piecesManquantes? }` typé large (compatible avec ce que la TOOL_REGISTRY passe déjà aux inputs des composants).
2. La méthode retourne le **nombre de champs du formulaire que `prefillFromAi()` poserait** si le composant était instancié maintenant avec ces inputs. Stricte parité avec la logique réelle du composant (mêmes guards `typeof === 'string'`, `ISO_DATE_RE.test(...)`, etc.).
3. Le panel calcule ce compteur via un helper `getToolPrefillCount(component, ctx)` (extension de `decision-tool.contract.ts`).
4. Le panel passe `[prefillCount]="getToolPrefillCount(item.entry.component, ctx)"` à chaque `<app-decision-tool-card>`.
5. La card affiche le badge `auto_awesome` (avec tooltip "N champ(s) pré-rempli(s) par l'IA") dès que `prefillCount > 0`. Mécanique déjà présente (SF-177-01).
6. Pour les composants **non instrumentés** dans cette SF, `getToolPrefillCount` retourne `null` → `prefillCount = null` → badge masqué (fallback safe).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Composant n'expose pas `getPrefillCount` | `getToolPrefillCount` retourne `null` (badge masqué). Pas d'erreur. Pas de log (cas légitime tant que toutes les SF d'instrumentation ne sont pas livrées). |
| Composant expose `getPrefillCount` mais throw à l'exécution | `getToolPrefillCount` capture l'erreur, log `console.warn` une fois par toolId, retourne `null`. Pas de crash du panel. |
| `aiData` absent dans le contexte (analyse non encore faite) | Chaque `getPrefillCount` retourne `0` silencieusement. Badge masqué. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Pattern existant `TOOL_LABEL` / `TOOL_ICON`** : déjà introduit par SF-177-03/03b/05/07 sur ~30 composants. Cette SF étend strictement le contrat `DecisionToolStatic` avec un troisième champ optionnel `getPrefillCount`. Pas de duplication, prolongation directe.
- [x] **Pattern existant `prefillFromAi()`** : présent sur les ~30 composants décisionnels (audit confirmé sur le ticket SF-IM-11-03 le 2026-05-01). La méthode statique `getPrefillCount` reflète la logique de `prefillFromAi()` mais sans toucher au state du composant — calcul pur sur les inputs. Risque de divergence si l'un évolue sans l'autre → atténué par la cohabitation dans le même fichier + tests Jest qui couvrent les deux.
- [x] **Helper `getToolMetadata(component)`** dans `decision-tool.contract.ts` : pattern à dupliquer pour `getToolPrefillCount`. Pas de refactor de l'existant.
- [x] **3 badges card prévus par F-177** : pré-rempli (cette SF), F-IA-03 cohérence (futur), métier (futur). La SF active uniquement le 1er. Les 2 autres restent en backlog SF-177-13+ ou plus tard.
- [x] **Autres domaines** : Travail FR + BE et Famille FR + BE ont aussi `prefillFromAi()`. Non instrumentés par cette SF — backlog SF-177-13/14/15/16/17/18 par domaine × pays (symétrique au découpage SF-177-03 → 08 utilisé pour `TOOL_LABEL`).
- [x] **Belgique** : aucun composant Immigration BE dans le pilote (volontaire — l'utilisateur teste FR). À couvrir SF-177-16 plus tard.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le pattern peut-il être réutilisé ?** : tout outil décisionnel futur. Le pattern `getPrefillCount` doit devenir un attendu CLAUDE.md (cf. règle "Nouveau composant Angular décisionnel frontend") — à inscrire en suite directe.
- [x] **Patterns concurrents** : aucun. Aujourd'hui le panel n'a aucune logique de comptage des champs pré-remplis par outil. C'est un pattern neuf, pas un remplacement.
- [x] **Le helper `getToolPrefillCount` peut-il servir ailleurs ?** : oui, le dashboard agrégé (SF-177-09) pourrait l'utiliser pour afficher des badges sur ses tiles. Hors scope de cette SF, à backlog.

### Décision

- [x] Étendu à toutes les cibles **prioritaires demandées par l'utilisateur** (4 outils Immigration FR).
- [x] SF parallèles à créer pour le reste (SF-177-13 Travail FR, -14 Travail BE, -15 Immigration FR restants, -16 Immigration BE, -17 Famille FR, -18 Famille BE) — non bloquant.
- [x] Backlog : extension du helper au dashboard agrégé (SF-177-09).
- [x] Mise à jour CLAUDE.md règle "Nouveau composant décisionnel frontend" pour intégrer `getPrefillCount` comme attendu — à faire **dans cette SF** (commit doc).

---

## Critères d'acceptation

- [ ] **Infrastructure** : `decision-tool.contract.ts` étendu — interface `PrefillCountInput`, `DecisionToolStaticWithPrefill` (extension), fonction `getToolPrefillCount(component, input): number | null`. Tests unitaires : retourne `null` si static absent, retourne le nombre si static présent, capture les erreurs (`console.warn` 1x + retourne `null`).
- [ ] **Panel** : `decisional-tools-panel.component.html` passe `[prefillCount]="prefillCountFor(item.toolId)"` à chaque `<app-decision-tool-card>`. Méthode `prefillCountFor(toolId)` dans le `.ts` qui résout l'entrée TOOL_REGISTRY + appelle `getToolPrefillCount` avec le contexte courant.
- [ ] **Pilote 1 — `immigration-title-decision-section`** : expose `static getPrefillCount(input)` qui retourne le nombre de champs parmi `{nationaliteUe, motif (typeTitreSejourCode/typeTitreSejour), situationFamiliale (depuis triggerEvents)}` qui seraient pré-remplis. Tests Jest : 0/3 (aiData null), 1/3 (typeTitreSejourCode seul), 3/3 (cas Chen 5 — nationaliteUe+typeTitreSejourCode+triggerEvent).
- [ ] **Pilote 2 — `immigration-work-right-section`** : `getPrefillCount` retourne 1 si `typeTitreSejourCode` présent, 0 sinon. Tests Jest : 0/1, 1/1.
- [ ] **Pilote 3 — `changement-statut-section`** : `getPrefillCount` retourne le nombre parmi `{titreActuel (depuis typeTitreSejourCode/typeTitreSejour), dureeRestanteSurTitreActuelMois (depuis dateExpirationTitre)}` — 0/2, 1/2, 2/2 selon les inputs.
- [ ] **Pilote 4 — `immigration-checklist-section`** : `getPrefillCount` retourne le nombre de pièces auto-cochées depuis `piecesManquantes` (ou autre source IA si `prefillFromAi()` consomme autre chose — vérifier l'implémentation actuelle avant de coder le compteur). Tests Jest : 0/N, M/N selon les pièces.
- [ ] **Effet visuel sur "Immigration Chen 5" (test manuel post-merge)** : les 4 cards listées ci-dessus affichent le badge `auto_awesome` avec un tooltip indiquant le nombre de champs pré-remplis. Les autres outils (non instrumentés) n'affichent pas de badge (sont en attente de SF-177-13+).
- [ ] **CLAUDE.md mis à jour** : la règle "Nouveau composant Angular décisionnel frontend" inclut désormais "exposer `static getPrefillCount(input): number` pour que la card du panel affiche le badge pré-rempli IA" (item 6 de la checklist).
- [ ] **Aucune régression** : 100% tests Jest existants passent. Les composants non instrumentés (Travail / Famille / BE) continuent de fonctionner sans badge (fallback null).

---

## Périmètre

### Hors scope

- Pas de badge F-IA-03 cohérence ni badge métier sur la card (futur, backlog SF-177-19+ ou plus).
- Pas d'instrumentation des ~26 autres composants (Travail FR/BE, Famille FR/BE, Immigration BE, Immigration FR restants : naturalisation, asile-avance, mineurs, oqtf, aes-*) — backlog SF-177-13 → 18.
- Pas d'extension au dashboard agrégé (SF-177-09) — backlog.
- Pas de verdict synthétique sur la card (le `<app-decision-tool-card>` a déjà l'input `[summary]` mais n'est pas alimenté — c'est une autre lutte, hors scope).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Notes |
|---|---|---|---|
| `getPrefillCount` retour | Oui (si statique exposé) | `number` ≥ 0 | Si throw → `getToolPrefillCount` retourne `null` |
| `prefillCount` input de card | Optionnel | `number \| null` | Pattern existant inchangé (SF-177-01) |

---

## Technique

### Endpoints

Aucun. Frontend pur.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Non applicable.

### Composants Angular

**Fichiers modifiés** :
- `frontend/src/app/case-files/decisional-tools-panel/decision-tool.contract.ts` — ajoute `PrefillCountInput`, `getToolPrefillCount`.
- `frontend/src/app/case-files/decisional-tools-panel/decision-tool.contract.spec.ts` — tests unitaires.
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — méthode `prefillCountFor(toolId)`, helper qui résout l'entrée + construit `PrefillCountInput` depuis le contexte.
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.html` — `[prefillCount]="prefillCountFor(item.toolId)"` sur la card.
- `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.ts` — `static getPrefillCount`.
- `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.spec.ts` — tests.
- `frontend/src/app/case-files/immigration-work-right-section/immigration-work-right-section.component.ts` — `static getPrefillCount`.
- `frontend/src/app/case-files/immigration-work-right-section/immigration-work-right-section.component.spec.ts` — tests.
- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.ts` — `static getPrefillCount`.
- `frontend/src/app/case-files/changement-statut-section/changement-statut-section.component.spec.ts` — tests.
- `frontend/src/app/case-files/immigration-checklist-section/immigration-checklist-section.component.ts` — `static getPrefillCount`.
- `frontend/src/app/case-files/immigration-checklist-section/immigration-checklist-section.component.spec.ts` — tests.
- `CLAUDE.md` — extension règle composant décisionnel frontend (item 6).

---

## Plan de test

### Tests unitaires (Jest)

**Helper `decision-tool.contract.spec.ts`**
- [ ] `getToolPrefillCount` retourne `null` si le static `getPrefillCount` n'est pas exposé.
- [ ] `getToolPrefillCount` retourne le nombre si le static est exposé.
- [ ] `getToolPrefillCount` capture une exception du static, log `console.warn`, retourne `null`.

**Composant 1 — immigration-title-decision-section**
- [ ] `getPrefillCount({})` → `0`.
- [ ] `getPrefillCount({ aiData: { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } })` → `1`.
- [ ] `getPrefillCount({ aiData: { nationaliteUe: false, typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE', triggerEvents: [{ event_code: 'MARIAGE_RESSORTISSANT_FR' }] } })` → `3` (cas Chen 5).

**Composant 2 — immigration-work-right-section**
- [ ] `getPrefillCount({})` → `0`.
- [ ] `getPrefillCount({ aiData: { typeTitreSejourCode: '...' } })` → `1`.

**Composant 3 — changement-statut-section**
- [ ] `getPrefillCount({})` → `0`.
- [ ] `getPrefillCount({ aiData: { typeTitreSejourCode: 'CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE' } })` → `1` (titre actuel uniquement).
- [ ] `getPrefillCount({ aiData: { typeTitreSejourCode: '...', dateExpirationTitre: '2026-08-31' } })` → `2` (cas Chen 4/5).
- [ ] `getPrefillCount({ aiData: { dateExpirationTitre: '2024-01-01' } })` → `1` (date passée → mois = 0 mais champ posé).

**Composant 4 — immigration-checklist-section**
- [ ] Tests adaptés à la logique réelle de `prefillFromAi()` du composant (à vérifier au moment de l'implémentation — la mini-spec ne préempte pas l'audit).

**Panel — `decisional-tools-panel.component.spec.ts`**
- [ ] `prefillCountFor('F-IM-05')` retourne le bon nombre quand l'analyse est présente.
- [ ] `prefillCountFor('toolId-non-instrumenté')` retourne `null` (fallback OK).

### Tests d'intégration

Aucun (frontend pur).

### Isolation workspace

- [x] Non applicable — pas d'accès aux données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension d'un pattern UI existant, pas d'auth / workspace / plans / navigation.

### Composants impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| Panel `decisional-tools-panel` | Nouvelle méthode + nouvel input passé à la card | Tests Jest existants du panel (3000+ tests) doivent rester verts |
| `<app-decision-tool-card>` | Aucun changement de surface (input `prefillCount` déjà présent depuis SF-177-01) | Tests Jest existants de la card |
| 4 composants Immigration FR | Ajout d'un static seul, pas de modif du runtime existant | Tests Jest existants des 4 composants |

### Smoke tests E2E

- [x] Aucun smoke E2E concerné — l'effet est purement visuel sur le panel.

---

## Impact par domaine métier

- **DROIT_DU_TRAVAIL** : l'infra est utilisable mais aucun outil instrumenté dans cette SF — backlog SF-177-13/14.
- **DROIT_IMMIGRATION** : 4 outils FR pilotes activés (couverture des outils les plus visibles sur Chen 5). Le reste (naturalisation, asile, mineurs, oqtf, aes-*) → backlog SF-177-15. BE → SF-177-16.
- **DROIT_FAMILLE** : non couvert dans cette SF — backlog SF-177-17/18.
- **France / Belgique** : pilote FR uniquement. BE suit le même pattern.

---

## Parité des domaines métier

L'outil est de **niveau infrastructure transverse** (pas un outil décisionnel métier au sens des 7 niveaux). La parité demandée se manifeste comme suite directe : SF-177-13 → 18 par domaine × pays. Cette SF assume explicitement le pilote pour valider le pattern avant de le répliquer.

---

## Dépendances

### Subfeatures bloquantes

- `SF-177-01` — `done` (composant `<app-decision-tool-card>` avec input `prefillCount`).
- `SF-177-11` — `done` (panel basculé sur cards + modal).
- `SF-177-03b` (et apparentés) — `done` (pattern `static TOOL_LABEL`/`TOOL_ICON` établi).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Pourquoi un static plutôt qu'une méthode d'instance ?** : la card est rendue **avant** que le composant outil soit instancié (le composant n'est instancié qu'à l'ouverture du modal). Le static évite l'instanciation prématurée et reste cohérent avec `TOOL_LABEL`/`TOOL_ICON`.
- **Pourquoi limiter le pilote à 4 outils ?** : couvre exactement les 4 outils que l'utilisateur a cités sur Chen 5 (validation immédiate du pattern). Étendre aux ~26 autres composants ferait > 2 j de dev → REFUS CLAUDE.md. Découpage SF-177-13+ par domaine × pays symétrique aux SF-177-03 → 08 utilisé pour `TOOL_LABEL`.
- **Risque de divergence entre `prefillFromAi()` runtime et `getPrefillCount` static** : atténué par cohabitation dans le même fichier + tests Jest qui couvrent les deux. Si un développeur modifie `prefillFromAi` sans toucher `getPrefillCount`, la valeur affichée sur la card sera fausse → règle CLAUDE.md à ajouter (item 6).
- **Pourquoi `console.warn` 1x sur exception du static ?** : éviter le bruit de log si un composant a un bug récurrent. Le panel reste fonctionnel.
- **Choix de la signature `PrefillCountInput`** : objet avec `aiData?, procedureChecks?, aiQuestions?, piecesManquantes?` typés `any` (pour limiter le couplage). Chaque composant cast vers son type fort (`ImmigrationExtractedData`, etc.) en interne. Pattern miroir des `inputs:` de TOOL_REGISTRY qui sont déjà non-strictement typés.
