# SF-253-03 — Export PDF : section « 🔍 Risques à creuser »

## Objectif

Ajouter au PDF récapitulatif une section dédiée aux risques restant à arbitrer (`A_CREUSER`), placée AVANT la section F-195 « ⚠️ Risques retenus par votre avocat » — ordre logique : indécision avant décision.

Aujourd'hui le PDF agrège VALIDÉ + ÉCARTÉ via F-195 mais ne mentionne pas les arbitrages restants. Conséquence : l'avocat reçoit (ou transmet) un PDF qui tait son travail de curation en cours.

## Comportement nominal

### Section « 🔍 Risques à creuser »

- **Titre** : `🔍 Risques à creuser` (police 16/bold/PRIMARY, liseré navy via PRIMARY).
- **Sous-titre** : `N risque(s) à arbitrer — arbitrage avocat en attente` (JetBrainsMono 11, TEXT_SECONDARY, singulier/pluriel adapté).
- **Bloc par risque** : picto 🔍 + libellé Inter 11 + suffixe `→ <label outil>` JetBrainsMono italique 9 si `toolIdsCibles[0]` non null. Liseré navy PRIMARY à gauche (palette gris navy, pas de rouge).

### Visibilité

La section retourne `[]` (donc absente du PDF) si :
- `risquesAlignment` est null / undefined / vide.
- Aucun risque au statut `A_CREUSER` (cas nominal post-arbitrage — F-195 « Risques retenus » couvre alors l'état final).

### Pas de variante critique

Contrairement à F-195 (qui distingue `ERROR` + picto 🔴 pour les libellés contenant `harcèlement`/`violence`/`expulsion`/`dilapidation`), F-253 reste **neutre** : un risque À_CREUSER n'est pas encore arbitré → l'avocat n'a pas (encore) confirmé sa criticité → palette gris navy uniforme.

## Ordre des sections PDF

Ordre canonique mis à jour avec F-253 :

```
Faits → 🎯 Stratégies retenues → 🔍 Conformité procédurale → 📎 Pièces à demander
     → 🔍 Risques à creuser (F-253) → ⚠️ Risques retenus par votre avocat (F-195)
     → ❓ Réponses aux questions complémentaires → Chronologie / autres
```

## Critères d'acceptation

- **CA-01** : section absente si `risquesAlignment` vide / null / undefined.
- **CA-02** : section absente si aucun risque `A_CREUSER` (V+É seulement).
- **CA-03** : section présente si ≥ 1 `A_CREUSER`, avec titre + sous-titre + bloc(s) par risque.
- **CA-04** : sous-titre singulier (`1 risque à arbitrer…`) ou pluriel (`N risques à arbitrer…`) selon le compteur.
- **CA-05** : suffixe `→ <label outil>` rendu via `toolLabelResolver(toolIdsCibles[0])` si non null ; fallback sur le toolId brut si resolver renvoie `null`/vide.
- **CA-06** : section F-253 placée AVANT la section F-195 « Risques retenus » dans le PDF.
- **CA-07** : cohabite avec F-195 (cas dossier mix V/À_C/É) — les 2 sections distinctes apparaissent dans le PDF.
- **CA-08** : pas de pictogramme critique 🔴 dans la section F-253 (réservé à F-195 / VALIDÉ critiques).

## Plan de test minimal

### Jest — `pdf-export.service.spec.ts`

1. `SF-253-03 alignment vide → section absente (fail-open)` — CA-01.
2. `SF-253-03 aucun risque À_CREUSER (V+É seulement) → section absente` — CA-02.
3. `SF-253-03 ≥ 1 risque À_CREUSER → section présente avec titre + sous-titre singulier` — CA-03 + CA-04.
4. `SF-253-03 plusieurs risques À_CREUSER → sous-titre pluriel` — CA-04.
5. `SF-253-03 risque À_CREUSER avec mapping outil → suffixe label résolu` — CA-05.
6. `SF-253-03 cohabite avec F-195 (V+À_C+É) — 2 sections distinctes` — CA-07.
7. `SF-253-03 ordre PDF : F-253 (à creuser) AVANT F-195 (validés)` — CA-06.
8. `SF-253-03 pas de pictogramme critique 🔴 dans la section À_CREUSER (neutre par défaut)` — CA-08.

### Tests de régression

Les tests F-195 SF-195-03 restent verts (section « Risques retenus » et sous-titre score inchangés).

## Tables / endpoints / composants impactés

| Élément | Modification |
|---|---|
| `pdf-export.service.ts` | + 2 méthodes privées (`buildRisquesACreuserSection`, `buildRisqueACreuserBloc`) + 1 spread dans `content` (juste avant F-195) |
| `pdf-export.service.spec.ts` | + 8 tests dédiés SF-253-03 |

**Aucune** modification de :
- La signature publique `buildDocument(...)` (réutilise `risquesAlignment` déjà présent).
- L'appel côté `SynthesisComponent.exportPdf()` (le 4ᵉ stream `risqueAlignmentService` existe déjà depuis F-195 SF-195-03 — fail-open INDÉPENDANT par stream).
- Les couleurs / fonts globaux (`PRIMARY`, `SURFACE`, `BG`, `TEXT`, `TEXT_SECONDARY` déjà importés).
- La méthode `buildRisquesValidesSection` (F-195) — inchangée, CA-07 régression.

## Hors périmètre SF-253-03

- Modification du PDF des risques VALIDÉS (F-195) : non.
- Section dédiée pour les ÉCARTÉS : non (compteur géré par F-195 — déjà visible).
- Page PDF dédiée À_CREUSER : non (bloc inline cohérent F-192/193/194/195).
- Mention dans le sous-titre score F-195 : non (le score F-195 calcule déjà via `risqueAlignmentService.computeScoreAvocat` qui exclut ÉCARTÉ — À_CREUSER reste comptabilisé dans le « retenus »).

## Notes et décisions

- **Pourquoi palette navy uniforme (pas de rouge)** : un risque À_CREUSER est neutre tant que pas arbitré ; la criticité (rouge 🔴) émerge après VALIDATION par l'avocat. Cohérent avec la pill `to_explore` du panel et la pill secondaire SF-253-02.
- **Pourquoi avant F-195 (et pas après)** : ordre logique « indécision → décision ». Le lecteur (avocat ou client) voit d'abord ce qui reste à trancher, puis ce qui a été tranché.
- **Pas de listing des écartés dans F-253** : F-195 a déjà un compteur `❌ Risques écartés : N` qui couvre les ÉCARTÉS.
- **Pas de modification de la signature `buildDocument`** : le paramètre `risquesAlignment` est déjà disponible — F-253 le re-consomme avec un filtre statut différent.

## Estimation

~30 min (2 méthodes privées en miroir de `buildRisquesValidesSection` + 8 tests Jest).
