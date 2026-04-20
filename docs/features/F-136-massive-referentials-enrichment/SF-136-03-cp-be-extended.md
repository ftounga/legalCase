# Mini-spec — F-136 / SF-136-03 Enrichissement + ajout CP BE

## Identifiant · `F-136 / SF-136-03`
## Date · `2026-04-20` · Branche · `feat/SF-136-03-cp-be-extended`

## Objectif
Deux volets indépendants dans la même migration 091 :
1. **Enrichir** 4 CP BE existantes identifiées par l'audit F-136 SF-136-01 comme "légères" ou sans congés supp (CP 200, CP 302, CP 118, CP 220)
2. **Ajouter** 7 nouvelles CP BE à fort volume (CP 121, CP 201, CP 207, CP 226, CP 306, CP 322, CP 337)

Après la SF, la DB contient **17 CP BE** seedées (vs 10 avant : 3 initiales + 7 ajoutées par SF-129-04).

## CP enrichies
| Code | Changement |
|---|---|
| CP 200 Employés | +1 niveau congés supp, +1 niveau prime d'ancienneté (4 niveaux complets) |
| CP 302 Horeca | +2 niveaux primes |
| CP 118 Alimentaire ouvriers | congés supp ajoutés (3 niveaux) |
| CP 220 Alimentaire employés | congés supp ajoutés (3 niveaux) |

## CP ajoutées
| Code | Secteur |
|---|---|
| CP 121 | Nettoyage |
| CP 201 | Commerce de détail indépendant |
| CP 207 | Industrie chimique (employés) |
| CP 226 | Commerce international / transport / logistique (employés) |
| CP 306 | Entreprises d'assurances (employés) |
| CP 322 | Entreprises de travail intérimaire |
| CP 337 | Secteur non marchand auxiliaire |

## Critères d'acceptation
- [x] Migration 091 avec 2 changeSets (enrichissement + ajouts)
- [x] UUIDs bf-c5 sans collision (b8-be utilisés par SF-129-04)
- [x] Rollback documenté pour chaque changeSet (UPDATE vs DELETE)
- [x] `source_ref` explicite "valeurs indicatives" pour chaque nouvelle entry
- [x] Context load PASS

## Hors scope
- Autres référentiels (SF-136-04)
- Frontend (cohérence automatique via endpoint filtré par pays SF-137-01)
