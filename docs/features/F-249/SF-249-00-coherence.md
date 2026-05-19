# F-249 — Cadrage cohérence (étape 0)

**Date** : 2026-05-19
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Feature** : F-249 — Refonte futuriste du tableau de bord d'accueil

---

## Verdict : GO

F-249 ne crée pas une capacité nouvelle : elle **refond un écran existant** (`/dashboard`) et l'enrichit. Toutes les briques fonctionnelles que le dashboard agrège (dossiers, délais, checklists, analyses) sont déjà livrées. Aucun trou amont. Le seul « aval » est la navigation vers un dossier — destination existante. Feature fonctionnellement cohérente.

## Intention métier (1 phrase)

Faire du tableau de bord d'accueil une véritable page d'accueil — belle, moderne, à fort impact visuel — qui répond en un coup d'œil à la question que l'avocat se pose en ouvrant LegalCase : « qu'est-ce qui demande mon attention aujourd'hui ? ».

## Workflow métier réel de l'utilisateur cible

Source : pratique avocat (⚠ hypothèse à valider — pas de doc workflow dédié) + rôle effectif observé de l'écran `/dashboard` (il agrège délais, checklists, dossiers, analyses).

1. L'avocat démarre sa journée de travail et ouvre LegalCase.
2. Il arrive sur le **tableau de bord d'accueil** (`/dashboard`, route post-login par défaut).
3. Il se pose une seule question : *« qu'est-ce qui est urgent / important aujourd'hui ? »*.
4. Il scanne les **délais procéduraux urgents** — un délai raté = forclusion / faute professionnelle. C'est l'information vitale.
5. Il scanne les **alertes de checklist** — points non conformes sur ses dossiers en cours.
6. Il repère le ou les **dossiers prioritaires** du jour.
7. Il clique vers un dossier → il quitte le dashboard pour l'écran détail dossier.
8. Il traite le dossier (import, analyse, outils décisionnels, conclusions…).
9. Entre deux tâches, il **revient au dashboard** pour se ré-orienter — l'écran est consulté plusieurs fois par jour, pas une seule.
10. En fin de journée, il repasse sur le dashboard pour vérifier qu'aucune urgence n'est en suspens.

Le dashboard n'est pas une étape d'un workflow linéaire : c'est un **hub d'orientation récurrent**. Son « job » métier est l'aiguillage vers l'action prioritaire.

## Cartographie features actuelles ↔ workflow

| Brique agrégée par le dashboard | Feature LegalCase | Statut |
|---|---|---|
| Liste / consultation des dossiers | F-04 | ✅ Livrée |
| Délais & échéances procédurales | F-69 (`case_deadlines`, `StatutoryDeadlineService`) | ✅ Livrée |
| Checklists procédurales / points non conformes | F-96 | ✅ Livrée |
| Analyses récentes (pipeline IA) | F-3/4/5 + pipeline | ✅ Livrée |
| Identité de l'avocat (accueil personnalisé) | Auth OAuth2 / Principal | ✅ Livrée |
| Écran de destination après clic | `case-file-detail` | ✅ Livrée |
| Endpoint dashboard | `GET /api/v1/dashboard` → `DashboardSummary` | ✅ Livré |

## Position de la nouvelle feature

F-249 se place aux **étapes 2 à 6 et 9** du workflow : tout le temps que l'avocat passe SUR le dashboard. Elle ne déplace aucune étape — elle améliore le rendu visuel et la densité d'information utile de l'écran déjà occupé à ces étapes.

## Challenge amont

**Aucun trou.** Le dashboard ne fait qu'agréger des données déjà produites par des features livrées (cf. cartographie). L'enrichissement prévu ne crée aucune dépendance nouvelle :

- Hero personnalisé → identité de l'avocat déjà disponible (Principal / contexte d'auth).
- Headline « ce qui demande votre attention » → calculable à partir des données déjà présentes dans `DashboardSummary` (délais, alertes).
- Indicateur de tendance d'activité → les analyses portent déjà un `created_at` en base ; agrégeable côté backend sans nouvelle source de données.

## Challenge aval

La **sortie** du dashboard = l'avocat clique vers le dossier qui demande son attention. La destination (`case-file-detail`) existe.

⚠ **Point de vigilance aval** : chaque élément enrichi doit *mener quelque part*. Un hero, une headline ou un indicateur de tendance non cliquables vers la zone concernée transforment l'enrichissement en décoration → traité en invariant anti-gadget ci-dessous.

## STOPs / pré-requis à ajouter au backlog

Aucun. F-249 ne nécessite aucun pré-requis.

## Invariants anti-gadget pour la mini-spec

1. **Tout élément affiché répond à « qu'est-ce qui demande mon attention ? »** — aucune donnée purement décorative. Un chiffre sans interprétation ni action est retiré.
2. **L'information critique reste la plus lisible.** Les délais urgents (risque de forclusion) ne doivent jamais être visuellement noyés sous la couche « futuriste » (dégradés, halos, effets). Hiérarchie : urgences > reste.
3. **Le hero est actionnable** : accueil + date + headline qui pointe (au clic) vers la zone la plus urgente. Pas de bandeau vide ni purement esthétique.
4. **L'indicateur de tendance est interprétable et navigable**, sinon il est retiré — pas de courbe « jolie » sans signification métier.
5. **Aucune donnée inventée** : tout chiffre provient du DTO réel `DashboardSummary`. Pas de fausse donnée de remplissage.
6. **Animations à l'entrée uniquement**, jamais en boucle — l'écran est consulté plusieurs fois par jour, une animation permanente devient une nuisance.
7. **États vides soignés** : un cabinet sans délai urgent / sans dossier voit un dashboard qui reste beau et rassurant, pas un écran vide cassé.

## Décision finale

**GO.** F-249 démarre. C'est une refonte d'écran existant sans trou fonctionnel. Découpage indicatif ~2-3 SF (amendement DESIGN_SYSTEM → backend DTO → frontend refonte), à figer en mini-spec après l'étape 0 bis. Prochaine étape : cadrage cohérence écran (étape 0 bis).

## Liens

- `ai-skills/feature-coherence-challenger.md` — skill appliquée
- `docs/features/F-249/SF-249-00b-ux-coherence.md` — cadrage cohérence écran (étape 0 bis)
- `docs/PRODUCT_SPEC.md` — F-249 ; F-118 (overlap zone)
- `docs/DESIGN_SYSTEM.md` — amendement « couche futuriste dashboard » à valider
