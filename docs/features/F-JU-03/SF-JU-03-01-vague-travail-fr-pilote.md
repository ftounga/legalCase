# Mini-spec — F-JU-03 / SF-JU-03-01 Instrumentation Travail FR (pilote F-DT-07)

## Identifiant
`F-JU-03 / SF-JU-03-01`

## Feature parente
`F-JU-03` — Instrumentation des outils décisionnels pour F-JU-01 + F-JU-02

## Statut
`partial` — 1 outil pilote livré (F-DT-07), 12 outils restants à instrumenter en réutilisant le pattern

## Date de création
2026-05-23

## Branche Git
`feat/SF-JU-03-01-pilote-travail-fr`

---

## Objectif

Établir le **pattern de référence** F-JU-03 sur 1 outil pilote du domaine Travail FR (F-DT-07 ancienneté/congés/prime), de bout en bout : `ToolBranchRegistry` + `ToolUsageContributor` côté backend + insertion `<app-tool-jurisprudence-citations>` côté frontend + tests. Le pattern sera dupliqué aux 12 outils Travail FR restants dans des PRs ultérieures.

---

## Pattern de référence — 4 étapes par outil

### 1. Backend — `ToolBranchRegistry`

Créer une classe `<Outil>ToolBranchRegistry` dans le même package que l'outil, qui implémente `fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry` :

```java
@Component
public class <Outil>ToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "<tool-id du TOOL_REGISTRY frontend>";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
```

V1 : 1 seule branche `default`. À enrichir si l'outil se ramifie (ex. CCN appliquée, ancienneté > 10 ans, etc.) pour permettre un mapping plus fin et un cron dérive plus précis.

### 2. Backend — `ToolUsageContributor`

Créer une classe `<Outil>ToolUsageContributor` dans le même package, qui implémente `fr.ailegalcase.jurisprudencemapping.ToolUsageContributor` :

```java
@Component
public class <Outil>ToolUsageContributor implements ToolUsageContributor {

    private final <Outil>AnalysisRepository repository;

    public <Outil>ToolUsageContributor(<Outil>AnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return <Outil>ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        <Outil>ToolBranchRegistry.TOOL_ID,
                        <Outil>ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
```

V1 : détection binaire (l'outil est utilisé si une `*Analysis` existe pour le dossier). À enrichir si la branche dépend du contenu de l'analyse.

### 3. Frontend — Composant section

Dans `<outil>-section.component.ts` :

```typescript
// 1. Import
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

// 2. Ajouter dans imports: [...]
ToolJurisprudenceCitationsComponent,

// 3. Ajouter dans la classe (constantes pour le template)
protected readonly toolIdForJurisprudence = '<tool-id>';
protected readonly brancheActiveForJurisprudence = 'default';
```

Dans `<outil>-section.component.html`, à la fin du bloc résultat (avant la fermeture de la section) :

```html
<!-- F-JU-03 SF-JU-03-01 — citations jurisprudentielles F-JU-01 sous le résultat -->
<app-tool-jurisprudence-citations
  [toolId]="toolIdForJurisprudence"
  [branchActive]="brancheActiveForJurisprudence">
</app-tool-jurisprudence-citations>
```

### 4. Tests

- **Backend** : `<Outil>ToolUsageContributorTest` (4 tests : toolId, null caseFileId → empty, no analysis → empty, analysis exists → ToolUsage) + `<Outil>ToolBranchRegistryTest` (1 test).
- **Frontend** : aucun nouveau test requis (le composant `<app-tool-jurisprudence-citations>` est déjà testé séparément ; l'intégration `[toolId]/[branchActive]` est triviale et silencieuse si le mapping n'existe pas).

---

## Outil pilote livré dans cette PR

**F-DT-07 ancienneté/congés/prime** (toolId `F-DT-07-anciennete-conges-prime`) :
- `AncienneteToolBranchRegistry.java` (backend)
- `AncienneteToolUsageContributor.java` (backend, lit `AncienneteAnalysisRepository.findByCaseFileId`)
- `anciennete-section.component.ts` : import + ajout imports[] + constantes `toolIdForJurisprudence` / `brancheActiveForJurisprudence`
- `anciennete-section.component.html` : insertion du composant à la fin du bloc résultat
- `AncienneteToolUsageContributorTest` (4 tests UT)
- `AncienneteToolBranchRegistryTest` (1 test UT)

Tests : 5 backend + 57 anciennete frontend (Jest existants) verts.

---

## Outils Travail FR à instrumenter dans les PRs suivantes (12)

Pattern identique à dupliquer pour chacun (~20-30 min par outil) :

| toolId | Composant frontend | Repository backend |
|---|---|---|
| `F-DT-04-fiche-prudhomale` | `prudhome-fiche-section` | `FichePrudhomaleAnalysisRepository` |
| `licenciement-validite` (F-DT-08) | `licenciement-section` | `LicenciementAnalysisRepository` |
| `compensation` (F-DT-09 comparateur) | `compensation-section` | `CompensationAnalysisRepository` |
| `rupture-conv-indemnite` (F-DT-10) | `rupture-conv-indemnite-section` | `RuptureConvIndemniteAnalysisRepository` |
| `licenciement-nul-detection` (F-DT-16) | `licenciement-nul-detection-section` | `LicenciementNulDetectionAnalysisRepository` |
| `requalification-cdd-cdi` (F-DT-22) | `requalification-interim-cdi-section` | `RequalificationCddCdiAnalysisRepository` |
| `indemnite-conges-payes` (F-DT-26) | `indemnite-conges-payes-section` | `IndemniteCongesPayesAnalysisRepository` |
| `procedure-nullite-licenciement` (F-DT-36) | `procedure-nullite-licenciement-section` | `ProcedureNulliteLicenciementAnalysisRepository` |
| `rupture-periode-essai` (F-DT-38) | `rupture-periode-essai-section` | `RupturePeriodeEssaiAnalysisRepository` |
| `indemnite-preavis` | `indemnite-preavis-section` | repo correspondant |
| `harcelement-licenciement-nul` (F-DT-16 variante) | `harcelement-licenciement-nul-section` | repo correspondant |
| `licenciement-economique` | `licenciement-economique-section` | repo correspondant |

Liste exacte à finaliser au moment de la PR d'extension en lisant le `TOOL_REGISTRY` et en croisant avec les composants `case-files/*-section/` du domaine Travail.

---

## Périmètre

### Hors scope cette PR pilote
- ❌ Les 12 outils Travail FR restants (à livrer en PRs ultérieures sur le même pattern)
- ❌ Travail BE / Immigration FR/BE / Famille FR/BE (SF-JU-03-02 à 06)
- ❌ Logique de branche fine (V2 — V1 utilise `default`)
- ❌ Tests Jest sur l'intégration du composant `<app-tool-jurisprudence-citations>` (déjà couvert par les tests propres du composant en SF-JU-01-04)
- ❌ Bootstrap des mappings F-JU-01 pour F-DT-07 (geste opérationnel via dashboard admin, hors dev)

---

## Critères d'acceptation
- [x] `AncienneteToolBranchRegistry` enregistre `F-DT-07-anciennete-conges-prime:default`
- [x] `AncienneteToolUsageContributor` retourne `Optional<ToolUsage>` non vide si `AncienneteAnalysis` existe pour le dossier
- [x] Composant `<app-tool-jurisprudence-citations>` inséré dans `anciennete-section.component.html`
- [x] Backend compile OK
- [x] 5 nouveaux tests UT verts (4 contributor + 1 registry)
- [x] 57 tests Jest `anciennete-section.component.spec` existants restent verts (aucune régression)
- [x] Mini-spec documente le pattern pour duplication aux 12 outils restants

## Analyse de cohérence transversale
- [x] **Préoccupations transversales** : aucune (additif pur sur 1 outil)
- [x] **Composant partagé** : `<app-tool-jurisprudence-citations>` réutilisable par les ~80 outils — déjà livré en SF-JU-01-04, instrumenté ici sur le 1er outil pilote

## Conformité F-IA-04 / Pré-fill IA
- [x] Non applicable (additif sur outil existant, pas de modification de l'outil lui-même)

## Plan de test
- 5 UT backend (créés)
- 57 Jest frontend (existants, doivent rester verts)

## Notes
1. **Pattern atomique** : 4 fichiers par outil (2 backend + 2 frontend). Pas de migration, pas de modification d'API existante.
2. **Effet visible en prod uniquement après bootstrap** : tant qu'aucune ligne `tool_jurisprudence_mappings` n'existe pour `F-DT-07-anciennete-conges-prime:default`, le composant reste invisible (silence > erreur, déjà géré en SF-JU-01-04).
3. **Pattern V2** : si l'outil a des branches multiples (CCN différente, ancienneté > X années), enrichir `knownBranches()` ET la logique `detectUsage` pour retourner la branche pertinente.

### Coût estimé
- 1 outil pilote : ~30 min (cette PR)
- 12 outils restants : ~6 h (20-30 min par outil)
