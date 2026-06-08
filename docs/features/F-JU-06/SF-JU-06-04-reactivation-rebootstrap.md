# Mini-spec — F-JU-06 / SF-JU-06-04 Réactivation d'un mapping archivé au re-bootstrap

## Identifiant
`F-JU-06 / SF-JU-06-04`

## Feature parente
`F-JU-06` (Qualité des citations jurisprudence). SF de durcissement — **bugfix/design**, exemptée des étapes 0 / 0 bis (job admin interne, aucun workflow avocat ni écran nouveau).

## Statut
`ready`

## Branche Git
`feat/SF-JU-06-04-reactivation-rebootstrap`

## Objectif (une phrase)
Permettre au re-bootstrap de **réactiver** (UPDATE) un mapping jurisprudence **archivé** lorsque le même arrêt est re-sélectionné **et** repasse les 3 garde-fous F-JU-06 — au lieu de le « skipper » à vie.

## Problème (constaté à l'assainissement F-JU-06, 2026-06-06)
`JurisprudenceBootstrapService` est idempotent sur le triplet `(toolId, brancheCalculId, arretRef)` via `existsByToolIdAndBrancheCalculIdAndArretRef` — **sans filtrer `archived`** (`JurisprudenceBootstrapService.java:286`). Conséquence : une fois un mapping **archivé** (par la ré-évaluation SF-JU-06-02), le re-bootstrap **skippe** ce triplet pour toujours et **ne peut jamais réactiver** l'arrêt, même s'il redevient un candidat légitime passant les garde-fous durcis. La contrainte unique `uq_tool_jurisprudence_mappings_active` portant sur `(tool_id, branche_calcul_id, arret_ref)` **sans** `archived`, on ne peut pas non plus ré-INSÉRER → la seule voie propre est l'**UPDATE** (réactivation).

## Comportement nominal
Dans la boucle de bootstrap, au point d'idempotence (après les 3 garde-fous : chapeau non vide, confiance ≥ 0,70, 2ᵉ passe pertinente), pour le triplet `(toolId, brancheCalculId, chosenRef)` :
1. **Aucun mapping** → INSERT (comportement actuel, `created++`).
2. **Mapping actif** (`archived = false`) → **skip** (idempotence inchangée, `skipped++`).
3. **Mapping archivé** (`archived = true`) → **réactivation** : `archived = false` + mise à jour des champs (juridiction, date, n° pourvoi, lien, chapeau, `confidenceScore`, `lastVerifiedAt`) avec les données du candidat courant + entrée d'audit **`AUTO_REACTIVATE`**. Compté dans `created` (le jeu actif gagne un arrêt) et loggé distinctement.

L'arrêt n'arrive à ce point **que** s'il a passé les 3 garde-fous F-JU-06 en amont → un arrêt archivé pour mauvaise qualité (chapeau vide, confiance basse, hors-sujet) **ne repasse pas** les garde-fous → **n'est jamais réactivé**. La réactivation ne contredit donc pas l'assainissement.

## Cas d'erreur / bords
- Échec d'UPDATE (RuntimeException) → capté comme les INSERT (try/catch existant `JurisprudenceBootstrapService.java:301`), `skipped++`, log warn, pas d'interruption du job.
- Deux exécutions consécutives : 1ʳᵉ réactive, 2ᵉ voit le mapping actif → skip (pas de double réactivation).
- Aucune régression sur le chemin INSERT et le chemin skip-actif.

## Solution technique (backend uniquement, **pas de migration**)
1. **`ToolJurisprudenceMappingRepository`** : ajouter `Optional<ToolJurisprudenceMapping> findByToolIdAndBrancheCalculIdAndArretRef(String toolId, String brancheCalculId, String arretRef)`.
2. **`JurisprudenceAuditAction`** : ajouter la valeur `AUTO_REACTIVATE` (varchar(30) sans CHECK → pas de migration).
3. **`JurisprudenceBootstrapService`** : remplacer le bloc `existsBy… → skip` (l.286-293) par un `findBy…` :
   - présent + actif → skip ;
   - présent + archivé → réactivation via une méthode `reactivate(mapping, chosen, evaluation, triggerUser)` (UPDATE + audit `AUTO_REACTIVATE`) ;
   - absent → `persistTopCandidates` (INSERT, inchangé).
   - Factoriser le mapping des champs `chosen → entité` (partagé INSERT/UPDATE) pour éviter la duplication.

## Critères d'acceptation (vérifiables)
1. Mapping **archivé** + arrêt repassant les garde-fous → **réactivé** : `archived=false`, champs mis à jour, **pas de doublon** (contrainte unique respectée), audit `AUTO_REACTIVATE`. (test)
2. Mapping **actif** existant → **skip**, aucun UPDATE, aucun doublon. (test)
3. Arrêt archivé **ne repassant pas** un garde-fou (chapeau vide / confiance < 0,70 / hors-sujet) → **reste archivé**, non réactivé (le rejet intervient avant l'idempotence). (test)
4. Aucun mapping → **INSERT** (non-régression). (test)
5. Pas de migration ; suite backend verte.

## Plan de test minimal
- **`JurisprudenceBootstrapServiceTest`** (Mockito) : cas (a) actif → skip ; (b) archivé + garde-fous OK → réactivé (vérifier `archived=false`, champs, save, audit `AUTO_REACTIVATE`) ; (c) archivé + chapeau vide / confiance basse / hors-sujet → pas de réactivation ; (d) absent → INSERT.
- **Isolation workspace** : N/A — `tool_jurisprudence_mappings` est une table **globale** (référentiel plateforme), job déclenché par SUPER_ADMIN, non multi-tenant.

## Tables / endpoints / composants impactés
- **Backend** : `JurisprudenceBootstrapService`, `ToolJurisprudenceMappingRepository` (+1 méthode), `JurisprudenceAuditAction` (+1 valeur). Aucun endpoint nouveau (le bouton « Bootstrap » + flag `enrichQueries` existants pilotent le job).
- **Frontend** : aucun (le contrat `JurisprudenceBootstrapResponse` est inchangé ; les réactivations sont comptées dans `created`).
- **Migration** : **aucune**.

### Préoccupations transversales
Aucune (pas d'auth/Principal, pas de workspace context, pas de plan/limite, pas de navigation, pas d'outil décisionnel modifié). Pipeline jurisprudence interne uniquement.

## Hors périmètre
- Retrait automatique d'un arrêt actif médiocre qui **passe** les garde-fous (cas « biscotterie sur-sujet ») — relève d'un durcissement de la 2ᵉ passe, sujet distinct.
- Ajout d'un compteur `reactivated` distinct dans `JurisprudenceBootstrapResponse` (changerait le contrat frontend) — compté dans `created` + log/audit suffisent au MVP.
- Top-N (>1 arrêt par outil) — reste V1 top-1.
