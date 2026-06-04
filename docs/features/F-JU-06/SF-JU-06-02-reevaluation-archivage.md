# Mini-spec — F-JU-06 / SF-JU-06-02 Ré-évaluation + archivage des mappings existants

## Identifiant
`F-JU-06 / SF-JU-06-02`

## Feature parente
`F-JU-06` — qualité des citations jurisprudence. Cadrage GO : `SF-JU-06-00-coherence.md`. Suit SF-JU-06-01 (pipeline durci).

## Statut
`ready`

## Date de création
2026-06-04

## Branche Git
`feat/SF-JU-06-02-reevaluation-archivage`

## Type
Feature (assainissement des données existantes). Cadrage cohérence couvert par SF-JU-06-00. Pas d'élément d'écran nouveau majeur (un bouton admin) → étape 0 bis non requise.

---

## Objectif (une phrase)
Passer chaque mapping jurisprudence **déjà en base** dans les garde-fous SF-JU-06-01 et **archiver** ceux qui échouent (chapeau vide, confiance < 0,70, hors-sujet), afin d'éliminer les citations douteuses existantes (ex. « restauration ferroviaire » sur F-DT-09) — **sans re-interroger JUDILIBRE**.

## Contexte
SF-JU-06-01 durcit le pipeline pour les *futurs* mappings, mais les mauvais mappings actuels (`archived=false`) restent affichés. Le re-bootstrap *ajoute* sans *retirer* → il faut une passe de ré-évaluation/archivage de l'existant. C'est l'étape qui assainit réellement les données (décision PO 2026-06-04 : nettoyage d'abord, re-remplissage = SF-03 ultérieure).

## Comportement nominal
Un SUPER_ADMIN déclenche la ré-évaluation depuis `/super-admin/jurisprudence-watch`. Pour chaque mapping actif (`archived=false`) :
1. chapeau vide/blanc → **archive** ;
2. `confidenceScore < 0,70` → **archive** ;
3. 2ᵉ passe de pertinence (`JurisprudenceRelevanceGate`) sujet de l'outil ↔ chapeau → non pertinent → **archive** ;
4. sinon → conservé.
Chaque archivage : `archived=true` + `JurisprudenceAuditLog` action `AUTO_ARCHIVE` avec la raison. Le traitement est **asynchrone** (≈ 1 appel LLM par mapping conservé jusqu'à la 2ᵉ passe). L'opérateur suit le résultat dans l'onglet **« Audit log »** existant.

## Cas d'erreur / bords
- 2ᵉ passe LLM échoue → verdict non pertinent (silence > erreur) → le mapping est archivé (cohérent : on ne garde pas un mapping qu'on n'a pas pu valider). *Décision : on archive sur échec.*
- Aucun mapping actif → job no-op, rapport vide.
- Le job tourne hors fenêtre de déploiement (rolling update tue les jobs async) — responsabilité opérateur, documenté.
- Idempotent : relancer la ré-évaluation ne ré-archive pas ce qui l'est déjà (on ne parcourt que `archived=false`) et ne touche pas les mappings conservés (sauf si l'IA change d'avis — accepté).

## Solution technique
### Backend
1. **Repo** `ToolJurisprudenceMappingRepository.findByArchivedFalse()` → tous les mappings actifs.
2. **`JurisprudenceRelevanceGate`** : surcharge `assess(String sujetOutil, String ref, String juridiction, String chapeau)` (la méthode `assess(sujet, JudilibreArret)` délègue) — permet de ré-évaluer un mapping existant sans `JudilibreArret`.
3. **`JurisprudenceReevaluationService`** (nouveau) :
   - `startReevaluation(User)` → compte les mappings actifs, lance le job via `taskExecutor`, retourne `{ totalAEvaluer }` ;
   - logique par mapping : garde-fous (1)(2)(3) → si échec `archive(mapping, raison, user)` (set archived + audit `AUTO_ARCHIVE`) ;
   - `sujetOutil` dérivé du `toolId` (slug → libellé lisible, ex. `F-DT-09-comparateur-indemnites` → « comparateur indemnites ») + `brancheCalculId` si pertinent ;
   - constante de seuil partagée avec le bootstrap (`0,70`) ;
   - log d'un rapport final (total évalués, archivés par raison, conservés).
4. **Endpoint** `POST /api/v1/super-admin/jurisprudence-watch/reevaluate` (gate `assertSuperAdmin`) → 202 + `{ totalAEvaluer }`.

### Frontend
5. Onglet « Bootstrap » (ou zone admin) de `jurisprudence-watch` : bouton **« Ré-évaluer la qualité des citations »** + dialog de confirmation (action lourde) → POST → message « Ré-évaluation lancée (N mappings) — suivez les archivages dans l'onglet Audit log ». Design DESIGN_SYSTEM (MatDialog confirmation, MatSnackBar).

## Critères d'acceptation (vérifiables)
1. Un mapping actif à chapeau vide est archivé (test).
2. Un mapping actif à confiance < 0,70 est archivé (test).
3. Un mapping actif jugé hors-sujet par la 2ᵉ passe est archivé (test — reproduit « restauration ferroviaire »).
4. Un mapping actif pertinent, chapeau plein, confiance ≥ 0,70 est conservé (test).
5. Chaque archivage produit un `JurisprudenceAuditLog` `AUTO_ARCHIVE` avec raison.
6. Endpoint gate SUPER_ADMIN (401/403 sinon).
7. La 2ᵉ passe passe par le gate Anthropic.
8. Build + tests verts.

## Plan de test minimal
- **Unitaire `JurisprudenceReevaluationService`** : 4 mappings (chapeau vide / conf 0,55 / hors-sujet / valide) → 3 archivés, 1 conservé ; audit `AUTO_ARCHIVE` ×3.
- **Unitaire `JurisprudenceRelevanceGate`** : surcharge `assess(sujet, ref, jur, chapeau)` cohérente.
- **IT endpoint** : POST `/reevaluate` → 202 ; gate SUPER_ADMIN.
- **Frontend spec** : clic bouton → confirm → POST appelé.
- **Isolation workspace** : N/A (donnée globale).

## Tables / endpoints / composants impactés
- **Backend** : `ToolJurisprudenceMappingRepository` (findByArchivedFalse), `JurisprudenceRelevanceGate` (surcharge), `JurisprudenceReevaluationService` (nouveau), `JurisprudenceWatchAdminController` (endpoint).
- **Table** : `tool_jurisprudence_mappings` (UPDATE archived), `jurisprudence_audit_log` (INSERT AUTO_ARCHIVE). Aucune migration de schéma (`AUTO_ARCHIVE` existe déjà dans l'enum).
- **Frontend** : `jurisprudence-watch` (bouton + dialog).

### Préoccupation transversale : **Outil décisionnel métier** + **gate Anthropic**
Composants listés. Pas d'impact auth/workspace/plans/navigation → smoke E2E auth/nav non requis (endpoint admin existant, gate réutilisé).

## Hors périmètre
- **SF-JU-06-03** : requêtes JUDILIBRE ciblées + re-bootstrap pour re-remplir les outils dégarnis.
- Suivi temps réel détaillé du job (polling) : V1 = retour via Audit log. Un job de suivi dédié pourra être ajouté si besoin.
- Exécution effective sur prod = action opérateur (hors fenêtre de déploiement).
