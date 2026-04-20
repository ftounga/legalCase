# Mini-spec — F-137 / SF-137-02 + SF-137-03 + SF-137-04 refonte UX

## Identifiant · `F-137 / SF-137-02..04`
## Date · `2026-04-20` · Branche · `feat/SF-137-ux-search-filters-design`

## Arbitrage de regroupement
Les 3 SFs (recherche, filtres, design) touchent la même page `ReferentialsComponent` et sont mutuellement dépendantes au niveau template (la recherche affecte la liste, les filtres affectent la liste, le design enrobe tout). Regroupées en une seule branche / PR pour cohérence UX et éviter 3 allers-retours. Chaque SF garde sa traçabilité via tests taggés `SF-137-02`, `SF-137-03`, `SF-137-04`.

## SF-137-02 · Recherche full-text
- Signal `searchQuery` + input `mat-form-field` avec icône loupe et bouton clear
- Filtrage live côté frontend sur `entry.key + entry.label + entry.sourceRef`, case-insensitive
- Si aucun résultat → empty state dédié "Aucun résultat" avec bouton "Réinitialiser"

## SF-137-03 · Filtres multi-critères
- Dropdown `mat-select` Type (alimenté dynamiquement depuis `availableTypes()`) + option "Tous"
- `mat-button-toggle-group` Scope (Tous / Système / Personnalisés) permettant de masquer les entries DB-système ou les overrides workspace
- Bouton "Réinitialiser" apparaît dès qu'un filtre est actif (`hasActiveFilters()`)
- `filteredSections()` computed signal combine les 3 filtres (recherche + type + scope) et masque les sections vides après filtrage

## SF-137-04 · Refonte design
- Toolbar dédiée (fond crème clair, bordure discrète, 10px radius)
- Compteur global "N sur M entrées" visible en permanence
- Section-panel : bordure gauche dorée (`border-left: 4px #C9A54B`), badge `type` en chip doré, compteur d'entries en pill bleu
- Entry-card : hover state doré, background jaune pâle pour entries `--custom`, chip `entry.key` en monospace
- Chip "Perso" pour les entries non-système
- `max-width` 1100px (contre 900px) pour accommoder l'écran plus dense

## Critères d'acceptation
- [x] 7 nouveaux tests Jest dans `referentials.component.spec.ts` (taggés SF-137-02/03/04)
- [x] 22 tests existants (REF-UI-01 à 11) restent verts sans modification
- [x] 1057/1057 frontend total
- [x] Build production PASS (0 erreur)
- [x] Design System : polices Inter/Merriweather/JetBrains Mono, palette gold (`#C9A54B`) + navy (`#1A3A5C`) + surface (`#F8F6F0`) + border (`#E6E1D4`), espacements multiples de 4px
- [x] Accessible : labels `aria-*` sur boutons icon, `mat-form-field appearance=outline`
- [x] Backend inchangé — SF-137-01 (filtrage pays) déjà opérationnelle, les filtres UX viennent par-dessus

## Hors scope
- Pagination (volume pas encore suffisant — à prévoir si > 200 entries/section)
- Tri manuel (tri par défaut déjà correct : type + entry_key)
- Recherche backend (full-text DB) : le volume actuel < 200 entries par workspace permet un filtre client efficace
