# Feature Lifecycle — AI LegalCase

## Principe directeur

Aucune ligne de code ne s'écrit sans mini-spec validée.
Chaque feature est découpée jusqu'à obtenir des unités indépendantes, testables et mergeables en moins de 2 jours.

---

## Cycle de vie complet

```
Feature
  └── Subfeature 1
        └── Mini-spec → Dev → Review → Test → Validation → Merge
  └── Subfeature 2
        └── Mini-spec → Dev → Review → Test → Validation → Merge
  └── ...
Feature validée quand toutes ses subfeatures sont mergées et validées.
```

---

## Étape 1 — Définition Feature

**Qui :** Product owner / avocat / lead dev
**Template :** `project-governance/templates/feature-template.md`

**Contenu obligatoire :**
- Objectif fonctionnel en une phrase
- Valeur utilisateur
- Périmètre V1 strict (ce qui est inclus / exclu)
- Liste des subfeatures identifiées
- Dépendances sur d'autres features ou tables
- Références aux questions ouvertes impactées (`docs/OPEN_QUESTIONS.md`)

**Critère de sortie :** La feature est décomposée en subfeatures indépendantes et lisibles.

---

## Étape 2 — Découpage en Subfeatures

**Règles de découpage :**
- Une subfeature = une responsabilité fonctionnelle unique
- Une subfeature peut être développée, reviewée et mergée sans bloquer les autres
- Maximum estimé : 2 jours de dev pour une subfeature
- Si une subfeature dépasse cette taille → la redécouper

**Cas particulier — micro-subfeatures naturellement couplées :**

Deux subfeatures peuvent être développées dans une même PR si toutes les conditions suivantes sont réunies :

- Leur taille cumulée reste ≤ 2 jours de dev
- Elles s'appliquent sur le même flux technique (ex : validation + persistance dans un même endpoint)
- Leurs responsabilités restent distinctes et séparées dans le code (classes/méthodes différentes)
- Chaque critère d'acceptation reste traçable individuellement dans la PR
- La fusion est **explicitement justifiée** dans la section "Décisions techniques" de la mini-spec ou du template PR

La fusion est interdite si :
- Les deux subfeatures peuvent être mergées indépendamment sans risque de régression
- Elles touchent à des couches différentes (ex : backend + frontend)
- La fusion rend la PR trop large pour être reviewée en moins d'une heure

**Exemples de bon découpage :**
```
Feature : Gestion des dossiers
  ├── SF-01 : Création d'un dossier (endpoint + persistance)
  ├── SF-02 : Listage des dossiers du workspace
  ├── SF-03 : Détail d'un dossier
  └── SF-04 : Changement de statut d'un dossier
```

---

## Étape 3 — Mini-spec (obligatoire avant tout dev)

**Template :** `project-governance/templates/subfeature-template.md`
**Checklist avant de commencer :** `project-governance/checklists/readiness-checklist.md`

**Contenu obligatoire de la mini-spec :**
- Contexte et objectif
- Comportement attendu (nominal + cas d'erreur)
- Critères d'acceptation (vérifiables, non ambigus)
- Plan de test minimal
- Tables / endpoints / composants impactés
- Ce qui est hors périmètre

**Blocage :** Si la mini-spec est absente ou incomplète → le dev ne démarre pas.

---

## Étape 4 — Développement

**Règles :**
- Travailler sur une branche dédiée : `feat/SF-XX-nom-court`
- Commits atomiques et lisibles
- Ne pas mélanger plusieurs subfeatures dans une même branche
- Respecter les coding rules : `project-governance/playbooks/coding-rules.md`
- Toute décision technique non prévue dans la mini-spec → la documenter dans la PR

### Parallélisation backend / frontend (optionnelle)

Quand une feature comporte une subfeature backend et une subfeature frontend indépendantes (le frontend consomme une API exposée par le backend), les deux SF peuvent être développées **en parallèle** à condition que :

1. **Le contrat API est figé avant le dev dans la mini-spec** — chaque SF contient explicitement la section "Contrat API" avec :
   - Méthode HTTP + URL exacte
   - Schema du body de requête (champs, types, validation)
   - Schema de la réponse (tous les champs retournés)
   - Codes d'erreur et messages attendus
   - Codes enum éventuels (valeurs exactes et cas d'emploi)
2. **Chaque SF travaille sur sa propre branche** — `feat/SF-XX-YY-backend` et `feat/SF-XX-YY-frontend`, créées depuis `master` à jour.
3. **Chaque SF produit une PR indépendante** — elles peuvent être mergées dans n'importe quel ordre, mais la frontend ne sera utilisable en production qu'après le merge du backend.
4. **Les tests frontend utilisent un mock du service** — pas besoin d'attendre le backend mergé pour faire passer les tests unitaires Jest. L'intégration réelle (end-to-end) est validée après merge des deux PRs.
5. **Les deux mini-specs se référencent mutuellement** — la SF frontend indique "contrat importé de SF-XX-YY-backend" pour traçabilité.

Cette règle s'applique uniquement quand la parallélisation est **explicitement décidée**. Le mode séquentiel standard (SF backend mergée → SF frontend démarrée) reste le défaut.

Les règles de blocage automatique associées (contrat API absent, branches partagées) sont rappelées dans `CLAUDE.md` section "Séquence obligatoire — Étape 3".

---

## Étape 5 — Review

**Template PR :** `project-governance/templates/pr-template.md`
**Checklist review :** `project-governance/checklists/review-checklist.md`
**Règles de review :** `project-governance/playbooks/review-rules.md`

**Critères bloquants pour merger :**
- Mini-spec respectée
- Tests présents et passants
- Critères d'acceptation vérifiés
- Aucune régression sur les tests existants
- Isolation multi-tenant respectée

---

## Étape 6 — Test

**Stratégie complète :** `project-governance/playbooks/testing-strategy.md`

**Minimum requis par subfeature :**
- Tests unitaires sur la logique métier
- Tests d'intégration sur les endpoints
- Cas d'erreur couverts (400, 403, 404, 500)
- Isolation workspace vérifiée si la subfeature accède à des données

---

## Étape 7 — Validation

**Qui valide :** Le développeur + le reviewer
**Critère :** Tous les critères d'acceptation de la mini-spec sont verts.

Si un critère est rouge → retour en dev, pas de merge.

---

## Étape 8 — Merge

**Checklist release :** `project-governance/checklists/release-checklist.md`

**Conditions :**
- Review approuvée
- CI verte
- Pas de conflit non résolu
- Documentation mise à jour si impact sur l'architecture

---

## Étape 9 — Itération

Après merge, évaluer :
- La subfeature suivante est-elle débloquée ?
- Une question ouverte a-t-elle été tranchée → mettre à jour `docs/OPEN_QUESTIONS.md`
- La feature parente est-elle complète ?

---

## États d'une feature / subfeature

| État | Signification |
|------|--------------|
| `draft` | En cours de spécification |
| `ready` | Mini-spec validée, dev peut démarrer |
| `in-progress` | Dev en cours |
| `in-review` | PR ouverte |
| `testing` | Tests en cours |
| `done` | Mergée et validée |
| `blocked` | Bloquée par une dépendance ou question ouverte |

---

## Compatibilité agents

Ce lifecycle est conçu pour être exécutable par des agents.
Chaque étape a des entrées, des sorties et des critères de blocage explicites.
Les templates sont stricts pour permettre un parsing automatique.
