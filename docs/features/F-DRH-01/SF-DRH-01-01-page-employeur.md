# Mini-spec — F-DRH-01 / SF-DRH-01-01 — Page publique « LegalCase Employeur »

> Validée AVANT dev. Étapes 0 (`SF-DRH-01-00-coherence.md`) et 0 bis (`SF-DRH-01-00b-ux-coherence.md`) rendues GO avec ajustements.

---

## Identifiant

`F-DRH-01 / SF-DRH-01-01`

## Feature parente

`F-DRH-01` — Page publique « LegalCase Employeur » (couche test du pivot DRH)

## Statut

`ready`

## Date de création

2026-06-08

## Branche Git

`feat/F-DRH-01-page-employeur`

---

## Objectif

> En une phrase : livrer une page publique `/employeur` qui présente l'offre LegalCase côté employeur/DRH (chiffrer l'exposition prud'homale, sécuriser la procédure) et oriente le prospect vers une démo Calendly — sans prix, sans inscription self-serve.

---

## Comportement attendu

### Cas nominal

1. Un visiteur (DRH/prospect) ouvre `/employeur` (route publique, no-auth, lazy-loaded).
2. La page affiche **4 blocs primaires** dans l'ordre : **hero → valeur → confiance → CTA final**.
3. Le **hero** porte le titre « Chiffrez l'exposition prud'homale d'un licenciement — avant de décider. », un sous-titre orienté maîtrise du risque & conformité, et un CTA primaire **« Réserver une démo »** (lien Calendly externe).
4. La **section valeur** liste **4 capacités réelles** : qualification du litige, détection des vices de procédure, chiffrage de l'exposition (barème Macron), signalement des délais — à partir des pièces du dossier.
5. La **section confiance** affiche hébergement EU, RGPD, données isolées, sous le cadre « maîtrise du risque & conformité ».
6. Le **CTA final** répète « Réserver une démo » (même URL Calendly).
7. Les clics sur les CTA ouvrent `https://calendly.com/tounga-franck-ng-itconsulting/30min` dans un nouvel onglet (`target="_blank" rel="noopener noreferrer"`).
8. Le `<title>` et les balises meta (description, OG, canonical) sont positionnés à l'init (pattern `/demos`).

### Cas d'erreur

> Page statique publique sans appel backend ni formulaire : pas de cas d'erreur applicatif. Le seul comportement dégradé possible est l'indisponibilité de Calendly (externe), hors périmètre.

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Route inconnue voisine (faute de frappe) | wildcard `**` → `NotFoundComponent` (existant) | — |
| Calendly indisponible (service tiers) | hors périmètre (le lien reste cliquable, dégradation côté tiers) | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable — aucune logique d'outil décisionnel, page marketing statique.
- [x] **Autres pays** : la page parle du droit français (barème Macron, prud'hommes). BE hors périmètre V1 de cette page (la couverture BE employeur viendrait d'une page distincte si signal).
- [x] **Autres domaines** : non applicable — page d'acquisition, pas de domaine juridique technique.
- [x] **Autres UI patterns** : réutilise le pattern « page publique » de `/demos` (hero + sections + footer + CTA Calendly + SEO meta).
- [x] **Autres flows transversaux** : **Navigation / routing** — nouvelle route publique `/employeur` (no-auth), à scanner ci-dessous.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : aucune API consommée (page statique).
- [x] **Record / DTO backend** : non applicable.
- [x] **Service / logique métier** : non applicable.
- [x] **Entité JPA + schéma DB** : non applicable (aucune table, aucune migration).
- [x] **Tests existants** : pas de test backend impacté ; ajout d'un spec Jest dédié + assertion route dans smoke E2E.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Route publique `/employeur` | Oui | Ajoutée à `app.routes.ts` à côté de `/demos`, `/blog`, no-auth, lazy. Aucun guard. |
| Sitemap / SEO | Oui | meta + canonical posés dans le composant (pattern `/demos`). Sitemap XML : non requis V1 (page d'amorce ; à ajouter si indexation voulue). |
| Navigation produit avocat | Non | Pas de lien depuis le shell authentifié (séparation des audiences, invariant neutralité D8). |
| Pré-fill IA / F-IA-03 / F-IA-04 / dashboard refresh | Non | Page marketing statique sans outil décisionnel. |

### Décision

- [x] Non applicable aux autres cibles (justification explicite) : page d'acquisition statique, aucun mécanisme d'outil décisionnel ni de persistance à propager.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF-DRH-01-01 est une **page marketing publique statique**, pas un composant décisionnel (`<app-XXX-section>`) consommant un endpoint POST/GET décisionnel et intégré au panel F-IA-04 via `TOOL_REGISTRY`. Aucun formulaire, aucun calcul, aucune extraction IA.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : aucun outil décisionnel à champs saisissables. La page n'a aucun formulaire ; le seul élément interactif est un lien externe Calendly.

---

## Critères d'acceptation

- [ ] La route `/employeur` rend le composant `EmployerLandingComponent` (public, no-auth, lazy `loadComponent`).
- [ ] Le hero affiche le titre exact « Chiffrez l'exposition prud'homale d'un licenciement — avant de décider. ».
- [ ] Le CTA primaire (hero) ET le CTA final pointent vers `https://calendly.com/tounga-franck-ng-itconsulting/30min` avec `target="_blank"` et `rel` contenant `noopener`.
- [ ] La section valeur liste les 4 capacités réelles (qualification, vices de procédure, chiffrage Macron, délais) — aucune mention d'un « tableau de bord portefeuille » ou écran DRH-natif.
- [ ] La section confiance mentionne hébergement EU, RGPD et données isolées.
- [ ] La page contient le messaging « maîtrise du risque & conformité » et **ne contient pas** la formulation « gagner contre vos salariés » (ni équivalent agressif).
- [ ] **Aucun prix** n'est affiché sur la page (pas de montant € de plan).
- [ ] **Aucune inscription self-serve** : pas de lien vers `/login` ou « créer un compte » comme CTA principal ; le seul CTA d'action est la démo Calendly.
- [ ] `<title>` et meta description positionnés (SEO) ; canonical `https://legalcase.fr/employeur`.
- [ ] Classes CSS du composant **toutes préfixées `emp-`** — aucune classe nue `.hero` / `.section`.
- [ ] Spec Jest vert ; assertion route `/employeur` ajoutée au smoke E2E.

---

## Périmètre

### Hors scope (explicite)

- Produit DRH-natif (workspace `EMPLOYEUR`, écrans portefeuille, pricing corporate) — reste gaté à 2 POC payants (cadrage `docs/drh/CADRAGE-STRATEGIQUE-DRH.md`).
- Formulaire de contact / lead capture custom (le CTA est Calendly ; pas de table `leads`).
- Version belge employeur (page distincte si signal).
- Sitemap XML / soumission Search Console (à ajouter si indexation voulue).
- Lien depuis la navigation produit avocat connecté.
- A/B testing du wording.

---

## Technique

### Endpoint(s)

> Aucun. Page statique publique.

### Tables impactées

> Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `EmployerLandingComponent` (standalone, `frontend/src/app/employer-landing/`) — hero + section valeur + section confiance + CTA final + footer, SEO meta/canonical à l'init, encapsulation par défaut (Emulated) + classes préfixées `emp-` par double sécurité.
- `app.routes.ts` — nouvelle route publique `/employeur` (lazy `loadComponent`, no-auth).

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `EmployerLandingComponent` — le hero rend le titre exact attendu.
- [ ] `EmployerLandingComponent` — le CTA hero et le CTA final ont un `href` Calendly correct, `target="_blank"`, `rel` `noopener`.
- [ ] `EmployerLandingComponent` — aucun symbole `€` / montant de prix dans le DOM rendu.
- [ ] `EmployerLandingComponent` — présence du messaging « maîtrise du risque » / « conformité » ; absence de « gagner contre vos salariés ».
- [ ] `EmployerLandingComponent` — la section valeur rend 4 items.
- [ ] `EmployerLandingComponent` — `<title>` mis à jour (contient « Employeur »).

### Tests d'intégration / E2E

- [ ] `e2e/smoke/landing.spec.ts` (ou spec dédié) — la route `/employeur` est accessible (réponse 200, hero présent). Assertion ajoutée, **non exécutée** dans cette livraison (validation orchestrateur).

### Isolation workspace

- [x] Non applicable — raison : page publique sans données workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — nouvelle route publique `/employeur` (no-auth, lazy), aucun guard, aucune redirection modifiée.
- [ ] Auth / Principal — non.
- [ ] Workspace context — non.
- [ ] Plans / limites — non.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `app.routes.ts` | Ajout d'une route ; le wildcard `**` reste en dernier | Smoke E2E navigation publique (route `/employeur` accessible, `/` et `/demos` inchangées) |
| Landing (`ViewEncapsulation.None`) | Risque de collision de classes globales | Classes `emp-` préfixées + encapsulation par défaut sur le nouveau composant |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/landing.spec.ts` — accessibilité des routes publiques (ajout assertion `/employeur`).

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes impactées

- [ ] Aucune question de `docs/OPEN_QUESTIONS.md` impactée. Le gate DRH est partiellement levé par décision PO **pour cette seule page d'amorce** (le produit DRH-natif reste gaté).

---

## Notes et décisions

- **Encapsulation** : `ViewEncapsulation` par défaut (Emulated) pour ce composant — contrairement à `landing` et `demos` (None). Double sécurité avec préfixe `emp-` sur toutes les classes (retour SF-249-04 : la landing en None expose ses classes globalement).
- **CTA** : URL Calendly `/30min` (créneau 30 min) imposée par le brief ; distincte de l'URL `/demo-legalcase` utilisée sur `/demos` (audience avocat).
- **Pas de table leads** : décision PO — la conversion passe par Calendly (qui capture le lead), pas par un formulaire maison à V1.
