# Mini-spec — F-110 / SF-110-06 : Formulaires de saisie typés dans la dialog d'édition des référentiels

## Identifiant
`F-110 / SF-110-06`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`in-progress`

## Date de création
`2026-04-05`

## Branche Git
`feat/SF-110-06-referentials-typed-edit-form`

---

## Objectif
Remplacer le textarea JSON brut de la dialog "Modifier" par des champs de formulaire typés et lisibles par un avocat, selon le type de section.

---

## Comportement attendu

### Champs par type de section

| Section | Avant | Après |
|---|---|---|
| `LITIGATION_TYPE` | textarea `{"years":3,"article":"..."}` | `Années` (number 1–30) + `Article` (text) |
| `BAREME_MACRON` | textarea `{"supported":true}` | Slide toggle "Applicable au barème Macron" |
| `PENSION_TAUX` | textarea `[[0.18,0.11],...]` | Grille 5 lignes × 2 colonnes (garde exclusive % / garde alternée %) |
| `PRESTATION_COEFF` | textarea `{"coeff":0.30,"dureeReferenceAns":8}` | `Coefficient (%)` (number 0–100) + `Durée de référence (ans)` (number) |
| `IMMIGRATION_PIECES` | textarea JSON array | Textarea newline-separated → sérialisé en `string[]` |
| `IMMIGRATION_JALONS` | textarea JSON array | Reste textarea JSON (structure trop complexe pour V1) |

### Validation
- `LITIGATION_TYPE` : années requis (1–30), article requis (max 200 chars)
- `BAREME_MACRON` : toggle booléen, toujours valide
- `PENSION_TAUX` : chaque cellule requise, entre 0 et 100, 2 décimales max
- `PRESTATION_COEFF` : coefficient requis (0–100), durée requise (entier > 0)
- `IMMIGRATION_PIECES` : au moins 1 ligne non vide
- `IMMIGRATION_JALONS` : validation JSON valide (comportement actuel conservé)

### Sérialisation (avant envoi au backend)
Chaque type reconstruit le `valueJson` avant d'appeler `dialogRef.close()` :
- `LITIGATION_TYPE` → `{"years": N, "article": "..."}`
- `BAREME_MACRON` → `{"supported": true|false}`
- `PENSION_TAUX` → `[[r0c0, r0c1], [r1c0, r1c1], ...]` (valeurs / 100)
- `PRESTATION_COEFF` → `{"coeff": N/100, "dureeReferenceAns": N}`
- `IMMIGRATION_PIECES` → `["pièce 1", "pièce 2", ...]`

### Cas d'erreur
- Valeur hors borne → `mat-error` sous le champ concerné
- `IMMIGRATION_JALONS` : JSON invalide → `mat-error` existant conservé
- Backend retourne une erreur → snackbar erreur (comportement existant inchangé)

---

## Critères d'acceptation

- [ ] Dialog `LITIGATION_TYPE` : 2 champs séparés (années + article), validation, sérialisation correcte
- [ ] Dialog `BAREME_MACRON` : slide toggle, sérialisation `{"supported": bool}`
- [ ] Dialog `PENSION_TAUX` : grille 5×2, % affiché (0–100), sérialisation en matrice /100
- [ ] Dialog `PRESTATION_COEFF` : 2 champs (% + ans), sérialisation correcte
- [ ] Dialog `IMMIGRATION_PIECES` : textarea newline-separated, sérialisé en `string[]`
- [ ] Dialog `IMMIGRATION_JALONS` : textarea JSON inchangé
- [ ] Aucun changement backend (même endpoint PUT, même format valueJson)
- [ ] Libellé du champ JSON disparaît pour tous les types typés

---

## Composants impactés

| Composant | Modification |
|---|---|
| `referential-edit-dialog.component.ts` | Formulaire dynamique selon `sectionType` |
| `referential-edit-dialog.component.html` | Champs typés par `@switch (data.sectionType)` |

---

## Plan de test

### Unitaires (EDT)
- EDT-01 : `LITIGATION_TYPE` — sérialise correctement `{years: 3, article: "Art. L3245-1"}`
- EDT-02 : `LITIGATION_TYPE` — invalide si années hors borne (0, 31)
- EDT-03 : `BAREME_MACRON` — toggle true → `{"supported":true}`
- EDT-04 : `PENSION_TAUX` — grille 5×2 → matrice correcte, valeurs /100
- EDT-05 : `PENSION_TAUX` — invalide si cellule > 100 ou vide
- EDT-06 : `PRESTATION_COEFF` — sérialise `{coeff: 0.30, dureeReferenceAns: 8}`
- EDT-07 : `IMMIGRATION_PIECES` — newlines → `["p1","p2"]`, filtre lignes vides
- EDT-08 : dialog s'ouvre avec les valeurs pré-remplies depuis `entry.valueJson`

### Intégration
- Non applicable (pas de changement backend)

### Isolation workspace
- Non applicable (affichage uniquement)

---

## Hors périmètre
- Backend : aucun changement
- `IMMIGRATION_JALONS` : reste textarea JSON
- Ajout/suppression de jalons dynamiquement
- Validation sémantique du contenu (ex : vérifier que l'article existe)
