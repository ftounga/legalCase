# Mini-spec — F-177 / SF-177-10 Polish SCSS legacy 4 composants immigration

## Identifiant

`F-177 / SF-177-10`

## Feature parente

`F-177` — Refonte panel F-IA-04 (cards verdict synthétique + ouverture modal). Cette SF absorbe **F-168 bis** (uniformisation visuelle des composants legacy).

## Statut

`draft`

## Date de création

2026-05-01

## Branche Git

`feat/SF-177-10-polish-scss-legacy`

---

## Objectif

Supprimer le `margin-top: 32px;` racine des 4 composants legacy immigration (`recours-section`, `checklist-section`, `title-decision-section`, `work-right-section`) — vestige du rendu inline pré-SF-177-11, devenu inutile maintenant que ces composants ne s'affichent plus que dans le `MatDialog` de SF-177-02.

---

## Position dans le découpage F-177

Dernière SF (10/10) du plan F-177 — pure hygiène. Les 9 autres SF couvrent l'infrastructure (01/02), l'instrumentation transversale (03/03b/05/07), la bascule (11) et le dashboard agrégé (09 — à venir).

---

## Constat technique

Les 4 composants legacy ont une racine `<div class="xxx-section">` avec une seule règle SCSS :

```scss
.xxx-section {
  margin-top: 32px;
}
```

| Composant | Selector | Root tag |
|-----------|----------|---------|
| `immigration-recours-section` | `app-immigration-recours-section` | `<div class="recours-section">` |
| `immigration-checklist-section` | `app-immigration-checklist-section` | `<div class="checklist-section">` |
| `immigration-title-decision-section` | `app-immigration-title-decision-section` | `<div class="title-decision-section">` |
| `immigration-work-right-section` | `app-immigration-work-right-section` | `<div class="work-right-section">` |

Pré-SF-177-11, ce `margin-top: 32px;` séparait visuellement chaque section du panel inline. Post-bascule, ces composants ne sont rendus **que dans le `MatDialog`** ouvert par `DecisionToolModalService.open()` — qui possède son propre cadrage (90vw/90vh, `mat-dialog-content` avec padding 24px Material default). Le `margin-top: 32px;` ajoute un espace blanc gratuit en haut du contenu, pas symétrique avec les autres composants (qui n'en ont pas).

Vérification : `grep -rn "app-immigration-recours-section|app-immigration-checklist-section|app-immigration-title-decision-section|app-immigration-work-right-section" frontend/src/app/` → seulement les 4 selectors eux-mêmes (aucun autre consommateur du sélecteur). Donc pas de risque de régression sur un autre point d'usage.

---

## Comportement attendu

### Cas nominal

1. Le SCSS racine de chaque composant legacy ne contient plus `margin-top: 32px;` — seulement les règles internes (header, body, sub-elements).
2. Au runtime, l'ouverture d'un de ces 4 composants dans le modal affiche le contenu en haut du `mat-dialog-content` sans 32 px de blanc parasite.
3. Les autres règles SCSS (header, badges, body, etc.) sont **inchangées** — pas de refactor au-delà de la racine.
4. Le markup HTML (`<div class="xxx-section">`) est **inchangé** — pas de migration `<div>` → `<section>` dans cette SF (hors scope, voir §Périmètre).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Régression visuelle dans le modal | Vérification manuelle pré-merge sur 1 dossier immigration ; rollback du SCSS si l'apparence est dégradée |
| Composant rendu en dehors du modal (cas non-existant aujourd'hui) | Pas de risque immédiat — aucun consommateur identifié ; si un futur usage inline réapparaît, il devra fournir son propre espacement parent |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : SF-177-10 ne touche que les 4 composants explicitement listés. Les ~92 autres composants ont déjà leur propre cadrage (`<section>` + border + background) et n'ont rien à harmoniser ici.
- [ ] **Autres pays** : non applicable (les 4 sont tous immigration FR — symétrique BE inexistante côté legacy)
- [ ] **Autres domaines** : non applicable (4 composants tous immigration)
- [x] **Autres UI patterns** : pas de nouveau pattern UI introduit. Pure suppression de règle obsolète.
- [ ] **Autres flows transversaux** : non concerné

### Niveaux de vérification

- [ ] **Modèle TypeScript** : non touché
- [ ] **Record / DTO backend** : non applicable
- [ ] **Service / logique métier** : non touché
- [ ] **Entité JPA + schéma DB** : non applicable
- [x] **Tests existants** : tests Jest des 4 composants vérifient le comportement (collapsed, click, render) sans assertion sur le margin-top → pas d'impact

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — pas de nouveau pattern.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 4 composants legacy listés | Oui | Cleanup margin-top racine |
| ~92 autres composants outils | Non | Déjà conformes (pattern canonique `<section>` + frame) |
| Composants outils hors panel (case-notes, dashboard, …) | Non | N/A — pas concernés par F-168 bis |

### Décision

- [x] Étendu à toutes les cibles applicables (4/4)
- [ ] SF parallèle : aucune
- [ ] Backlog VN : aucune
- [ ] Non applicable aux autres cibles

---

## Critères d'acceptation

- [ ] `immigration-recours-section.component.scss` ligne 1-3 : `.recours-section {}` ne contient plus `margin-top: 32px;` (suppression de la règle racine, ou bloc vidé). Commentaire `// F-177 SF-177-10 …` justifie l'absence si bloc vide gardé.
- [ ] Idem pour `immigration-checklist-section.component.scss`
- [ ] Idem pour `immigration-title-decision-section.component.scss`
- [ ] Idem pour `immigration-work-right-section.component.scss`
- [ ] Aucune autre règle SCSS modifiée dans les 4 fichiers
- [ ] Templates HTML inchangés
- [ ] TypeScript inchangé
- [ ] Tests Jest des 4 composants restent verts
- [ ] Suite Jest complète verte (≥ 3959 tests)
- [ ] Build Angular vert
- [ ] Vérification manuelle (post-merge staging) : 1 dossier immigration ouvert, 1 outil legacy ouvert dans le modal → pas de blanc parasite en haut du contenu

---

## Périmètre

### Hors scope (explicite)

- **Migration `<div>` → `<section>` racine** : changement sémantique potentiellement utile pour A11y (rôle `region`), mais hors scope SF-177-10 qui se limite au cleanup du symptôme F-168 bis. À ouvrir comme tâche distincte si jugé prioritaire.
- **Renommage des classes root** (`.recours-section` → `.td-section`-like) : aucune valeur ajoutée tant que les composants sont seulement consommés via le modal.
- **Ajout d'un cadrage canonique** (border + background) sur la racine : redondant avec le cadre du modal — créerait un double cadre.
- **Audit des 92 autres composants** : tous déjà conformes selon SF-168, pas besoin de re-vérifier ici.
- **Mise à jour design system** : pas d'ajout de variables CSS, pas de changement DS.

---

## Valeurs initiales

Non applicable.

---

## Contraintes de validation

Non applicable (cleanup SCSS pur).

---

## Technique

### Endpoint(s)

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- 4 fichiers SCSS modifiés :
  - `frontend/src/app/case-files/immigration-recours-section/immigration-recours-section.component.scss`
  - `frontend/src/app/case-files/immigration-checklist-section/immigration-checklist-section.component.scss`
  - `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.scss`
  - `frontend/src/app/case-files/immigration-work-right-section/immigration-work-right-section.component.scss`

---

## Plan de test

### Tests unitaires

- [x] Tests Jest existants des 4 composants couvrent collapsed/render/click — pas d'assertion sur la marge racine, donc verts par construction. Pas de test ajouté.

### Tests d'intégration

Non applicable (pas de backend).

### Isolation workspace

Non applicable.

### Vérification visuelle (post-merge)

- [ ] Ouvrir un dossier immigration sur staging, ouvrir l'outil "RECOURS IMMIGRATION" dans le modal → contenu collé en haut du dialog (pas de blanc 32 px)
- [ ] Idem pour "CHECKLIST PIÈCES IMMIGRATION", "TITRE DE SÉJOUR RECOMMANDÉ", "DROIT AU TRAVAIL"

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing frontend — non
- [x] **Aucune préoccupation transversale** — pure hygiène SCSS

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| 4 composants legacy listés | Apparence dans le modal légèrement modifiée (suppression marge top) | Vérification visuelle pré-PR ou post-merge staging |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (cleanup SCSS isolé)

---

## Impact par domaine métier

Cette SF est **spécifique à 4 composants immigration FR**. Elle ne touche ni Travail, ni Famille, ni Belgique. C'est volontaire — les autres composants n'ont pas le pattern legacy (pas de `<div class="xxx-section">` avec uniquement `margin-top:32px;` à racine).

---

## Dépendances

### Subfeatures bloquantes

- [x] **SF-177-11** — Bascule panel cards + modal — done (PR #729 mergée). Sans la bascule, supprimer le `margin-top: 32px;` aurait cassé le layout panel inline. Avec, la marge est obsolète.

### Subfeatures débloquées

- F-177 sera complète au merge de SF-177-09 (dashboard agrégé). SF-177-10 ne débloque rien d'autre.

### Questions ouvertes impactées

- [x] Aucune

---

## Notes et décisions

- **Pourquoi pas migrer `<div>` → `<section>` ?** Bénéfice A11y marginal, risque de casser des sélecteurs CSS internes (`.recours-section .section-header`) si on ne fait pas attention. Garde l'incrément minimal.
- **Pourquoi pas ajouter border + background canoniques ?** Le cadre du `MatDialog` (mat-dialog-content + ses paddings) sert déjà de frame. Ajouter une 2e couche créerait un double cadre.
- **Pourquoi le `margin-top: 32px;` n'avait pas été retiré dans SF-177-11 ?** SF-177-11 a la bascule du panel, mais conserve volontairement les composants outils inchangés (modification ciblée). SF-177-10 est le suivi explicite pour les artefacts SCSS désormais inutiles.
