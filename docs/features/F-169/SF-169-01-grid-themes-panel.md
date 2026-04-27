# SF-169-01 — Refonte UX panel F-IA-04 : grid 2 colonnes + groupement par thème métier

**Feature parente** : F-169 — Refonte UX panel F-IA-04
**Type** : frontend
**Branche** : `feat/SF-169-01-grid-themes-panel`
**Effort estimé** : ~2-3 h

---

## Objectif (1 phrase)

Remplacer la longue colonne unique verticale du panel F-IA-04 (qui regroupait les outils par layer technique `Outils principaux` (ALWAYS_ON) / `Outils contextuels` (CONTEXTUAL) / `Catalogue`) par **5 thèmes métier** (`Indemnités & calculs`, `Validité & contestation`, `Délais & procédure`, `Documents`, `Diagnostic situation`) affichés en **grid 2 colonnes** (responsive 1 col < 1024px), de sorte que l'avocat scanne les outils par usage métier au lieu de la distinction technique de visibilité.

---

## Contexte

Le panel actuel (`<app-decisional-tools-panel>`) affiche les cards en 3 sections :
1. **Outils principaux** (ALWAYS_ON) — colonne unique verticale, 11 cards en moyenne post F-165
2. **Outils contextuels** (CONTEXTUAL) — colonne unique, 0 à 15 cards selon ce que l'IA détecte
3. **Catalogue** — chips grisés non activables

**Constat 2026-04-27** :
- Distinction `Principaux vs Contextuels` est **technique**, pas métier — un avocat ne se demande pas "ce calcul est-il toujours pertinent ou conditionnel ?", il se demande "quel calcul d'indemnité je peux lancer ?"
- Sur dossier riche : scroll long, fatigue visuelle, pas de hiérarchie sémantique
- Un calcul d'indemnité (ex : précarité CDD) côtoie un outil de validité (ex : validité licenciement) sans regroupement logique
- Capacité largeur écran (cards font ~700-800px de large = 50 % d'un écran 1440px) mal exploitée

Cette SF résout le constat en :
- Conservant la **distinction technique en interne** (le service backend continue de retourner ALWAYS_ON / CONTEXTUAL — comportement F-IA-04 préservé)
- Réorganisant uniquement la **présentation** côté frontend en 5 thèmes métier + grid 2 colonnes

---

## Comportement nominal

1. Le panel charge la visibilité depuis `CaseFileService.getVisibleToolSet()` comme aujourd'hui (signal `visibility()`).
2. Les outils résolus (`alwaysOn` + `contextual`) sont **fusionnés** puis classés selon un **mapping `tool_id → theme`** déclaratif côté composant TS.
3. Pour chaque thème non vide, une section est affichée avec :
   - Titre canonique en `<h2 class="theme-title">{NOM DU THÈME}</h2>` (typo Merriweather serif, comme aujourd'hui)
   - Sous-titre optionnel (description courte du thème)
   - **Grid CSS 2 colonnes** (`grid-template-columns: repeat(2, 1fr); gap: 16px;`)
   - Responsive : `@media (max-width: 1023px) { grid-template-columns: 1fr; }`
4. Chaque card conserve son composant Angular existant (instancié via `*ngComponentOutlet`) et son comportement intrinsèque (collapse/expand, formulaires, etc.).
5. Le **Catalogue** (chips grisés) reste affiché en fin de panel, inchangé (cf. mini-spec F-IA-04).
6. **Ordre des thèmes** (fixe) :
   1. Indemnités & calculs
   2. Validité & contestation
   3. Délais & procédure
   4. Documents
   5. Diagnostic situation
7. **Ordre des outils dans un thème** : ordre de retour du backend (`alwaysOn` puis `contextual`, déjà priorisé). Pas de tri alphabétique.

### Mapping `tool_id → theme` (déclaratif côté TS)

Une constante `static readonly THEME_BY_TOOL_ID: ReadonlyMap<string, ThemeKey>` est ajoutée dans `decisional-tools-panel.component.ts`. Si un `tool_id` n'a pas d'entrée, il est rangé dans le thème **`Diagnostic situation`** (fallback métier — couvre les arbres décisionnels d'orientation).

**Mapping cible** (90 tool_ids actuels) :

#### Indemnités & calculs (16 outils — chiffrage de montant)
- F-DT-07-anciennete-conges-prime
- F-DT-09-comparateur-indemnites
- F-DT-12-discrimination-dommages-interets
- F-DT-15-inaptitude
- F-DT-17-indemnite-precarite-cdd
- F-DT-18-fin-mission-interim
- F-DT-19-heures-sup
- F-DT-20-rappel-salaire
- F-DT-21-travail-dissimule
- F-DT-25-indemnite-preavis
- F-DT-26-conges-payes-indemnite
- F-DT-28-avantages-conventionnels-be
- F-DT-31-transaction
- F-DT-35-contestation-are-fr
- F-132-rupture-conv-indemnite
- F-FA-15-recompenses

#### Validité & contestation (19 outils — validité d'un acte ou d'une clause)
- F-DT-08-licenciement-validity
- F-DT-10-rupture-conv-validity
- F-DT-11-harcelement-licenciement-nul
- F-DT-13-licenciement-economique
- F-DT-14-pse-validite
- F-DT-16-licenciement-nul-detection
- F-DT-22-requalification-cdd-cdi
- F-DT-23-requalification-interim-cdi
- F-DT-24-non-concurrence
- F-DT-27-motif-grave-be
- F-DT-30-protection-rp
- F-FA-08-divorce-alteration
- F-FA-09-divorce-faute
- F-FA-10-divorce-accepte
- F-FA-11-desunion-irremediable-be
- F-FA-18-contestation-paternite
- F-FA-18-recherche-paternite
- F-FA-18-reconnaissance-paternelle
- F-FA-18-possession-etat
- F-FA-24-testament-validite

#### Délais & procédure (15 outils — temporalité, mise en œuvre procédurale)
- F-DT-03-prescription-litige
- F-DT-29-credit-temps-be
- F-DT-33-at-mp
- F-DT-34-refere-prudhomal
- F-FA-12-mesures-provisoires
- F-FA-13-revisions-post-divorce
- F-FA-14-ordonnance-protection
- F-FA-23-ordonnance-requete
- F-IM-06-recours
- F-IM-08-oqtf-avec-delai-fr
- F-IM-08-oqtf-sans-delai-fr
- F-IM-08-referes-admin-fr
- F-IM-08-annexe13-be
- F-IM-20-mesures-eloignement
- F-136-travail-procedure

#### Documents (6 outils — production ou validation de document)
- F-DT-04-fiche-prudhomale
- F-DT-06-requete-tribunal-travail
- F-DT-32-documents-fin-contrat
- F-IM-01-checklist-pieces
- F-FA-07-checklist-divorce
- F-132-rupture-amiable-info

#### Diagnostic situation (35 outils — orientation, arbre décisionnel, choix de régime)
- F-IM-05-arbre-decisionnel-titre
- F-IM-07-droit-au-travail
- F-IM-09-aes-etudiant
- F-IM-09-aes-famille
- F-IM-09-aes-humanitaire
- F-IM-09-aes-metiers-tension
- F-IM-11-changement-statut
- F-IM-12-asile-avance
- F-IM-13-naturalisation
- F-IM-14-40bis-cohabitant-ue-be
- F-IM-14-40ter-familial-belge-be
- F-IM-14-9bis-humanitaire-be
- F-IM-14-9ter-medical-be
- F-IM-17-regime-algerien
- F-IM-19-mineurs
- F-FA-05-partage-immobilier
- F-FA-06-calendrier-garde
- F-FA-16-communaute-universelle
- F-FA-17-partage-judiciaire
- F-FA-18-adoption
- F-FA-19-autorite-parentale
- F-FA-19-changement-residence
- F-FA-19-desaccords-parentaux
- F-FA-20-pacs-dissolution
- F-FA-21-separation-corps
- F-FA-22-indivision
- F-FA-24-devolution-legale
- F-FA-24-donation
- F-FA-24-reserve-heriditaire
- F-FA-24-partage-successoral
- F-FA-24-indivision-successorale
- F-FA-24-rapport-succession
- F-FA-25-majeurs-proteges
- F-FA-26-changement-etat-civil
- F-FA-27-pma-gpa

**Total** : 16 + 19 + 15 + 6 + 35 = **91** entrées (cohérent avec les 90 tool_ids du registry actuel ; tout doute → fallback `Diagnostic situation`).

### Cas d'erreur

- **`tool_id` non mappé** : fallback automatique sur **`Diagnostic situation`** + `console.warn(...)` pour signaler la dette de mapping. Pas d'erreur utilisateur.
- **Aucun outil dans aucun thème** (cas dossier sans analyse + 0 ALWAYS_ON, ex. domaine non Travail FR) : afficher l'état vide existant `<mat-card class="empty-state">` (déjà géré par `isEmpty()` signal).
- **Catalogue vide ET thèmes vides** : `isEmpty()` reste à `true`, état vide affiché.
- **Catalogue non vide ET thèmes vides** : afficher uniquement la section Catalogue (cas extrême peu probable).

---

## Critères d'acceptation vérifiables

1. Le panel affiche **5 sections thématiques** (ou moins si certaines sont vides) au lieu des 2 sections actuelles `Outils principaux` + `Outils contextuels`.
2. L'ordre des thèmes est strictement : Indemnités & calculs → Validité & contestation → Délais & procédure → Documents → Diagnostic situation.
3. Une section thématique vide (0 outil applicable) **n'est pas affichée du tout** (pas de titre, pas de zone vide).
4. Chaque section thématique applique `display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;` sur écran ≥ 1024px.
5. Sur écran < 1024px : `grid-template-columns: 1fr;` (1 colonne, comme aujourd'hui).
6. Les cards conservent leur composant Angular existant (vérifié par DOM : `<ng-container *ngComponentOutlet>` toujours utilisé pour rendu).
7. Le comportement collapse/expand de chaque card reste fonctionnel après refonte.
8. Le **Catalogue** (chips grisés) reste affiché en dernière section, après les 5 thèmes, inchangé.
9. Le mapping `THEME_BY_TOOL_ID` couvre 90/90 tool_ids du `TOOL_REGISTRY` actuel (vérification : aucun `console.warn` "tool_id sans mapping" lors du chargement d'un dossier réel).
10. Tous les tests Jest existants (177 suites, 3642 tests) restent verts.
11. Nouveaux tests Jest sur `decisional-tools-panel.component.spec.ts` :
    - `T-01`: mapping `THEME_BY_TOOL_ID` est exhaustif sur tous les tool_ids du `TOOL_REGISTRY` (test boucle).
    - `T-02`: les outils sont bien classés dans leur thème (échantillon : F-DT-25 → Indemnités, F-DT-08 → Validité, F-DT-03 → Délais, F-DT-04 → Documents, F-IM-05 → Diagnostic).
    - `T-03`: thème vide n'est pas affiché.
    - `T-04`: tool_id non mappé → fallback Diagnostic situation + warn console.
12. Aucune régression visuelle sur les composants enfants (cards) — vérification visuelle staging post-merge.

---

## Plan de test minimal

### Tests unitaires (Jest)

Fichier : `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts`

Adaptations + nouveaux tests :
- **Existants** (suite ~20 tests Jest) : adapter les sélecteurs `'.tool-section'` → `'.theme-section'` si nécessaire ; conserver l'assertion `*ngComponentOutlet` instancié pour chaque outil.
- **T-01** Mapping exhaustif :
  ```typescript
  it('T-01: THEME_BY_TOOL_ID couvre tous les tool_ids du TOOL_REGISTRY', () => {
    const registryIds = Array.from(DecisionToolsPanelComponent.TOOL_REGISTRY.keys());
    const mappedIds = Array.from(DecisionToolsPanelComponent.THEME_BY_TOOL_ID.keys());
    const unmapped = registryIds.filter(id => !mappedIds.includes(id));
    expect(unmapped).toEqual([]);
  });
  ```
- **T-02** Échantillon classement :
  ```typescript
  it('T-02: outils classés dans le bon thème', () => {
    const map = DecisionToolsPanelComponent.THEME_BY_TOOL_ID;
    expect(map.get('F-DT-25-indemnite-preavis')).toBe('INDEMNITES');
    expect(map.get('F-DT-08-licenciement-validity')).toBe('VALIDITE');
    expect(map.get('F-DT-03-prescription-litige')).toBe('DELAIS');
    expect(map.get('F-DT-04-fiche-prudhomale')).toBe('DOCUMENTS');
    expect(map.get('F-IM-05-arbre-decisionnel-titre')).toBe('DIAGNOSTIC');
  });
  ```
- **T-03** Thème vide :
  ```typescript
  it('T-03: thème sans outils n\'est pas rendu', () => {
    component.visibility.set({ alwaysOn: ['F-DT-25-indemnite-preavis'], contextual: [], catalog: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-theme="INDEMNITES"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-theme="VALIDITE"]')).toBeNull();
  });
  ```
- **T-04** Fallback non mappé :
  ```typescript
  it('T-04: tool_id non mappé → Diagnostic + warn console', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation();
    component.visibility.set({ alwaysOn: ['F-INCONNU-99'], contextual: [], catalog: [] });
    fixture.detectChanges();
    expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('F-INCONNU-99'));
    warnSpy.mockRestore();
  });
  ```

### Tests d'intégration / E2E

Aucun test E2E spécifique requis (refonte présentation, contrat backend inchangé). Les smoke tests `e2e/smoke/` ne sont pas concernés.

### Tests d'isolation workspace

Non applicable (composant frontend pur, aucun accès données nouveau).

### Vérification manuelle staging

Sur dossier travail FR avec analyse complète :
- [ ] 5 sections thématiques s'affichent dans l'ordre attendu
- [ ] Grid 2 colonnes visible sur écran ≥ 1024px (visibilité immédiate de 4-6 cards en haut sans scroller)
- [ ] Sur mobile / écran réduit, retour en 1 colonne
- [ ] Toggle expand/collapse fonctionne sur chaque card
- [ ] Catalogue chips reste visible en bas

---

## Tables / endpoints / composants impactés

### Backend
**Aucun.** Contrat `GET /api/v1/case-files/{id}/visible-tools` (qui retourne `alwaysOn` / `contextual` / `catalog`) est inchangé.

### Frontend — composants modifiés

| Fichier | Type de modification |
|---|---|
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.html` | Remplacement des 2 sections `Outils principaux` + `Outils contextuels` par une boucle `@for (theme of orderedThemes; ...)` qui rend les 5 thèmes en grid |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.scss` | Ajout `.theme-section { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }` + media query mobile + adaptation des styles existants |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Ajout `static readonly THEME_BY_TOOL_ID: ReadonlyMap<string, ThemeKey>` (90+ entrées) + ajout `static readonly THEMES_ORDERED` + `computed` qui regroupe `alwaysOn` + `contextual` par thème |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.spec.ts` | Ajout 4 tests (T-01 à T-04) + adaptation sélecteurs si nécessaire |

### Composants **non** impactés
- Tous les composants individuels de cards (`<app-XXX-section>`) restent inchangés.
- `case-file-detail.component` (parent) : utilise toujours `<app-decisional-tools-panel>` avec les mêmes inputs.
- `CaseFileService.getVisibleToolSet()` : inchangé.

### Endpoints / API
**Aucun.**

### Migrations / DB
**Aucune.**

---

## Hors périmètre

- ❌ Bandeau dashboard synthétique avec verdicts agrégés en haut → **F-167**.
- ❌ Section "Documents" (du dossier, pas du panel décisionnel) en accordéon → **F-170**.
- ❌ Refonte des composants individuels (cards eux-mêmes).
- ❌ Navigation drawer/modal vers détail (option C de la discussion 2026-04-27 — reportée).
- ❌ Tri des outils par nom alphabétique dans un thème (ordre conservé du backend).
- ❌ Personnalisation du mapping par utilisateur (ex. drag & drop pour réorganiser).
- ❌ Ajout d'icône par thème (peut être ajouté ultérieurement si demande UX).
- ❌ Modification du `Catalogue` (chips grisés) — reste tel quel.
- ❌ Modification de la logique backend `DecisionToolVisibilityService` (ALWAYS_ON / CONTEXTUAL préservés).

---

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|---|---|---|
| **Tous les outils décisionnels Travail FR + BE** | ✅ intégré dans la SF | 35 tool_ids classés dans le mapping |
| **Tous les outils décisionnels Famille FR + BE** | ✅ intégré dans la SF | 33 tool_ids classés |
| **Tous les outils décisionnels Immigration FR + BE** | ✅ intégré dans la SF | 22 tool_ids classés |
| **Outils transversaux (F-132, F-136)** | ✅ intégré dans la SF | F-132 → Indemnités, F-136 → Délais |
| **Backend `DecisionToolVisibilityService`** | ✅ aucune modification | Contrat ALWAYS_ON / CONTEXTUAL préservé. La SF reorganise uniquement la présentation frontend. |
| **`<app-case-dashboard>` (dashboard agrégé F-IA-02)** | 🟢 backlog F-167 | F-167 enrichira ce dashboard avec les verdicts calculés, indépendamment de F-169. |
| **Section "Documents" sur `case-file-detail`** | 🔵 SF parallèle F-170 | Indépendant de F-169 (le grid concerne le panel décisionnel uniquement). |
| **Synthesis component** | ✅ aucune modification | Son layout interne (faits, risques, timeline) est hors scope F-169. Refonte prévue F-162. |
| **Onboarding / autres pages** | ✅ aucune modification | Le grid est cantonné à `<app-decisional-tools-panel>`. |
| **Tests E2E smoke** | ✅ aucun impact | Aucune préoccupation transversale touchée. |

**Conclusion** : la refonte est cantonnée au composant `<app-decisional-tools-panel>` et son mapping. Aucun effet de bord identifié sur les autres parties de l'app.

---

## Nouveau pattern UI ou service partagé

⚠️ **Oui, partiellement** : la SF introduit le concept de **mapping thème métier** au niveau frontend (constante `THEME_BY_TOOL_ID`) qui est nouveau.

Zones où ce pattern pourrait être réutilisé :
- **F-167 Dashboard agrégé** : pourrait utiliser le même mapping pour grouper ses verdicts par thème (cohérence avec le panel détaillé en dessous). **Décision** : exposer `THEMES_ORDERED` et `THEME_BY_TOOL_ID` comme `static readonly` du `DecisionToolsPanelComponent` pour permettre leur réutilisation par F-167. Si la duplication devient gênante (3+ composants utilisateurs), extraire dans un service `DecisionToolThemeService` ou un fichier `decision-tool-themes.ts` partagé.
- **F-163 Simulateurs autonomes (V8+)** : la page liste des simulateurs hors dossier pourrait aussi grouper par thème — ré-utilisation directe possible.

**Patterns concurrents existants** :
- `decisional-tools-panel.component` actuel a déjà le pattern `Outils principaux / Contextuels / Catalogue` — c'est précisément ce que F-169 remplace côté frontend (le backend conserve la logique technique).
- Aucun autre mapping métier transversal n'existe ailleurs dans le frontend (vérifié par grep `THEME_BY` et `theme:`).

**Action de prévention de la dette de convergence** : la constante `THEMES_ORDERED` + `THEME_BY_TOOL_ID` reste `static readonly` du composant pour cette SF. Si F-167 réutilise effectivement ce mapping, **extraire dans un fichier `frontend/src/app/case-files/decisional-tools-panel/decision-tool-themes.ts`** au moment de F-167 (pas avant — éviter le over-engineering).

---

## Impact par domaine métier

Cette SF est **transversale aux 3 domaines métier (Travail / Famille / Immigration)** et aux **2 pays (France / Belgique)**. Le mapping `THEME_BY_TOOL_ID` couvre explicitement les outils des 3 domaines × 2 pays.

Spécificités par domaine :
- **Droit du travail** : majoritairement répartis entre Indemnités, Validité, Délais, Documents (peu de "Diagnostic situation" pure).
- **Immigration** : majoritairement Diagnostic situation (arbres décisionnels d'orientation), avec Délais (recours OQTF, référés admin) et Documents (checklist pièces).
- **Famille** : équilibre entre Validité (motifs de divorce, paternité), Délais (mesures provisoires, ordonnances), Diagnostic (régimes successoraux, autorité parentale).

**Le mapping est universel** : un outil Travail Indemnité (F-DT-25 préavis) et un outil Famille Indemnité (F-FA-15 récompenses) finissent dans le même thème "Indemnités & calculs". L'avocat voit son dossier sous l'angle métier global, pas par domaine technique.

---

## Préoccupations transversales (anti-régression)

| Préoccupation | Impacté ? | Action |
|---|---|---|
| Auth / Principal | Non | — |
| Workspace context | Non | — |
| Plans / limites | Non | — |
| Navigation / routing | Non | — |
| Outil décisionnel métier | **Oui (présentation uniquement)** | Aucun outil n'est ajouté, modifié, ou retiré. La SF reorganise uniquement leur affichage. **Vérifié** : invariant "1 outil = 1 situation métier" préservé (le mapping thème ne mélange pas plusieurs situations dans un même outil). |

---

## Notes de mise en œuvre

1. Lire `decisional-tools-panel.component.ts` lignes 170-1300 pour récupérer la liste exhaustive des `tool_id` actuels.
2. Implémenter `static readonly THEME_BY_TOOL_ID: ReadonlyMap<string, ThemeKey>` juste après `TOOL_REGISTRY` (même pattern de déclaration).
3. Créer un type `type ThemeKey = 'INDEMNITES' | 'VALIDITE' | 'DELAIS' | 'DOCUMENTS' | 'DIAGNOSTIC';`
4. Créer une constante `static readonly THEMES_ORDERED: { key: ThemeKey; label: string; description?: string }[]` :
   ```typescript
   static readonly THEMES_ORDERED = [
     { key: 'INDEMNITES', label: 'Indemnités & calculs' },
     { key: 'VALIDITE', label: 'Validité & contestation' },
     { key: 'DELAIS', label: 'Délais & procédure' },
     { key: 'DOCUMENTS', label: 'Documents' },
     { key: 'DIAGNOSTIC', label: 'Diagnostic situation' },
   ] as const;
   ```
5. Créer une `computed` `themedTools` qui regroupe `[...alwaysOn, ...contextual]` par thème :
   ```typescript
   readonly themedTools = computed(() => {
     const v = this.visibility();
     if (!v) return new Map<ThemeKey, ResolvedToolEntry[]>();
     const all = [...this.resolvedAlwaysOn(), ...this.resolvedContextual()];
     const byTheme = new Map<ThemeKey, ResolvedToolEntry[]>();
     for (const item of all) {
       const theme = DecisionToolsPanelComponent.THEME_BY_TOOL_ID.get(item.toolId) ?? 'DIAGNOSTIC';
       if (!DecisionToolsPanelComponent.THEME_BY_TOOL_ID.has(item.toolId)) {
         console.warn(`[F-169] tool_id sans mapping thème : ${item.toolId} → fallback DIAGNOSTIC`);
       }
       const list = byTheme.get(theme) ?? [];
       list.push(item);
       byTheme.set(theme, list);
     }
     return byTheme;
   });
   ```
6. Dans le HTML, remplacer le bloc `@if (resolvedAlwaysOn().length > 0) { ... }` + `@if (resolvedContextual().length > 0) { ... }` par :
   ```html
   @for (theme of themesOrdered; track theme.key) {
     @if (themedTools().get(theme.key)?.length) {
       <section class="theme-section" [attr.data-theme]="theme.key">
         <h2 class="theme-title">{{ theme.label }}</h2>
         <div class="theme-grid">
           @for (item of themedTools().get(theme.key)!; track item.toolId) {
             <ng-container *ngComponentOutlet="item.entry.component; inputs: componentInputsFor(item.entry)"></ng-container>
           }
         </div>
       </section>
     }
   }
   ```
7. SCSS : ajouter `.theme-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }` et `@media (max-width: 1023px) { .theme-grid { grid-template-columns: 1fr; } }`.
8. Conserver le bloc `Catalogue` tel quel après la boucle des thèmes.
9. Build de validation : `cd frontend && npx ng build --configuration=staging` doit passer.
10. Vérifier qu'aucun composant rendu via `*ngComponentOutlet` n'a un comportement spécifique au layout vertical (largeur fixe en `px` qui empêche le grid de fonctionner). Audit rapide : `grep -rn "width: [0-9]\+px" frontend/src/app/case-files/*-section/` — s'il y a des largeurs fixes, les passer à `width: 100%` ou les laisser et accepter le rendering.
