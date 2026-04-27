# SF-170-01 — Section "Documents" repliable en accordéon sur la page dossier

**Feature parente** : F-170 — Section Documents en accordéon
**Type** : frontend
**Branche** : `feat/SF-170-01-documents-accordeon`
**Effort estimé** : ~30-45 min

---

## Objectif (1 phrase)

Transformer la section "Documents" affichée en permanence sur `case-file-detail.component.html` (ligne 94 actuellement, `<div id="section-documents" class="section-header">`) en **accordéon repliable** au pattern canonique `td-section`/`td-header`/`td-title`/`td-body` (cohérent avec F-168), avec **état persisté en `sessionStorage`** (clé `case-file-{caseFileId}-docs-collapsed`), afin que l'avocat puisse replier la liste sur un dossier riche pour gagner de l'espace vertical.

---

## Contexte

Aujourd'hui, sur `case-file-detail.component.html` :
- Ligne 94-101 : bandeau "Documents" + bouton "Ajouter des documents"
- Ligne 104-126 : bandeau retry OCR (conditionnel)
- Ligne 128-191 : liste des fichiers pending + options OCR (conditionnel)
- Ligne 193-202 : bandeau Vision (conditionnel)
- Ligne 204-329 : table `<mat-card class="docs-card">` ou état vide

Sur les dossiers riches (10+ documents) la liste pousse les autres sections (Analyse, Synthèse, Outils décisionnels) bien plus bas — l'avocat doit scroller longtemps pour atteindre le panel décisionnel après chaque rechargement.

L'accordéon permet :
- Affichage par défaut **déplié** (premier accès au dossier)
- Repli manuel à un clic sur le header
- Persistance du choix en `sessionStorage` (par dossier) — l'avocat retrouve son état au switch d'onglet ou à la navigation
- Réinitialisation au prochain login (pas localStorage car non persistant cross-session)

---

## Comportement nominal

1. Au chargement, le composant lit `sessionStorage.getItem('case-file-{caseFileId}-docs-collapsed')` :
   - `null` ou `'false'` → `docsCollapsed.set(false)` (déplié, comportement par défaut)
   - `'true'` → `docsCollapsed.set(true)` (replié)
2. La section est wrappée dans `<section class="td-section">` + `<header class="td-header">` cliquable.
3. Le header affiche :
   - Icône `<mat-icon class="td-icon">folder</mat-icon>`
   - Titre `<span class="td-title">DOCUMENTS</span>`
   - Badge `<span class="td-chip">{{ documents().length }} document(s)</span>` (visible quand `documents().length > 0`)
   - Bouton "Ajouter des documents" — **conservé dans le header**, à droite du chip, **avec `(click)="$event.stopPropagation()"`** pour ne pas déclencher le toggle
   - Chevron `<mat-icon class="td-toggle">{{ collapsed ? 'expand_more' : 'expand_less' }}</mat-icon>`
4. Au clic sur le header (hors bouton "Ajouter") OU `Entrée` au focus clavier : toggle `docsCollapsed` + écriture sessionStorage.
5. Si `docsCollapsed()` est `true` : tout le body est masqué (bandeau OCR retry, pending files, Vision, table). Si `false` : tout le body est visible (comportement actuel préservé).
6. **L'`id="section-documents"`** existant (utilisé par certains scripts/tour spotlights) est conservé sur l'élément `<section>`.

### Cas d'erreur

- **`sessionStorage` indisponible** (navigateur très restrictif, mode incognito strict) : le `try / catch` autour des appels à sessionStorage fait fail-silent ; l'état n'est juste pas persisté entre rechargements. Pas de message d'erreur.
- **`caseFileId` indéfini au moment du `ngOnInit`** : on ne lit pas sessionStorage. Le déplié-par-défaut s'applique. Au prochain `ngOnChanges` avec un `caseFileId` valide, on lit l'état.
- **Aucun document** (`documents().length === 0`) : le badge n'est pas affiché (cohérent avec le pattern `td-chip` conditionnel). L'accordéon reste fonctionnel et affiche l'état vide à l'intérieur du body.

---

## Critères d'acceptation vérifiables

1. La section "Documents" est wrappée dans un `<section class="td-section" id="section-documents">` (pas un `<div>`).
2. Le header `<header class="td-header">` est cliquable et au focus avec `tabindex="0"`, `role="button"`, `[attr.aria-expanded]="!docsCollapsed()"`, `aria-controls="section-documents-body"`.
3. Le titre est `<span class="td-title">DOCUMENTS</span>` (MAJUSCULES dans le HTML, `font-weight: 700` via SCSS canonique).
4. Le badge `<span class="td-chip">{{ documents().length }} document(s)</span>` est affiché si et seulement si `documents().length > 0`.
5. Le bouton "Ajouter des documents" reste dans le header avec `(click)="triggerUpload(); $event.stopPropagation()"` — un clic dessus ne déclenche pas le toggle.
6. Quand `docsCollapsed() === true` : aucun élément du body n'est rendu (bandeau OCR retry, pending files, Vision banner, table OU empty state).
7. Quand `docsCollapsed() === false` : le body est rendu intégralement comme avant la SF (comportement actuel préservé).
8. Le clic sur le header (hors bouton) toggle `docsCollapsed`.
9. La touche `Entrée` au focus du header toggle aussi `docsCollapsed` (`(keydown.enter)="toggleDocsCollapsed()"`).
10. Au toggle, `sessionStorage.setItem('case-file-{caseFileId}-docs-collapsed', String(value))` est appelé (avec try/catch silencieux).
11. Au `ngOnInit` avec `caseFileId` valide, `sessionStorage.getItem(...)` est appelé et l'état appliqué (`'true'` → replié, sinon déplié).
12. Tous les tests Jest existants de `case-file-detail.component.spec.ts` restent verts.
13. Nouveaux tests Jest :
    - `T-01` : `docsCollapsed` est `false` par défaut quand sessionStorage est vide.
    - `T-02` : `docsCollapsed` est `true` quand sessionStorage contient `'true'` au `ngOnInit`.
    - `T-03` : `toggleDocsCollapsed()` bascule la valeur ET écrit dans sessionStorage.
    - `T-04` : `sessionStorage` indisponible (mock throw) → pas de crash, fail-silent.
14. Aucune régression sur les autres sections de la page (Analyse, Synthèse, Outils décisionnels).
15. Le panel des outils décisionnels (sous Documents) reste rendu normalement.

---

## Plan de test minimal

### Tests unitaires (Jest)

Fichier : `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts`

**T-01 Default expanded** :
```typescript
it('SF-170-01 T-01: docsCollapsed est false par défaut quand sessionStorage est vide', () => {
  sessionStorage.clear();
  fixture.detectChanges();
  expect(component.docsCollapsed()).toBe(false);
});
```

**T-02 Restaure depuis sessionStorage** :
```typescript
it('SF-170-01 T-02: docsCollapsed=true si sessionStorage contient "true"', () => {
  sessionStorage.setItem(`case-file-${component.caseFileId}-docs-collapsed`, 'true');
  component.ngOnInit();
  expect(component.docsCollapsed()).toBe(true);
});
```

**T-03 Toggle persiste** :
```typescript
it('SF-170-01 T-03: toggleDocsCollapsed() bascule + écrit sessionStorage', () => {
  sessionStorage.clear();
  expect(component.docsCollapsed()).toBe(false);
  component.toggleDocsCollapsed();
  expect(component.docsCollapsed()).toBe(true);
  expect(sessionStorage.getItem(`case-file-${component.caseFileId}-docs-collapsed`))
    .toBe('true');
  component.toggleDocsCollapsed();
  expect(component.docsCollapsed()).toBe(false);
  expect(sessionStorage.getItem(`case-file-${component.caseFileId}-docs-collapsed`))
    .toBe('false');
});
```

**T-04 SessionStorage indisponible fail-silent** :
```typescript
it('SF-170-01 T-04: sessionStorage indisponible — pas de crash', () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
    throw new Error('SecurityError');
  });
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
    throw new Error('SecurityError');
  });
  expect(() => component.ngOnInit()).not.toThrow();
  expect(() => component.toggleDocsCollapsed()).not.toThrow();
  expect(component.docsCollapsed()).toBe(true); // toggle malgré l'échec d'écriture
});
```

### Tests d'intégration / E2E

Aucun test E2E spécifique requis. Les smoke tests existants ne sont pas concernés.

### Tests d'isolation workspace

Non applicable (pas d'accès données nouveau, sessionStorage côté client).

### Vérification manuelle staging

- [ ] Sur dossier avec 0 document : header accordéon visible, pas de badge, body affiche état vide.
- [ ] Sur dossier avec ≥ 1 document : header avec badge "N document(s)", body affiche la table.
- [ ] Clic sur le header → repli ; clic à nouveau → dépli.
- [ ] Clic sur "Ajouter des documents" dans le header replié → ouvre le file picker SANS déplier la section (`stopPropagation`).
- [ ] Repli + reload (F5) → la section reste repliée (sessionStorage).
- [ ] Repli + ouvrir le dossier dans un nouvel onglet → state initial déplié (sessionStorage est par onglet).
- [ ] Logout / login → state initial déplié (sessionStorage perdu après fermeture session).

---

## Tables / endpoints / composants impactés

### Backend
**Aucun.**

### Frontend — composants modifiés

| Fichier | Type de modification |
|---|---|
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.html` | Refactor de la section Documents (lignes 94-329) en `td-section`/`td-header`/`td-body` ; titre MAJUSCULES ; bouton "Ajouter" garde son emplacement avec `stopPropagation` ; ajout `@if (!docsCollapsed())` autour du body |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.ts` | Ajout `readonly docsCollapsed = signal(false);` + méthode `toggleDocsCollapsed()` + lecture `sessionStorage` au `ngOnInit` (avec try/catch) + clé `case-file-{caseFileId}-docs-collapsed` |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.scss` | Ajout classes `.td-section`, `.td-header`, `.td-icon`, `.td-title`, `.td-chip`, `.td-toggle`, `.td-body` (alignées sur le pattern canonique) ; conserver styles existants pour bandeau OCR / pending / Vision / table |
| `frontend/src/app/case-files/case-file-detail/case-file-detail.component.spec.ts` | Ajout 4 tests T-01 à T-04 |

### Composants **non** impactés
- Tous les autres composants de `case-file-detail` (Analyse, Synthèse, panel décisionnel, etc.).
- Page Settings, Pricing, etc.

### Endpoints / API
**Aucun.**

### Migrations / DB
**Aucune.**

---

## Hors périmètre

- ❌ Refonte de la table des documents (colonnes, tri, pagination) → reste tel quel.
- ❌ Comportement upload, OCR, Vision → préservés intégralement.
- ❌ Persistance cross-session (`localStorage`) — décision : sessionStorage uniquement (l'avocat retrouve son contexte dans la session courante mais pas après logout).
- ❌ Repli synchronisé entre onglets — par design, chaque onglet a son sessionStorage.
- ❌ Animation d'ouverture/fermeture (transition CSS) — comportement instantané comme les cards `td-section` existantes.
- ❌ Repli automatique au-delà d'un nombre de documents — toujours déplié par défaut.
- ❌ Repli des autres sections de la page (Analyse, Synthèse, etc.) — uniquement Documents dans cette SF.
- ❌ Modification du panel décisionnel — déjà en accordéon par card individuelle, pas de wrapping global.

---

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|---|---|---|
| **Section "Documents" sur `case-file-detail`** | ✅ intégré dans la SF | Le seul cas applicable. |
| **Section "Analyse"** sur `case-file-detail` | 🟢 backlog éventuel | Refonte écran synthèse prévue F-162 (V8+). Pas de besoin immédiat de repli. |
| **Section "Synthèse"** | 🟢 backlog F-162 | Idem. |
| **Section "Outils décisionnels"** (panel F-IA-04) | ✅ aucune action | Chaque card est déjà repliable individuellement (template canonique `td-section`) ; le panel parent F-169 vient d'être livré avec grid 2 colonnes par thème, pas de wrapping accordéon global nécessaire. |
| **Pattern `td-section` canonique** | ✅ rejoint | Cohérent avec F-168 (4 cards décisionnelles) et le pattern travail-dissimule-section. |
| **Persistance sessionStorage** | ✅ pattern aligné | Le projet utilise déjà `sessionStorage` ailleurs (cf. tour spotlight). Pas de nouveau service de persistance créé. |
| **Tests E2E smoke** | ✅ aucun impact | Pas de préoccupation transversale. Le bouton "Ajouter des documents" reste fonctionnel (le `stopPropagation` est testé). |

**Conclusion** : la SF est cantonnée à `case-file-detail.component.html` (zone Documents) et son TS/SCSS associés.

---

## Nouveau pattern UI ou service partagé

❌ **Non**. La SF rejoint le pattern canonique `td-section` (existant) et utilise `sessionStorage` directement (pas de service abstrait nouveau). La duplication des classes SCSS `.td-*` reste cantonnée au composant (cohérent avec les 4 composants migrés en F-168 et les composants canoniques préexistants).

Si la duplication SCSS devient gênante (>15 composants utilisateurs), une SF d'extraction en mixin SCSS partagé pourrait suivre — hors scope ici.

---

## Impact par domaine métier

Cette SF est **transversale** : la page `case-file-detail` est utilisée pour les 3 domaines métier (Travail / Famille / Immigration) et les 2 pays (FR / BE). Aucune logique métier touchée. Le comportement est identique quel que soit le domaine.

---

## Préoccupations transversales (anti-régression)

| Préoccupation | Impacté ? | Action |
|---|---|---|
| Auth / Principal | Non | — |
| Workspace context | Non | — |
| Plans / limites | Non | — |
| Navigation / routing | Non | — |
| Outil décisionnel métier | Non | Aucun outil touché. La section Documents n'est pas un outil décisionnel. |

---

## Notes de mise en œuvre

1. Lire les lignes 94-329 de `case-file-detail.component.html` pour la structure complète.
2. Wrapper le tout dans `<section class="td-section" id="section-documents">`.
3. Header :
   ```html
   <header class="td-header"
           (click)="toggleDocsCollapsed()"
           (keydown.enter)="toggleDocsCollapsed()"
           tabindex="0" role="button"
           [attr.aria-expanded]="!docsCollapsed()"
           aria-controls="section-documents-body">
     <mat-icon class="td-icon">folder</mat-icon>
     <span class="td-title">DOCUMENTS</span>
     @if (documents().length > 0) {
       <span class="td-chip">{{ documents().length }} document{{ documents().length > 1 ? 's' : '' }}</span>
     }
     <input #fileInput type="file" accept=".pdf,.doc,.docx,.txt" multiple hidden (change)="onFileSelected($event)">
     <button mat-raised-button color="primary"
             data-tour-target="upload-trigger-btn"
             (click)="triggerUpload(); $event.stopPropagation()"
             [disabled]="!canUpload()">
       <mat-icon>add</mat-icon>
       Ajouter des documents
     </button>
     <mat-icon class="td-toggle">{{ docsCollapsed() ? 'expand_more' : 'expand_less' }}</mat-icon>
   </header>
   @if (!docsCollapsed()) {
     <div id="section-documents-body" class="td-body">
       <!-- bandeau OCR retry + pending + Vision + table OU empty -->
     </div>
   }
   ```
4. TS :
   ```typescript
   readonly docsCollapsed = signal(false);

   private docsCollapsedKey(): string {
     return `case-file-${this.caseFileId}-docs-collapsed`;
   }

   private restoreDocsCollapsedFromSession(): void {
     try {
       const v = sessionStorage.getItem(this.docsCollapsedKey());
       this.docsCollapsed.set(v === 'true');
     } catch {
       // sessionStorage indisponible (incognito strict) — fail-silent.
     }
   }

   toggleDocsCollapsed(): void {
     const next = !this.docsCollapsed();
     this.docsCollapsed.set(next);
     try {
       sessionStorage.setItem(this.docsCollapsedKey(), String(next));
     } catch {
       // fail-silent
     }
   }
   ```
5. Dans `ngOnInit` : appeler `this.restoreDocsCollapsedFromSession()` après que `caseFileId` est défini.
6. Dans `ngOnChanges` : si `caseFileId` change (cas peu probable mais possible), réappeler `restoreDocsCollapsedFromSession`.
7. Build de validation : `cd frontend && npx ng build --configuration=staging` doit passer sans warning nouveau.
