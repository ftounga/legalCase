# Mini-spec — F-140 / SF-140-01 Infrastructure aide contextuelle + docs de section

## Identifiant · `F-140 / SF-140-01`
## Date · `2026-04-20` · Branche · `feat/SF-140-01-referential-help-popovers`

## Objectif
Aider l'avocat à comprendre, pour chaque section de "Guides & barèmes", ce qu'est le barème, par quels outils il est utilisé, et comment le modifier — **sans jargon technique** (pas de codes F-DT-*). Exposer en plus, par entrée, la description métier déjà présente dans certains JSONs (critères, titres, recours).

## Composants
- `ReferentialHelpDialogComponent` — dialog Material qui gère 2 modes (aide de type / aide d'entrée).
- `SECTION_DOCS` — constante avec 16 fiches (une par type) : titre humain, description, `usedIn[]`, `howToModify`, `format`.
- `ReferentialsComponent.extractMetierDescription(entry, sectionType)` — extrait la meilleure description depuis le JSON selon le type :
  - `LICENCIEMENT_CRITERES` / `RUPTURE_CONV_CRITERES` / `DIVORCE_ETAPES` / `DIVORCE_PIECES` → champ `description`
  - `IMMIGRATION_TITLES` → `motif` + `conditions`
  - `IMMIGRATION_RECOURS` → `juridiction` + `delaiJours` + `textesApplicables`
  - `IMMIGRATION_WORK_RIGHTS` → `droitTravail` + `conditions`
  - `GARDE_MODES` → `repartitionType` + `vacances`
  - `LITIGATION_TYPE` → `years` + `article`
  - Autres → pas de description native (→ SF-140-02 enrichira)

## UI
- Bouton `help_outline` dans chaque section-header.
- Bouton `help_outline` dans chaque entry-header (à côté du crayon).
- Dialog :
  - Titre adaptatif (label de l'entrée ou titre de section)
  - Sections : Description / Où vous le verrez / Comment modifier / Format attendu
  - Mode entrée : chips code + pays, description métier, source juridique, accordion "Voir la valeur brute (JSON)"
  - Palette Design System (gold/navy/crème + Inter/Merriweather/JetBrains Mono)

## Contenu SECTION_DOCS (16 fiches)
Rédigées en langage avocat : outils nommés par leur nom fonctionnel ("Outil Ancienneté", "Comparateur d'indemnités"…), actions concrètes (click crayon, override workspace, rollback), format en prose (pas de dump JSON).

## Critères d'acceptation
- [x] `ReferentialHelpDialogComponent` + SECTION_DOCS 16 entrées
- [x] `extractMetierDescription` couvre 8 types (dont 6 à description riche native + LITIGATION_TYPE + GARDE_MODES synthétisés)
- [x] Boutons info sur chaque section et chaque entry
- [x] Dialog 2 modes (section seule / section + entry)
- [x] Accordion JSON brut
- [x] 4 tests Jest (hasSectionDoc, openSectionHelp, openEntryHelp avec et sans metierDescription)
- [x] 1061/1061 frontend verts (+4)
- [x] Build Angular PASS

## Hors scope
- Enrichissement JSON pour entries sans description native → **SF-140-02** (CONVENTION_BAREMES, IMMIGRATION_JALONS, IMMIGRATION_PIECES, PENSION_TAUX, PRESTATION_COEFF, BAREME_MACRON, INDEMNITE_BAREMES)
- Liens cliquables depuis le dialog vers les outils métier
