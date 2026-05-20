# Mini-spec — F-217 / SF-217-17 — Frontend : section reconnaissance mariage / divorce étranger (Belgique)

## Identifiant
`F-217 / SF-217-17`

## Feature parente
`F-217` — P2 Famille BE — Vague 3 — Successions / protection / international

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-217-17-mariage-etranger-be-reconnaissance`

---

## Objectif

Livrer la section Angular de l'outil décisionnel « Reconnaissance mariage / divorce
étranger (Belgique) » (couvrant aussi le talaq) dans le panel décisionnel, branchée
sur l'API figée par SF-217-16, avec son entrée `TOOL_REGISTRY` et le seed
`decision_tool_visibility_rules`.

---

## Comportement attendu

### Cas nominal
1. Sur un dossier de droit de la famille belge, la section
   `app-mariage-etranger-be-reconnaissance-section` apparaît dans le panel décisionnel
   en mode **`CATALOG`** (visibilité gérée par F-IA-04 — outil contextuel, pas
   systématique sur tout dossier famille). L'avocat peut l'ajouter via le catalogue
   F-238.
2. L'avocat saisit les éléments (select `natureActe`, champ ISO-2 `paysOrigine`
   avec aide à la sélection, `<input type="date">` `dateActe`, selects
   `residenceHabituelleAuMoinsUnePartie` / `nationaliteAuMoinsUnePartie`, toggles
   `conformiteDroitFondPersonnel` / `conformiteFormeLocusRegitActum`, bloc conditionnel
   talaq affiché uniquement si `natureActe = TALAQ_REPUDIATION` avec les 4 booleans
   talaq, toggle `conventionBilateraleApplicable`, textarea `commentaire`) et clique
   « Calculer ».
3. Le composant POST la requête, affiche le verdict en bandeau (5 niveaux), la liste
   des motifs de refus (rouge `HIGH`), la liste des motifs de réserve (orange `HIGH`,
   jaune `MEDIUM`), la liste des actes à produire, les bases juridiques et les messages.
4. Au rechargement, la section restitue le dernier résultat (GET) ; « Modifier »
   ré-ouvre le formulaire pré-rempli avec le snapshot d'inputs.

### Cas d'erreur
- Workspace `FRANCE` → bannière info « Outil Belgique uniquement », pas de formulaire,
  aucun appel réseau (gate `workspaceCountry` strict).
- Erreur HTTP backend → `MatSnackBar` rouge, le formulaire reste éditable.
- `GET` 404 (jamais calculé) → mode formulaire, pas d'erreur visible.
- `natureActe = TALAQ_REPUDIATION` mais bloc talaq non saisi → validation côté
  formulaire (les 4 booleans deviennent obligatoires).

---

## Contrat API consommé (importé de SF-217-16 — FIGÉ)
- `POST /api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance`
- `GET /api/v1/case-files/{caseFileId}/mariage-etranger-be-reconnaissance`
- DTO `MariageEtrangerBeReconnaissanceRequest` /
  `MariageEtrangerBeReconnaissanceResponse`, enums `verdict`, `natureActe`,
  `residenceHabituelleAuMoinsUnePartie`, `nationaliteAuMoinsUnePartie`, `code` de
  motif, `severite` — cf. SF-217-16.

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels DIP famille BE | Oui | Aucun outil BE équivalent (F-FA-18-reconnaissance-paternelle / F-FA-18-* sont FR-only ; F-IM-* couvre l'immigration pas le DIP famille). Outil unifié mariages + divorces + talaq cohérent avec la méthode d'analyse CDIP unique (cf. SF-217-16). Un outil = une situation. |
| Pré-fill IA | Oui | Aucun flag pivot V1 effectif → `getPrefillCount()` = 0, `PREFILL_COUNT_ALWAYS_ZERO = true`. Pas de nouveau champ `FamilleExtractedData`. |
| F-IA-03 cohérence | Oui | Câblage `coherenceAlerts` présent ; en V1, `dateActe` croisable potentiellement avec d'autres dates mais aucune source IA stable. Câblage présent, peu actif V1. |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` dans le `next:` du POST. |
| Gate `workspaceCountry` | Oui | Bannière info si workspace FR. |

### Décision
- [x] Étendu à toutes les cibles applicables.

---

## Conformité F-IA-04
- [x] Entrée `TOOL_REGISTRY` ajoutée dans `decisional-tools-panel.component.ts` avec
  `displayLabel` humain (`Reconnaissance mariage / divorce étranger (Belgique)`) —
  garde-fou `DecisionToolDisplayLabelIntegrityIT`.
- [x] Entrée `THEME_BY_TOOL` (thème `VALIDITE` — outil d'analyse de validité de la
  reconnaissance).
- [x] `static getPrefillCount` + helper co-localisé `*-prefill-rules.ts` étiqueté
  `PREFILL_COUNT_ALWAYS_ZERO = true`.
- [x] Seed `decision_tool_visibility_rules` : `tool_id =
  mariage-etranger-be-reconnaissance`, **`CATALOG`**, `DROIT_FAMILLE` / `BELGIQUE`,
  priority 50 — outil contextuel (cf. `SF-217-00-coherence.md` ajustement n° 1 :
  « `CATALOG` pour les situations contextuelles sans flag IA extractible en V1 »).
- [x] Palette navy/or pour info, vert pour `RECONNAISSANCE_DE_PLEIN_DROIT`, orange
  pour `RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS` et `EXEQUATUR_REQUIS`, rouge réservé
  à `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`. `QUALIFICATION_INCOMPLETE` = navy info.
- [x] Bloc talaq affiché conditionnellement (signal computed sur `natureActe`).
- [x] `<input type="date">` pour `dateActe`.
- [x] OnPush + signals + `markForCheck()` dans `next:` / `error:` (cf.
  `feedback_onpush_subscribe_markforcheck`).

### Parité des domaines (niveau ≥ 5)
- Niveau : **5** (analyse de validité / qualification d'ordre public).
- Famille = F-217 (BE-only par construction). Droit du travail / immigration non
  pertinents (l'immigration F-IM-* couvre le séjour / la nationalité, pas la
  reconnaissance d'actes étrangers en famille).

---

## Critères d'acceptation
- [ ] La section apparaît dans le catalogue F-238 sur un dossier Famille BE, est
      absente sur un workspace FR.
- [ ] Une fois ajoutée au dossier (via F-238), la section est visible et utilisable.
- [ ] Le formulaire couvre tous les champs du contrat ; le POST envoie le bon body.
- [ ] Bloc talaq affiché uniquement si `natureActe = TALAQ_REPUDIATION` ; les 4
      booleans deviennent obligatoires.
- [ ] Le verdict (5 niveaux) s'affiche ; rouge réservé à
      `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC`.
- [ ] Listes `motifsRefus` et `motifsReserve` rendues avec sévérité colorée
      (`HIGH` = rouge, `MEDIUM` = jaune, `LOW` = navy).
- [ ] Liste `actesAProduire` rendue en bullet points.
- [ ] « Modifier » ré-ouvre le formulaire pré-rempli avec le snapshot.
- [ ] `getPrefillCount({}) === 0` ; le test d'intégrité prefill passe.
- [ ] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT` verts.
- [ ] Tests Jest du composant verts (statics, gate pays, rendu, calculate, bloc talaq
      conditionnel).

---

## Périmètre
### Hors scope
- Backend (SF-217-16).
- Pré-fill IA (V1 = 0 champ).
- Aide à la sélection du pays au-delà d'un champ texte ISO-2 (V1 = texte libre avec
  pattern de validation ; une combo box de pays est reportée).
- Génération de la requête en transcription / exequatur — outil dédié potentiel,
  reporté.
- Visibilité `CONTEXTUAL` automatique sur détection de mariage étranger — reporté
  (flag pipeline V2 `mariage_etranger_reconnaissance_detecte`).

---

## Technique
### Composants / fichiers
| Fichier | Opération |
|---------|-----------|
| `frontend/src/app/case-files/mariage-etranger-be-reconnaissance-section/mariage-etranger-be-reconnaissance-section.component.ts` | CREATE (standalone) |
| `…/mariage-etranger-be-reconnaissance-section.component.html` / `.scss` | CREATE |
| `…/mariage-etranger-be-reconnaissance-section.component.spec.ts` | CREATE |
| `…/mariage-etranger-be-reconnaissance-section-prefill-rules.ts` / `.spec.ts` | CREATE |
| `frontend/src/app/core/models/mariage-etranger-be-reconnaissance.model.ts` | CREATE |
| `frontend/src/app/core/services/mariage-etranger-be-reconnaissance.service.ts` | CREATE |
| `decisional-tools-panel.component.ts` | EDIT — import + entrée TOOL_REGISTRY + THEME_BY_TOOL |

### Migration Liquibase
- [x] Oui — `277-seed-f217-vague3-mariage-etranger-visibility.xml`
  (seed `decision_tool_visibility_rules`). Numéro `277` = prochain libre après `276`
  (SF-217-16). UUID dédié : `f1a04003-0000-0000-0000-eeee21703XXX`.

---

## Plan de test
### Tests unitaires (Jest)
- [ ] Statics `TOOL_LABEL` / `TOOL_ICON` / `getPrefillCount` exposés.
- [ ] `getPrefillCount({})` = 0.
- [ ] Gate `workspaceCountry=FRANCE` → bannière info, aucun GET.
- [ ] Rendu nominal BE → titre + sélecteurs + toggles affichés.
- [ ] Bloc talaq affiché uniquement si `natureActe = TALAQ_REPUDIATION` ; validation
      des 4 booleans obligatoires.
- [ ] `calculate()` POST le body et bascule en mode résultat.
- [ ] Affichage des couleurs par sévérité dans `motifsRefus` / `motifsReserve`.
- [ ] Validation du champ `paysOrigine` : pattern ISO-2 (2 lettres majuscules).

### Isolation workspace
- [x] Couvert backend (SF-217-16) ; le frontend gate l'affichage par
  `workspaceCountry`.

---

## Analyse d'impact
### Préoccupations transversales
- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + seed visibilité.
  Scan : un outil = une situation (cf. SF-217-16). Mode `CATALOG` choisi pour outil
  contextuel.
- [x] **Navigation / routing** — non modifié (section additive dans un panel existant,
  ajout via catalogue F-238).

### Smoke tests E2E
- [x] Aucun — feature additive.

---

## Dépendances
- SF-217-16 (backend) — contrat API figé ; merge backend avant frontend.

---

## Notes et décisions
- Wrapper complet (formulaire + calcul + restitution) — pattern `divorce-dc-be-section`
  / `autorite-parentale-be-section`.
- Mode **`CATALOG`** (et non `ALWAYS_ON`) : la reconnaissance d'un acte étranger n'est
  pas une situation systématique de tout dossier famille BE — elle est contextuelle
  (typiquement présente sur 20-30 % des dossiers belges selon la population du cabinet,
  cf. audit F-191 § 1.4 « mariage marocain / algérien / turc fréquent en BE »).
  L'avocat l'ajoute via F-238. Pas de flag IA extractible en V1 → pas de mode
  `CONTEXTUAL`. Cohérent avec `SF-217-00-coherence.md` ajustement n° 1.
- Couleurs des sévérités : `HIGH` = rouge, `MEDIUM` = jaune/orange, `LOW` = navy —
  décision UI documentée. La rouge est réservée aux refus civils, pas aux réserves
  procédurales (cohérent avec la palette du produit).
- Le bloc talaq conditionnel est implémenté via un signal computed sur `natureActe()`
  pour éviter la pollution du formulaire dans les autres branches.
- Champ `paysOrigine` en V1 : texte libre validé par pattern ISO-2 (avec hint « 2
  lettres majuscules, ex MA, DZ, TR »). Une combo box de pays est reportée à une
  amélioration ultérieure.
