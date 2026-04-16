# Mini-spec — F-118 / SF-118-06 Retirer les résidus "IA" user-facing

## Identifiant

`F-118 / SF-118-06`

## Feature parente

`F-118` — Refonte visuelle / polish UX

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-118-06-terminologie-ia-residus`

---

## Objectif

Retirer les dernières occurrences user-facing du terme "IA" / "l'IA a détecté" / "Analyse IA" / "Pré-rempli depuis l'analyse IA" dans les écrans fonctionnels, en complément de SF-118-05 qui avait unifié la terminologie principale.

La landing page publique et les meta tags SEO restent **hors scope** — la mention "IA" y est volontaire (description produit marketing).

---

## Comportement attendu

### Nominal

Remplacement de ~45 occurrences user-facing fonctionnelles selon le mapping suivant :

| Occurrence actuelle | Remplacement |
|---|---|
| "Analyse IA" (sauf landing) | "Analyse" ou "Analyse du dossier" selon contexte |
| "L'IA a détecté : X" / "L'IA a détecté le mode \"X\"…" | "Détecté : X" / "Mode détecté : \"X\"…" |
| "Pré-rempli(e) depuis l'analyse IA" | "Pré-rempli(e) depuis l'analyse" |
| "Importer depuis l'analyse IA" | "Importer depuis l'analyse" |
| "Raison IA :" | "Motif détecté :" |
| "Propositions IA" + badge "IA" | "Propositions suggérées" (badge retiré) |
| "Génération des questions IA…" | "Génération des questions complémentaires…" |
| "question(s) IA en attente de réponse" | "question(s) complémentaire(s) en attente de réponse" |
| "données extraites par l'IA" | "données extraites" |
| "mise(s) à jour suggérée(s) par l'IA" | "mise(s) à jour suggérée(s)" |
| "L'IA a détecté une possible divergence" | "Divergence détectée" |
| "Pièce manquante signalée par l'IA" | "Pièce manquante signalée" |
| "assistant juridique IA" (onboarding wizard, tour) | "assistant juridique" |
| "Risque global IA" (dashboard) | "Évaluation des risques" |

### Zones concernées

**In scope** (18 fichiers environ) :
- `auth/login/login.component.html`
- `case-files/analysis-diff/analysis-diff.component.html` (6 "Raison IA")
- `case-files/case-dashboard/case-dashboard.component.html`
- `case-files/case-deadlines-section/case-deadlines-section.component.html`
- `case-files/case-file-detail/case-file-detail.component.html` (2 occurrences)
- `case-files/immigration-recours-section/*.html` (2)
- `case-files/immigration-work-right-section/*.html`
- `case-files/partage-immobilier-section/*.html`
- `case-files/synthesis/synthesis.component.html` (3)
- `referentials/referentials.component.html`
- `referentials/referential-warning-dialog/*.html`
- Composants TS avec texte dynamique : `anciennete-section.component.ts:123`, `calendrier-garde-section.component.ts:287`, `divorce-checklist-section.component.ts:218`, `indemnite-comparatif-section.component.ts:249,409`
- Onboarding / tour : `onboarding-wizard-dialog.component.ts` (2), `tour-overlay.component.ts` (2)

**Hors scope** (volontairement conservé) :
- `landing/landing.component.html` et `landing.component.ts` meta tags — mention produit marketing, décision produit antérieure. Peut être rouverte dans une SF marketing dédiée.
- Noms de variables/services TypeScript (`aiData`, `aiQuestions`, `aiDataSignal`) — technique, pas visible utilisateur.
- Documentation interne, commentaires de code.

### Cas d'erreur

Aucun — c'est du remplacement textuel pur, pas d'impact logique.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10 outils décisionnels — les textes à remplacer y sont, traités dans cette SF.
- [x] **Autres pays** : FR + BE — les remplacements sont en français, couvrent tous les dossiers.
- [x] **Autres domaines** : Travail, Famille, Immigration — couverts par les 18 fichiers impactés.
- [x] **Autres UI patterns** : tooltips, badges provenance, sections messages. Pas de nouveau pattern introduit.
- [x] **Autres flows transversaux** : aucun (pas d'auth/workspace/plans/navigation).

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — aucun nouvel outil créé.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun composant/service/endpoint/directive/DTO créé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 18 fichiers user-facing | Oui | **Intégré** dans cette SF |
| Landing page + meta SEO | Oui | **Hors scope** (marketing/produit) |
| Variables TS techniques | Non | Non user-facing |

### Décision

- [x] Étendu à toutes les cibles user-facing applicables
- [x] Landing + SEO explicitement exclus (justifié)

---

## Critères d'acceptation

- [ ] Aucune occurrence de "Analyse IA" dans les templates fonctionnels (hors landing).
- [ ] Aucune occurrence de "L'IA a détecté" / "Raison IA" / "par l'IA" dans les templates fonctionnels.
- [ ] Aucune occurrence de "Pré-rempli depuis l'analyse IA" — remplacée par "Pré-rempli depuis l'analyse".
- [ ] Badges "IA" sur les délais (`case-deadlines-section`) retirés ou renommés "Suggéré".
- [ ] Tous les specs existants passent sans modification (le remplacement est purement textuel).
- [ ] Vérification `grep -r "IA\|l'IA\|Analyse IA"` sur les templates hors landing → aucun hit fonctionnel.
- [ ] Build frontend vert.

---

## Périmètre

### Hors scope

- Landing page et meta tags SEO.
- Renommage des variables TS (`aiData`, `aiQuestions`).
- Modifications backend (aucun texte backend user-facing impacté).
- Ajout de nouvelles fonctionnalités.

---

## Contraintes

Aucune (pur remplacement textuel).

---

## Technique

### Endpoints

Aucun changement.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular modifiés

~18 composants (listés ci-dessus). Remplacement textuel uniquement.

---

## Plan de test

### Tests frontend

- [ ] Non-régression : les 974 specs existants doivent rester verts sans modification (sauf si un test vérifie littéralement un ancien texte "IA" — dans ce cas adapter le spec avec le nouveau texte).
- [ ] Build frontend vert.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| 18 templates + composants TS | Texte affiché modifié |

### Smoke tests E2E

- [x] Aucun (pas de changement de navigation ni d'auth).

---

## Dépendances

### Subfeatures bloquantes

- `SF-118-05 Done` — unification de la majorité de la terminologie.

### Questions ouvertes

- [x] Aucune.

---

## Notes et décisions

- **Pourquoi séparer en SF** (et ne pas l'intégrer dans SF-118-05 à l'époque) : SF-118-05 a retiré les occurrences principales, ce sont des oublis mineurs dispersés. Les traiter en une SF dédiée donne une traçabilité explicite et force un scan complet.
- **Landing hors scope** : la mention "IA" sur la landing est une décision produit (positionnement marketing, SEO). Si cette décision doit évoluer, elle sera traitée dans une SF marketing séparée.
- **Pas de reformulation contextuelle complexe** : le remplacement est 1:1 sur des clés identifiées. Un reviewer peut vérifier chaque remplacement facilement.
