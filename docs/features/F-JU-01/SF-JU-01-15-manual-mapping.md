# Mini-spec — F-JU-01 / SF-JU-01-15 Création manuelle de mapping (SUPER_ADMIN)

## Identifiant

`F-JU-01 / SF-JU-01-15`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-15-manual-mapping`

---

## Objectif

Permettre à un SUPER_ADMIN de créer manuellement un mapping `tool_jurisprudence_mappings` ad hoc, pour combler les outils que le bootstrap auto-pilot Claude n'a pas couvert (mots-clés trop génériques pour `/search`, outils BE non couverts par JUDILIBRE FR en attendant **F-JU-04**).

Identifiée le 2026-05-27 après le 3ᵉ run du bootstrap CSV 200 entrées : **71 outils couverts sur 75** (~95 %). 5 outils manquants — 1 FR (`F-DT-75-conges-payes-arret-maladie`) + 4 BE (`at-fedris-declaration`, `c4-onem-checklist`, `contestation-c4-onem`, `outplacement-be-obligatoire-45`).

**Note process** : ce trou « complétion manuelle » aurait dû être identifié dès la spec F-JU-01 V1. La spec mentionnait :
> *« Dashboard admin reste disponible pour audit a posteriori + traitement des flags utilisateurs + override manuel ponctuel »*

mais aucune SF n'a été ouverte pour l'« override manuel » — seul l'arbitrage de flags existants était implémenté. SF-15 comble ce trou.

---

## Comportement attendu

### Cas nominal

Nouveau endpoint **`POST /api/v1/super-admin/jurisprudence-watch/mappings`** :
- Body `ManualMappingCreateRequest` : `toolId, brancheCalculId, arretRef, juridiction, dateArret, numeroPourvoi, lienLegifrance, chapeauOfficiel` — **tous obligatoires** (validation Bean Validation)
- Service `JurisprudenceWatchAdminService.createManualMapping(request, user)` :
  1. Vérifie unicité via `existsByToolIdAndBrancheCalculIdAndArretRef` (réutilise SF-14)
  2. INSERT `tool_jurisprudence_mappings` avec `confidence_score = 1.00`, `last_verified_at = now()`, `archived = false`
  3. INSERT `jurisprudence_audit_log` action `MANUAL_ADD`, actor `SUPER_ADMIN`, claudeReason `"Création manuelle SUPER_ADMIN (SF-JU-01-15)"`
- Retourne **201 Created** avec `ManualMappingCreatedResponse` (id + champs métier)

Nouveau tab **« Ajouter mapping »** dans `/super-admin/jurisprudence-watch` :
- Formulaire 8 champs (text inputs + datepicker + textarea pour chapeau)
- Bouton « Créer le mapping » désactivé tant qu'un champ est vide
- Snackbar succès + reset du form + reload audit log

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Triplet `(toolId, brancheCalculId, arretRef)` déjà existant | 409 Conflict + message « Mapping déjà existant pour ce triplet ». Frontend affiche snackbar dédié. |
| Champ obligatoire absent ou regex incorrect | 400 Bad Request (Bean Validation). Frontend bloque le bouton si form incomplet. |
| User sans rôle SUPER_ADMIN | 403 (gating existant `superAdminService.assertSuperAdmin`) |

---

## Critères d'acceptation

- [ ] `JurisprudenceWatchAdminService.createManualMapping` vérifie l'unicité avant INSERT.
- [ ] Le mapping créé a `confidence_score = 1.00` et `archived = false`.
- [ ] Un `jurisprudence_audit_log` `MANUAL_ADD` est créé avec `actor = SUPER_ADMIN`.
- [ ] 409 retourné si triplet déjà existant.
- [ ] 400 retourné si champ manquant ou format invalide (regex).
- [ ] Endpoint gated par `SUPER_ADMIN` (anti-régression).
- [ ] Tab « Ajouter mapping » visible dans le dashboard.
- [ ] Form bloque le bouton si un champ est vide.
- [ ] Tests UT 2/2 verts sur le service + tests Jest 3/3 verts sur le composant.
- [ ] Anti-régression : 7/7 tests existants `JurisprudenceWatchAdminServiceTest`, 27/27 Jest.

---

## Hors scope (F-JU-04 Backlog)

- **Source juris belge** : Juridat / Cass BE / WebSearch Anthropic — feature dédiée F-JU-04 à ouvrir au backlog. SF-15 permet le contournement manuel en attendant.

---

## Technique

| Fichier | Modification |
|---------|--------------|
| `ManualMappingCreateRequest.java` (nouveau) | Record DTO avec Bean Validation |
| `ManualMappingCreatedResponse.java` (nouveau) | Record DTO réponse 201 |
| `JurisprudenceWatchAdminService.java` | + `createManualMapping(request, user)` |
| `JurisprudenceWatchAdminController.java` | + `POST /mappings` 201 |
| `JurisprudenceWatchAdminServiceTest.java` | +2 UT |
| `jurisprudence-watch-admin.service.ts` | + `createManualMapping(payload)` |
| `jurisprudence-watch.component.ts` | + tab + form + `submitManualMapping()` |
| `jurisprudence-watch.component.html` | + tab « Ajouter mapping » |
| `jurisprudence-watch.component.spec.ts` | +3 Jest (T-14/15/16) |

Aucune migration. Aucun changement de schéma.

---

## Notes

- Pourquoi SF-15 et pas SF-14 : SF-14 a été utilisée en parallèle par une autre session pour le fix **HF-2026-05-27-03** (idempotence du bootstrap — guard `existsBy` avant `save`). SF-15 réutilise cette méthode `existsBy` ajoutée par SF-14.
- F-JU-04 (source BE) reste à ouvrir au backlog. Sans SF-15, les 4 outils BE du CSV étaient bloqués jusqu'à F-JU-04. Avec SF-15, l'admin peut combler manuellement via Juridat / Cass BE en attendant.
