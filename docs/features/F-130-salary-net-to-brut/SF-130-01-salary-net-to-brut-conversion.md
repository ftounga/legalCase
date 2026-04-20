# Mini-spec — F-130 / SF-130-01 Conversion salaire net → brut avec badge

## Identifiant
`F-130 / SF-130-01`

## Feature parente
`F-130` — Conversion salaire net → brut

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-130-01-salary-net-to-brut-conversion`

---

## Objectif

Débloquer les dossiers où les documents mentionnent uniquement le salaire **net** (cas fréquent chez les contrats de nettoyage, services à domicile, HCR, etc.). Aujourd'hui l'IA refuse de convertir net↔brut et laisse `salaire_brut_mensuel=null` → tout le calcul d'ancienneté/comparateur devient inutilisable.

---

## Comportement

### Prompt IA — extension

Mise à jour de la description de `salaire_brut_mensuel` dans `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` :

> "salaire_brut_mensuel" : salaire brut mensuel moyen de référence (décimal). Si les documents mentionnent **uniquement un salaire net**, applique la conversion approximative **brut ≈ net × 1,30** (moyenne FR non-cadre) et positionne `salaire_est_deduit: true`. Si brut explicite présent → `salaire_est_deduit: false` ou omis. Null uniquement si aucun salaire (ni brut ni net) n'est détectable.

Ajout d'un nouveau champ optionnel dans `travail_extracted_data` :
- `"salaire_est_deduit" : true/false/null` — true si brut déduit d'un net

### Backend

`TravailExtractedData` record étend son champ :
```java
public record TravailExtractedData(
    ..., Boolean salaireEstDeduit, ...
)
```

Migration non nécessaire — le DTO est stocké en JSON via `analysis_result`, pas en colonne structurée.

`CaseAnalysisResponse.extractTravailData` lit `salaire_est_deduit` depuis le JSON.

### Frontend

Nouveau badge à côté du champ "Salaire brut mensuel" dans `anciennete-section.component.html` :

```html
@if (aiData?.salaireEstDeduit) {
  <span class="coherence-badge coherence-info"
        matTooltip="Aucun salaire brut dans les documents. Valeur déduite d'un salaire net détecté, multiplié par 1,30 (approximation FR). Corrigez si vous connaissez le brut exact.">
    <mat-icon>info</mat-icon>
    Déduit du net (approx.)
  </span>
}
```

Couleur info (bleu), pas warn (orange) — informatif, pas alerte.

### Cas d'erreur

- IA renvoie `salaire_est_deduit: true` mais `salaire_brut_mensuel: null` → incohérence, le badge ne s'affiche pas (condition `&& salaireBrutMensuel != null` sur le computed)
- IA renvoie valeur invalide (ex. string) → parser existant ignore via try/catch fail-open

---

## Critères d'acceptation

- [ ] Le prompt contient l'instruction de conversion net × 1,30 et demande le flag `salaire_est_deduit`
- [ ] `TravailExtractedData` a le champ `salaireEstDeduit: Boolean`
- [ ] L'endpoint `/synthesis` renvoie `travailExtractedData.salaireEstDeduit`
- [ ] Le frontend affiche le badge "Déduit du net" quand `salaireEstDeduit=true` et `salaireBrutMensuel != null`
- [ ] Le badge ne s'affiche pas quand salaire explicitement brut dans les docs
- [ ] Sur E28 : relancer analyse → salaire brut rempli automatiquement avec badge "Déduit du net"
- [ ] Aucune régression sur les autres champs pré-remplis

---

## Plan de test

### Unitaires backend
- `CaseAnalysisResponseTest` — nouveau test : JSON avec `salaire_est_deduit: true, salaire_brut_mensuel: 2340` → DTO expose la valeur et le flag
- Test : JSON sans `salaire_est_deduit` → flag null (rétrocompat)

### Unitaires frontend
- `AncienneteSectionComponent.spec.ts` — nouveau test : `aiData.salaireEstDeduit=true` + `salaireBrutMensuel=2340` → badge "Déduit du net" visible
- Test : `salaireEstDeduit=false` → badge caché
- Test : `salaireBrutMensuel=null` → badge caché même si salaireEstDeduit=true

### Intégration manuelle staging
- E28 (que net dans les docs) : relancer analyse → salaire brut auto-rempli avec badge

### Isolation workspace
- N/A

---

## Tables / endpoints / composants impactés

### Backend
- `LegalDomainPromptBuilder.java` — prompt TRAVAIL_INSTRUCTION étendu
- `CaseAnalysisResponse.java` — TravailExtractedData + extractTravailData
- `EnrichedAnalysisService.java` — prompt TRAVAIL_INSTRUCTION identique (via appendDomain)
- Tests unitaires

### Frontend
- `TravailExtractedData` model — ajout `salaireEstDeduit?: boolean | null`
- `anciennete-section.component.html` — badge
- `anciennete-section.component.ts` — éventuel computed helper
- Tests

---

## Hors périmètre

- Conversion net→brut pour la Belgique : taux différents (≈ 1,35 non-cadre, 1,40 cadre), renvoyé à SF-130-02
- Conversion brut→net (cas inverse) : pas besoin pour le moment
- Conversion distincte cadre/non-cadre : simplifié à 1,30 en V1 pour ne pas demander à l'IA de détecter le statut cadre
- UI permettant à l'avocat d'ajuster le coefficient (1,28, 1,32, etc.) : V2 si demande

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Belgique | Oui — **Backlog SF-130-02** : taux différent (1,35/1,40), même logique |
| Autres domaines (immigration/famille) | Non applicable — salaire spécifique droit du travail |
| Autres champs d'extraction (date_entree, convention, etc.) | Non applicable — ceux-là sont extraits tels quels sans conversion |
| Comparateur d'indemnités | **Intégré transparent** — consomme salaire brut via même endpoint, badge visible dans l'anciennete-section suffit pour alerter |

**Analyse d'impact cross-cutting** :
- [ ] Auth — non touché
- [ ] Workspace — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de pattern nouveau — badge info réutilise la classe `coherence-badge coherence-info` existante
- [x] Pas de service partagé nouveau — conversion côté IA uniquement
