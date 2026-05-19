# Mini-spec — F-249 / SF-249-01 — Refonte futuriste du tableau de bord d'accueil

> Feature à impact écran. Étapes 0 (`SF-249-00-coherence.md`, GO) et 0 bis
> (`SF-249-00b-ux-coherence.md`, GO avec ajustements) produites et validées.

---

## Identifiant

`F-249 / SF-249-01`

## Feature parente

`F-249` — Refonte futuriste du tableau de bord d'accueil (page d'accueil cabinet)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-249-01-dashboard-refonte`

---

## Découpage retenu

F-249 est livrée en **1 SF full-stack** (`SF-249-01`). Le changement backend est
additif et minimal (3 champs sur un DTO existant) et fortement couplé au rendu
frontend — la parallélisation back/front n'apporte rien et créerait 2 PR pour un
écran unique. La SF couvre : amendement `DESIGN_SYSTEM.md` (déjà fait), backend
(enrichissement `DashboardSummaryResponse`), frontend (refonte `DashboardComponent`).

---

## Objectif

> En une phrase : que fait cette subfeature ?

Transformer le tableau de bord d'accueil `/dashboard` en une vraie page d'accueil
— hero personnalisé, compteurs animés, indicateur de tendance, sections
retravaillées avec une couche visuelle « futuriste maîtrisée » conforme à la
charte amendée (DESIGN_SYSTEM §10).

---

## Comportement attendu

### Cas nominal

1. L'avocat se connecte → `/dashboard`. `GET /api/v1/dashboard` renvoie le
   `DashboardSummary` enrichi.
2. Un **hero d'accueil** s'affiche en tête : dégradé navy + halo doré, accueil
   personnalisé (« Bonjour Maître {prénom} » / « Bonjour » si prénom absent),
   date du jour en toutes lettres, **headline d'orientation** (« 3 délais
   urgents, dont 1 critique » / « 2 alertes de checklist » / « Tout est à jour »
   selon les données), 4 compteurs KPI intégrés au hero en glassmorphism avec
   animation count-up à l'entrée, et un mini-sparkline « votre semaine » (7
   jours d'activité d'analyse).
3. Sous le hero, les 4 sections existantes (Délais urgents, Alertes checklist,
   Dossiers ouverts, Activité récente) — mêmes données, rendu retravaillé
   (profondeur, en-têtes liseré or, hover doré), entrée en cascade.
4. La headline et chaque compteur KPI sont cliquables → défilent vers la section
   correspondante (ancre existante `scrollTo`).
5. Tout clic sur un délai / dossier / analyse → navigue vers `case-file-detail`
   (comportement inchangé).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `GET /api/v1/dashboard` échoue | État d'erreur existant conservé (icône + message + bouton « Réessayer ») — restylé mais fonctionnellement identique |
| `userFirstName` absent / null | Accueil générique « Bonjour » sans nom, sans « Maître » |
| `weeklyActivity` vide ou tout à 0 | Sparkline affiche une ligne plate à zéro + libellé « Aucune analyse cette semaine » — pas de graphe cassé |
| Cabinet sans dossier / sans délai | Hero + sections en états vides soignés (déjà gérés, restylés) — headline « Tout est à jour », dashboard reste beau |
| `casesOpenedThisWeek` = 0 | Le KPI « Dossiers actifs » n'affiche pas de delta (pas de « +0 ») |

---

## Contrat API (figé)

Endpoint **inchangé** : `GET /api/v1/dashboard` → `200 DashboardSummaryResponse`.
La réponse gagne **3 champs additifs** (aucun champ existant modifié ou retiré —
les consommateurs actuels ne cassent pas) :

```jsonc
{
  // ... champs existants : openCases, openCasesCount, urgentDeadlines,
  //     staleChecks, recentAnalyses ...
  "userFirstName": "Marie",            // string | null — prénom pour l'accueil
  "casesOpenedThisWeek": 2,            // number — dossiers créés sur 7 jours glissants
  "weeklyActivity": [                  // 7 entrées, du plus ancien (J-6) au jour courant
    { "date": "2026-05-13", "analysesCount": 0 },
    { "date": "2026-05-14", "analysesCount": 3 },
    // ... 5 autres ...
  ]
}
```

Nouveau record backend `DashboardActivityDayItem(LocalDate date, long analysesCount)`.

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Autres outils décisionnels | Non | Le dashboard d'accueil n'est pas un outil décisionnel — pas de Calculator, pas de section `<app-XXX-section>`, pas de `TOOL_REGISTRY`. |
| Autres pays (FR/BE) | Non | Le dashboard agrège tous les dossiers sans logique pays. |
| Autres domaines | Non | Agrège les 3 domaines, aucune adaptation par domaine. |
| Autres UI patterns | Oui | Le hero, le glassmorphism, le sparkline et le count-up sont des patterns **nouveaux et propres au dashboard d'accueil** (couverts par la dérogation DESIGN_SYSTEM §10, limitée à `/dashboard`). Ils ne sont **pas** introduits comme composants partagés `shared/` — donc pas de dette de convergence : aucune réutilisation hors `/dashboard` n'est prévue ni autorisée. |
| Flows transversaux (auth/workspace/plans/nav) | Non | Endpoint et route inchangés (cf. Analyse d'impact). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.
- [x] Non applicable aux autres cibles (justification ci-dessus).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : `DashboardComponent` est l'écran
  d'accueil agrégateur, **pas un composant décisionnel**. Aucune section type
  `<app-XXX-section>`, aucun endpoint POST/GET décisionnel, aucune entrée
  `TOOL_REGISTRY`, aucun pré-fill / validation F-IA-03.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : aucun outil décisionnel ni
  champ saisissable. Le dashboard est en lecture seule.

---

## Critères d'acceptation

- [ ] `GET /api/v1/dashboard` renvoie les 3 nouveaux champs (`userFirstName`,
  `casesOpenedThisWeek`, `weeklyActivity` à 7 entrées) sans modifier les champs
  existants.
- [ ] `weeklyActivity` couvre exactement les 7 derniers jours calendaires
  (J-6 → aujourd'hui), une entrée par jour, count `0` pour les jours sans
  analyse, ordonnées du plus ancien au plus récent.
- [ ] `casesOpenedThisWeek` = nombre de dossiers non supprimés créés sur les
  7 derniers jours du workspace.
- [ ] Le hero affiche accueil personnalisé + date + headline d'orientation +
  4 KPI animés + sparkline ; il **remplace** la barre KPI (aucun bloc primaire
  ajouté — invariant étape 0 bis).
- [ ] Les KPI affichent l'animation count-up **une seule fois** à l'entrée.
- [ ] Headline et KPI cliquables défilent vers leur section.
- [ ] Les délais urgents restent visibles sans scroll à 768px de large.
- [ ] États vides (cabinet vide, semaine sans analyse) rendus proprement.
- [ ] Couleurs strictement dans la palette (navy/or/sémantiques) ; polices
  Inter/Merriweather/JetBrains Mono ; espacements multiples de 4px.
- [ ] Isolation workspace : `weeklyActivity` et `casesOpenedThisWeek` ne
  comptent que les données du workspace courant.

---

## Périmètre

### Hors scope (explicite)

- Toute modification d'un autre écran que `/dashboard`.
- Tout nouveau bloc primaire / nouvelle route / nouvelle navigation.
- Optimisation mobile < 768px (DESIGN_SYSTEM §8 — non ciblée en V1).
- Réutilisation du hero / glassmorphism hors `/dashboard` (interdit, §10).
- Personnalisation du contenu agrégé (le périmètre des données reste celui du
  `DashboardSummary` actuel + les 3 champs ci-dessus).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle | Changement |
|---------|-----|------|------|-----------|
| GET | `/api/v1/dashboard` | Oui | MEMBER | Inchangé — réponse enrichie de 3 champs additifs |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_files` | SELECT (count) | comptage des dossiers créés sur 7 jours |
| `case_analyses` | SELECT | analyses DONE du workspace sur 7 jours, bucketées par jour |
| `users` | — | `firstName` lu depuis l'entité `User` déjà résolue par le controller |

### Migration Liquibase

- [x] Non applicable — aucun changement de schéma.

### Backend — fichiers

- `DashboardSummaryResponse.java` — 3 champs additifs ; nouveau record
  `DashboardActivityDayItem(LocalDate date, long analysesCount)`.
- `DashboardService.java` — `buildSummary` reçoit aussi le `User` ;
  calcule `casesOpenedThisWeek` et `weeklyActivity` (cutoff = `now - 7 j`).
- `DashboardController.java` — passe le `User` déjà résolu à `buildSummary`.
- `CaseFileRepository` — `countByWorkspaceAndDeletedAtIsNullAndCreatedAtAfter(Workspace, Instant)`.
- `CaseAnalysisRepository` — `findByCaseFile_WorkspaceAndAnalysisStatusAndCreatedAtAfter(Workspace, AnalysisStatus, Instant)`.

### Frontend — fichiers

- `core/models/dashboard.model.ts` — `DashboardActivityDay` + 3 champs sur `DashboardSummary`.
- `dashboard/dashboard.component.ts` / `.html` / `.scss` — refonte.
- `shared/animations.ts` — réutilisation `fadeInUp` ; ajout éventuel d'un trigger stagger.

---

## Direction visuelle détaillée (brief d'implémentation)

Conforme à `DESIGN_SYSTEM.md` §10 (couche d'accueil dashboard).

### Hero (remplace la barre KPI)

- Bloc pleine largeur, `border-radius` 16px, fond **dégradé navy**
  `linear-gradient(135deg, #1A3A5C 0%, #0E2341 100%)`, halo doré diffus
  (pseudo-élément `radial-gradient` `#C9973A` à ~12 % d'opacité, en haut à
  droite), ombre `0 4px 12px rgba(16,35,65,.18)` + `0 8px 24px rgba(16,35,65,.12)`.
- Ligne 1 : « Bonjour Maître {prénom} » (Merriweather, blanc) + date du jour
  en toutes lettres (Inter, blanc à ~70 %).
- Ligne 2 : **headline d'orientation** — phrase calculée côté frontend à partir
  des données : priorité aux délais critiques, puis délais urgents, puis
  alertes checklist ; sinon « Tout est à jour ». Pastille colorée selon la
  criticité (rouge / or / vert). Cliquable → `scrollTo` la section concernée.
- Ligne 3 : **4 compteurs KPI** en cartes glassmorphism (fond
  `rgba(255,255,255,.08)`, `backdrop-filter: blur(8px)`, bordure
  `1px solid rgba(255,255,255,.14)`), valeur en Merriweather, label en Inter
  uppercase clair. Animation **count-up** ≤ 800 ms à l'entrée (une fois).
  Le KPI « Délais urgents » / « Alertes » prend une teinte d'alerte si > 0.
  Le KPI « Dossiers actifs » affiche un delta discret `+N cette semaine` si
  `casesOpenedThisWeek > 0`.
- Accent : **sparkline** `weeklyActivity` (7 points), tracé SVG or sur le
  dégradé, sans axes, libellé « Votre semaine — N analyses ».

### Sections (Délais / Alertes / Dossiers / Activité)

- Cartes blanches (charte §5 — **pas** de fond coloré), `border-radius` 12px,
  ombre en couches, **en-tête avec fin liseré dégradé or** sous le titre.
- Hover : légère élévation `translateY(-2px)` + glow doré subtil.
- Entrée en **cascade** (stagger ~60 ms) via animation.
- Hiérarchie préservée : la section « Délais urgents » garde le traitement
  d'alerte le plus saillant (rouge/orange) — la couche futuriste ne l'uniformise pas.

### Garde-fous (invariants étapes 0 / 0 bis)

- ~5 blocs primaires (hero + 4 sections) — le hero **absorbe** la barre KPI.
- Délais urgents visibles sans scroll à 768px.
- Aucune animation en boucle ; count-up et cascade jouent **une seule fois**.
- Tout élément du hero est cliquable vers sa zone (pas de décor mort).

---

## Plan de test

### Tests unitaires backend (`DashboardServiceTest`)

- [ ] `buildSummary` — `weeklyActivity` contient 7 entrées, dates J-6→J0, ordre croissant.
- [ ] `buildSummary` — jour sans analyse → entrée présente avec `analysesCount = 0`.
- [ ] `buildSummary` — `analysesCount` par jour correct (bucketing).
- [ ] `buildSummary` — `casesOpenedThisWeek` compte les dossiers créés < 7 j, exclut les plus anciens et les supprimés.
- [ ] `buildSummary` — `userFirstName` repris du `User` ; null géré.

### Tests d'intégration backend (`DashboardControllerIT`)

- [ ] `GET /api/v1/dashboard` → 200, payload contient les 3 nouveaux champs, `weeklyActivity` à 7 entrées.
- [ ] Isolation workspace : un dossier / une analyse d'un autre workspace ne sont pas comptés.

### Tests frontend (Jest)

- [ ] Le hero rend accueil + date + headline + 4 KPI + sparkline.
- [ ] `userFirstName` null → accueil générique sans « Maître ».
- [ ] `weeklyActivity` tout à 0 → sparkline plat + libellé « Aucune analyse cette semaine ».
- [ ] Headline calculée : délai critique > délai urgent > alerte checklist > « Tout est à jour ».
- [ ] Clic KPI / headline → `scrollTo` appelé avec la bonne ancre.
- [ ] États vides (cabinet sans dossier) rendus sans erreur.

### Isolation workspace

- [x] Applicable — testée en IT : `weeklyActivity` / `casesOpenedThisWeek` filtrés sur le workspace courant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — `GET /api/v1/dashboard` :
  endpoint, route `/dashboard`, auth et rôle **inchangés**. L'enrichissement du
  DTO est purement additif ; aucun consommateur existant n'est cassé. Pas de
  modification d'auth/Principal, de workspace context, de plans/quotas, ni de
  routing.

### Smoke tests E2E concernés

- [x] Aucun smoke test bloquant — aucune préoccupation transversale touchée.
  `e2e/smoke/navigation.spec.ts` (qui visite `/dashboard`) doit rester vert :
  à vérifier en non-régression, non bloquant attendu.

---

## Dépendances

- Aucune subfeature bloquante. `DESIGN_SYSTEM.md` §10 (amendement couche
  d'accueil) : produit dans cette SF, pré-requis du rendu frontend.
- Aucune question ouverte de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Sparkline en SVG inline** (pas de librairie de graphes) : 7 points, tracé
  simple, zéro dépendance ajoutée.
- **`weeklyActivity` bucketé en Java** (pas de `GROUP BY` SQL) : volume de 7
  jours négligeable, et garde la compatibilité H2 (tests) / PostgreSQL (prod)
  sans SQL spécifique.
- **`userFirstName`** servi par le DTO dashboard plutôt que par un appel
  d'auth séparé : le dashboard reste auto-suffisant en un seul appel réseau.
