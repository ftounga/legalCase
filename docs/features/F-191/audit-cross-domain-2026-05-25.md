# Audit de couverture cross-domain — 2026-05-25

> Audit post-F-212 (19/19) + F-216 (15/15) + F-256 (refactor record). Application de la règle de gouvernance `project_coverage_audit_every_10_features.md` — refaire l'audit de couverture tous les 10 outils livrés du bloc 2026-04-24+.

## Bilan livré post-F-212 + F-216

### Compteurs bruts

- **145 Calculators backend** sur master (toutes features confondues).
- Approche grossière par préfixe de classe :
  - Patterns Travail FR (~59) — F-212 contribue 19 outils.
  - Patterns Belgique (~21) — F-211 + F-217 + ~7 outils Travail BE pré-F-213.
  - Patterns Immigration (~17) — F-208/209.
  - Patterns Famille (~49) — F-210 + F-216 + F-217.

### Features P1+P2 par domaine × pays

| Domaine | FR | BE |
|---|---|---|
| **Travail** | F-206 P1 ✅ + **F-212 P2 ✅ 19/19** + extension F-205 ✅ | F-207 P1 ✅ + 🟡 **F-213 P2 en cours (0/10 → vague 1 lancée)** |
| **Immigration** | F-208 P1 ✅ + 🔴 F-214 P2 0/22 | F-209 P1 ✅ + 🔴 F-215 P2 0/10 |
| **Famille** | F-210 P1 ✅ + **F-216 P2 ✅ 15/15** | F-211 P1 ✅ + F-217 P2 ✅ 10/10 |

**Score P2 cross-domain** : **3 domaines sur 6 (Travail FR, Famille FR, Famille BE) terminés**. Travail BE en cours, Immigration FR+BE pas démarré.

### Outils gap restants

| Feature | Cible | Reste | Mini-specs ready |
|---|---|---|---|
| F-213 P2 Travail BE | 10 outils | 10 (vague 1 en cours = -2) | ✅ 10 mini-specs `ready` |
| F-214 P2 Immigration FR | ~22 outils | 22 | ❌ à produire |
| F-215 P2 Immigration BE | ~10 outils | 10 | ❌ à produire |
| F-218 P3 Travail FR | ~45 outils | 45 | ❌ à produire |
| F-219 P3 Travail BE | ~32 outils | 32 | ❌ à produire |
| F-220 P3 Immigration FR | ~25 outils | 25 | ❌ à produire |
| F-221 P3 Immigration BE | ~30 outils | 30 | ❌ à produire |
| F-222 P3 Famille FR | ~12 outils | 12 | ❌ à produire |
| F-223 P3 Famille BE | ~30 outils | 30 | ❌ à produire |

**Total P2+P3 restant** : ~206 outils décisionnels (mais P3 = spécificité nationale forte, peut être splité en multiples vagues si signal terrain).

## Recommandation séquencement post-F-213

1. **F-213 vague 1-5** (10 outils Travail BE) — en cours, pattern Belgium isolé sans conflit avec record FR.
2. **F-214 P2 Immigration FR** — 22 outils, mini-specs à produire (étape 0 + étape 1 par lot de 3-5 SF). Volume avocats FR le justifie. Priorité après F-213.
3. **F-215 P2 Immigration BE** — 10 outils BE-only, après F-214 (parité belge attendue, mémoire `feedback_belgique_never_forget`).
4. **F-218-223 P3** — différer à V10+ (signal terrain prouvant la demande). Le bloc P1+P2 sur les 6 domaines × pays = couverture suffisante pour la cible V1-V9.

## Patterns stabilisés post-F-256

- **Sous-record `@JsonUnwrapped` Jackson 2.19** : pattern obligatoire pour tout nouvel outil étendant `TravailExtractedData` ou `FamilleExtractedData` (records sujet à saturation 255 slots JVM). À reproduire pour `BelgianTravailExtractedData` et `ImmigrationExtractedData` quand ils approchent la même limite.
- **Pattern parallèle 2 agents** : worktrees isolés `/tmp/legalCase-sf-XXX-YY/` + UUIDs/migrations pré-assignés dans le brief + résolution conflits additive HEAD+INCOMING strict par l'agent qui rebase. Confirmé sur ~10 vagues parallèles 2026-05-23/25.
- **F-JU-03 intégré dès le départ** : tout nouvel outil livre `ToolBranchRegistry` + `ToolUsageContributor` + composant frontend instrumenté `<app-tool-jurisprudence-citations>` — pas de PR de completion résiduelle.

## Conclusion

Le bloc V1-V8 cible (Travail FR + Famille FR + Famille BE P2) est **terminé sur 3 domaines sur 6**. Travail BE en cours. Immigration FR+BE reste à attaquer. P3 différé V10+ selon signal.

Master à `56f85a1d` (post F-212 vague 9 + F-216 correctif + F-212 clôture). F-213 vague 1 en cours (2 agents).
