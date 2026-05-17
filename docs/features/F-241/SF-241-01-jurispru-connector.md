# Mini-spec — F-241 / SF-241-01 Connecteur jurispru léger (deeplinks Doctrine / Lexis Plus / Lextenso)

## Identifiant
`F-241 / SF-241-01`

## Feature parente
`F-241` — Connecteur jurispru léger — deeplinks Doctrine / Lexis Plus / Lextenso + queries IA pré-formulées

## Statut `done` (mergé — PR #974, 2026-05-13) · Date `2026-05-13` · Branche `feat/SF-241-01-jurispru-connector`

---

## Objectif

Afficher à côté de chaque point juridique de la synthèse (composant `synthesis-points-juridiques`) trois boutons qui ouvrent dans un nouvel onglet la recherche jurisprudence pré-formulée chez Doctrine, Lexis Plus et Lextenso. L'avocat conserve son abonnement chez l'éditeur de son choix — LegalCase n'est qu'un orchestrateur via deeplinks publics.

---

## Contexte business

Origine : 11+ signaux terrain mai 2026 (Mengue 11/05, Gaspard 07/05, Renversez au booking 12/05, ~8 autres prospects SAF/BE) — la recherche jurispru open data est régulièrement citée comme attendu. La construction d'un produit jurispru propre est écartée (option A : 6-12 mois d'effort + accord commercial incertain face à Wolters Kluwer / LexisNexis / Lefebvre). Le connecteur léger via deeplinks couvre 80 % du besoin sans dépendance API.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre la synthèse d'un dossier analysé (`/case-files/{id}/synthesis/points-juridiques`)
2. Pour chaque `pointJuridique` listé, **3 boutons compacts** sont affichés en bas de la card :
   - **🔍 Doctrine**
   - **🔍 Lexis+**
   - **🔍 Lextenso**
3. Au clic sur un bouton, un nouvel onglet s'ouvre vers l'éditeur correspondant avec la query déjà pré-formulée dans la barre de recherche
4. L'avocat (déjà connecté chez l'éditeur via son propre abonnement) consulte les arrêts qui matchent et copie ce qui l'intéresse dans ses conclusions
5. Aucune communication backend : pas d'enregistrement, pas de tracking V1

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Texte du point juridique vide ou null | Boutons non rendus (early return) |
| Mots-clés générés trop courts (< 2 tokens utiles) | Boutons rendus mais query = texte brut tronqué à 200 chars (fallback) |
| Éditeur change son URL pattern dans le futur | Bouton continue à fonctionner techniquement, mais peut atterrir sur page d'erreur côté éditeur — détection via test E2E mensuel manuel |
| Utilisateur sans abonnement chez l'éditeur cible | Atterrit sur la page d'inscription/connexion de l'éditeur (comportement géré par l'éditeur, hors notre périmètre) |

---

## Analyse de cohérence transversale

### Cibles scannées

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres surfaces synthèse pouvant accueillir le même connecteur (alertes F-IA-03, pièces manquantes, questions IA, sections décisionnelles) | **Oui pour V2** | Intégré dans cette SF uniquement pour `synthesis-points-juridiques` (cible la plus directe — 1 point juridique = 1 question de recherche). **V2 / SF-241-02 backlog** : extension aux 3 autres surfaces si signaux terrain post-livraison F-241 le justifient. |
| Autres éditeurs jurispru (Dalloz, Lefebvre, Lamy, Stradalex BE) | Oui à terme | V1 = 3 éditeurs majeurs (Doctrine FR/BE, Lexis Plus FR, Lextenso FR). V2 selon retour utilisateurs. Architecture extensible : ajouter un éditeur = 1 ligne dans le mapping. |
| Autres pays (BE) | Couvert nativement | Doctrine couvre FR + BE (`doctrine.fr` et `doctrine.be`). Si `workspaceCountry === 'BE'`, le deeplink Doctrine bascule vers `doctrine.be`. Lextenso et Lexis Plus = FR-only en V1. Pour la BE, alternative belge = Stradalex (V2). |
| Pattern UI bouton externe | Nouveau | Composant `<app-jurisprudence-deeplinks>` standalone réutilisable. Pas d'équivalent existant. |
| Service de génération de query depuis du texte juridique | Nouveau | Utility `JurisprudenceDeeplinkBuilder` (stateless pure functions). Pas d'équivalent existant. |

### Décision

- [x] Étendu uniquement à `synthesis-points-juridiques` dans cette SF — délibéré pour rester à 4-6 jours dev
- [x] Extension aux autres surfaces (alertes F-IA-03 / pièces manquantes / questions IA / sections décisionnelles) : **SF-241-02 backlog** déclenchée par signaux post-livraison
- [x] Extension aux autres éditeurs (Dalloz, Lefebvre, Stradalex BE) : **V2 backlog F-241** déclenchée par demande explicite

---

## Nouveau pattern UI ou service partagé

- [x] **Nouveau composant `<app-jurisprudence-deeplinks>`** sous `frontend/src/app/shared/jurisprudence-deeplinks/` — réutilisable. Pas de pattern équivalent dans l'app actuellement (les boutons « source » sont gérés par `<app-source-ref>`, indépendant).
- [x] **Nouveau utility `jurisprudence-deeplink-builder.ts`** sous `frontend/src/app/shared/jurisprudence-deeplinks/` — pure functions stateless, testables isolément (extraction keywords, génération URLs).
- [x] **Pas de pattern concurrent à harmoniser** — première fois qu'on intègre des deeplinks externes vers éditeurs juridiques dans LegalCase.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — la SF livre un composant utilitaire externe (deeplinks vers sites tiers), ce n'est ni un composant décisionnel intégré au panel F-IA-04 via `TOOL_REGISTRY`, ni un outil qui consomme `aiData` ou produit un scoring. Aucun pré-fill IA, aucune validation F-IA-03 nécessaire, aucun `getPrefillCount`. Justification : c'est un connecteur d'orchestration externe, pas un outil métier.

---

## Critères d'acceptation

- [ ] À côté de chaque point juridique affiché dans `synthesis-points-juridiques`, **3 boutons compacts** « Doctrine », « Lexis+ », « Lextenso » sont rendus en bas de la `point-card__body`
- [ ] Au clic sur un bouton, **un nouvel onglet s'ouvre** (`target="_blank"` + `rel="noopener noreferrer"`) vers l'éditeur correspondant
- [ ] L'URL générée contient la query pré-formulée dans le paramètre `q` (ou équivalent selon l'éditeur), correctement **URL-encoded** (espaces, accents, caractères spéciaux)
- [ ] **Query générée** : extraction des mots-clés du texte du point juridique via tokenisation simple (tokens ≥ 4 lettres, filtrage stop-words FR via liste `STOPWORDS_FR` interne au builder, max 8 tokens significatifs)
- [ ] **Doctrine pour workspace BE** : le deeplink utilise `doctrine.be` au lieu de `doctrine.fr` quand `workspaceCountry === 'BE'`
- [ ] **Lexis+ et Lextenso** : seulement rendus si `workspaceCountry === 'FR'` (pas pertinents pour BE en V1)
- [ ] Composant **standalone Angular** (`standalone: true`), pas d'NgModule
- [ ] Aucune communication backend (frontend pur)
- [ ] Tests Jest unitaires couvrant : (a) extraction keywords sur texte connu, (b) génération URL Doctrine FR, (c) génération URL Doctrine BE, (d) génération URL Lexis Plus, (e) génération URL Lextenso, (f) rendering 3 boutons côté composant, (g) `target="_blank"` + `noopener noreferrer` présents, (h) gating BE (Lexis+/Lextenso non affichés)
- [ ] **Aucune régression** sur le composant `synthesis-points-juridiques` existant (les autres éléments — titre, source-ref, expand/collapse — fonctionnent comme avant)

---

## Périmètre

### Hors scope (explicite)

- Intégration API native d'aucun éditeur (Option A reportée à V10+ si traction permet négociation)
- Affichage in-app des résultats jurispru (l'utilisateur reste sur l'éditeur tiers, c'est délibéré)
- Tracking d'analytics sur les clics deeplinks (V2 si besoin)
- Préférence utilisateur éditeur favori (V2 / SF-241-02 selon retour)
- Extension aux alertes F-IA-03, pièces manquantes, questions IA, sections décisionnelles (V2 selon retour)
- Ajout d'autres éditeurs (Dalloz, Lefebvre, Stradalex BE, Lamy) (V2 selon retour)
- Re-formulation IA de la query (la query V1 est extractive simple ; un appel LLM pour reformuler optimalement = surcoût + latence injustifié à ce stade)
- Test E2E automatique de la disponibilité des URLs (les patterns d'URL peuvent évoluer chez l'éditeur ; on documente la procédure de check manuel mensuel + on log les fallbacks)
- Internationalisation de l'UI des boutons (FR uniquement en V1)

---

## Patterns URLs des 3 éditeurs (à valider en check manuel avant merge)

| Éditeur | Pattern URL V1 | À vérifier |
|---------|----------------|------------|
| Doctrine FR | `https://www.doctrine.fr/search?q={URL-encoded-keywords}&type=jurisprudence` | Vérifier en condition réelle si la query rend bien la barre de recherche pré-remplie. Si non, fallback `https://www.doctrine.fr/recherche?q=...` |
| Doctrine BE | `https://www.doctrine.be/search?q={URL-encoded-keywords}&type=jurisprudence` | Idem |
| Lexis Plus (Lexis 360 Intelligence) | `https://www.lexis360intelligence.fr/recherche?q={URL-encoded-keywords}` | Pattern à vérifier (possible que l'URL exacte soit `/recherche?keyword=...`) |
| Lextenso | `https://www.lextenso.fr/recherche?searchText={URL-encoded-keywords}` | Pattern à vérifier (possible que ce soit `?q=...`) |

**Note** : si un pattern ne marche pas en check manuel, le builder TypeScript est paramétrable en 1 ligne. Pas de bloqueur architectural.

---

## Technique

### Composants Angular nouveaux

- **`frontend/src/app/shared/jurisprudence-deeplinks/jurisprudence-deeplink-builder.ts`** : pure functions stateless
  - `extractKeywords(text: string): string[]` — tokenisation + filtrage stop-words FR + dedup, max 8 tokens
  - `buildDoctrineUrl(keywords: string[], country: 'FR' | 'BE'): string`
  - `buildLexisPlusUrl(keywords: string[]): string`
  - `buildLextensoUrl(keywords: string[]): string`
  - Constante `STOPWORDS_FR` : liste 80-100 stop-words français standards (le, la, les, de, du, et, ou, à, en, dans, sur, etc.)
- **`frontend/src/app/shared/jurisprudence-deeplinks/jurisprudence-deeplinks.component.ts`** : composant standalone
  - Inputs : `[pointText]` (string requis), `[workspaceCountry]` ('FR' | 'BE', défaut 'FR')
  - Template : 3 `<a>` boutons MatButton avec icônes MatIcon `search` + texte court + `target="_blank"` + `rel="noopener noreferrer"`
  - Pas d'output, pas de subscribe, pas d'injection lourde

### Composants Angular modifiés

- **`synthesis-points-juridiques.component.ts`** : import de `JurisprudenceDeeplinksComponent` dans le décorateur, ajout d'un champ `workspaceCountry = signal<'FR' | 'BE'>('FR')` initialisé depuis `caseFile()?.country`
- **`synthesis-points-juridiques.component.html`** : injection de `<app-jurisprudence-deeplinks [pointText]="item.texte" [workspaceCountry]="workspaceCountry()" />` à l'intérieur de chaque `<article class="point-card">`, juste avant le bouton `expand` toggle
- **`synthesis-points-juridiques.component.scss`** : style mineur pour positionner les boutons sur une ligne, gap 8px

### Tests à créer

- **`frontend/src/app/shared/jurisprudence-deeplinks/jurisprudence-deeplink-builder.spec.ts`** : Jest pur (sans Angular TestBed) — 8 tests minimum (extraction, 4 builders × 1 cas + 1 cas avec accents/spéciaux)
- **`frontend/src/app/shared/jurisprudence-deeplinks/jurisprudence-deeplinks.component.spec.ts`** : Jest avec TestBed — 4 tests minimum (rendu 3 boutons FR, rendu 1 bouton Doctrine seul BE, attributs `target` + `rel`, gestion pointText vide)
- **`frontend/src/app/case-files/synthesis-points-juridiques/synthesis-points-juridiques.component.spec.ts`** : ajout 1 test de non-régression « le composant `<app-jurisprudence-deeplinks>` est rendu pour chaque point juridique »

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Endpoints

Aucun nouvel endpoint. Pas de modification backend.

---

## Plan de test

### Tests unitaires builder

- `extractKeywords('La rupture conventionnelle nécessite un consentement libre et éclairé du salarié')` → contient `['rupture', 'conventionnelle', 'consentement', 'libre', 'éclairé', 'salarié']` (stop-words `la`, `et`, `du`, `un` exclus)
- `extractKeywords('')` → `[]`
- `extractKeywords('a b c d e f g h')` → `[]` (tous < 4 lettres)
- `buildDoctrineUrl(['rupture', 'conventionnelle', 'consentement'], 'FR')` → `https://www.doctrine.fr/search?q=rupture+conventionnelle+consentement&type=jurisprudence`
- `buildDoctrineUrl(['rupture'], 'BE')` → `https://www.doctrine.be/search?q=rupture&type=jurisprudence`
- `buildLexisPlusUrl(['rupture', 'conventionnelle'])` → `https://www.lexis360intelligence.fr/recherche?q=rupture+conventionnelle`
- `buildLextensoUrl(['rupture'])` → `https://www.lextenso.fr/recherche?searchText=rupture`
- `extractKeywords('Insuffisance professionnelle: refus de formation contestable')` → contient `['insuffisance', 'professionnelle', 'refus', 'formation', 'contestable']`, encodage URL correct des espaces et apostrophes
- Cas accents : `extractKeywords('Caractère réel et sérieux du licenciement')` → contient `caractère`, `réel`, `sérieux`, `licenciement` ; URL encoding correct des é/è/ç

### Tests composant `<app-jurisprudence-deeplinks>`

- Rendu de 3 boutons quand `workspaceCountry='FR'`
- Rendu de 1 seul bouton (Doctrine BE) quand `workspaceCountry='BE'`
- Attribut `target="_blank"` et `rel="noopener noreferrer"` présents sur tous les boutons
- `pointText` vide → composant ne rend rien (early return)

### Tests intégration (composant parent)

- Non-régression `synthesis-points-juridiques` : le composant `<app-jurisprudence-deeplinks>` est rendu pour chaque point juridique de `pointsJuridiques`
- Les autres éléments (titre, source-ref, expand/collapse) fonctionnent comme avant

### Isolation workspace

- Non applicable — composant frontend pur, ne touche aucune donnée workspace-scoped en lecture/écriture. Le `workspaceCountry` est lu depuis `caseFile()` qui est déjà workspace-scoped par les services existants.

---

## Impact par domaine métier

**Transversal** — la SF s'applique aux 3 domaines (Droit du travail, Immigration, Famille) et aux 2 pays (FR, BE). Le mécanisme est domain-agnostic : il extrait des mots-clés depuis le texte d'un point juridique généré par l'IA, qui lui-même est déjà domain-aware. Aucune adaptation domain-specific côté connecteur. Pour la BE, seul Doctrine BE est exposé en V1 (Lexis+ et Lextenso n'ont pas d'équivalent BE pertinent ; Stradalex BE = backlog V2 si demande).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée frontend, additive, impact limité à son périmètre. Pas de modification du Principal, du workspace context, des plans/limites, ou du routing. Pas de modification backend, pas de migration, pas de modification DB.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné. Justification : les smoke tests de `e2e/smoke/` couvrent auth, workspace switch, navigation post-login — aucun ne touche la page Points juridiques. Cette SF ajoute du contenu visuel sans modifier le routing ni les flux d'auth/workspace.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. F-241 SF-01 est autonome. Le pipeline IA existant (`EnrichedAnalysisService`) produit déjà les points juridiques avec leur `texte`.

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

### Décision 1 — Frontend-only vs backend
La logique de génération de query est implémentée **côté frontend** (TypeScript pur), pas dans le backend Spring Boot. Raisons :
- Pas de migration DB
- Pas de re-déploiement backend pour ajuster les patterns URL
- Tests unitaires Jest rapides (pas de Spring Boot)
- L'utility est stateless et n'a pas besoin d'accès DB
- Si V2 ajoute du tracking analytics ou de la reformulation LLM, on bougera la logique côté backend à ce moment-là (architecture refactorable sans casser le contrat frontend↔backend)

### Décision 2 — Extraction keywords extractive vs LLM
L'extraction de mots-clés est faite par **heuristique simple** (tokenisation + stop-words FR), pas par appel LLM. Raisons :
- Latence : extraction instantanée vs ~500-1500ms pour un appel LLM
- Coût : 0 € vs ~$0.001 par clic deeplink (cumulé sur 1000 prospects = $1, négligeable mais réel)
- Qualité : l'extraction simple suffit pour 80 % des cas car le texte du point juridique est déjà rédigé par l'IA de manière concise et keyword-rich
- Si la qualité s'avère insuffisante après livraison (signal terrain), V2 peut introduire une re-formulation LLM optionnelle

### Décision 3 — Patterns URLs à valider manuellement
Les patterns URL de Doctrine, Lexis Plus, Lextenso sont **présumés** à partir de l'observation des barres URL de leurs moteurs de recherche publics. Un **check manuel obligatoire** avant merge :
- Test sur 3 queries différentes par éditeur
- Vérification visuelle que la barre de recherche est bien pré-remplie
- Documentation des éventuels ajustements de paramètres dans le code

### Décision 4 — Pas de préférence utilisateur en V1
On affiche les 3 boutons côte à côte en V1. La préférence utilisateur d'éditeur favori (pour n'afficher qu'un seul bouton ou réordonner) est reportée en SF-241-02 si signaux terrain post-livraison le justifient.

### Décision 5 — Lexis+ et Lextenso uniquement FR en V1
Pour `workspaceCountry='BE'`, seul Doctrine BE est exposé. Lexis+ et Lextenso n'ont pas d'offre BE pertinente à ce stade. Stradalex BE est noté en V2 si demande.
