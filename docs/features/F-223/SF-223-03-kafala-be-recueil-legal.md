# Mini-spec — F-223 / SF-223-03 — Outil reconnaissance du recueil légal (kafala) (Belgique)

## Identifiant
`F-223 / SF-223-03` — tool_id `kafala-be-recueil-legal` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-03-kafala-be-recueil-legal`

## Objectif (1 phrase)
Qualifier le sort en Belgique d'une **kafala (recueil légal)** prononcée à l'étranger (CDIP — loi 16/07/2004 ; CC art. 343 al. 2 nouveau — à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019) : exclusion de l'effet adoptif mais reconnaissance possible comme mesure de recueil/protection via le DIP, et voies d'effet en Belgique (autorité parentale déléguée, séjour, succession).

## Périmètre / anti-doublon
**BE-only pur, aucun équivalent FR** (la France exclut la conversion en adoption — Cciv FR art. 370-3 ; reporté explicitement à F-223 par F-217). Distinct de `adoption-be` (la kafala n'est PAS une adoption — institution juridique distincte) et de `mariage-etranger-be-reconnaissance` (acte de famille de nature différente). Ne tranche pas le séjour de l'enfant (immigration F-IM-*).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/kafala-be-recueil-legal-analysis` (+ `GET`).
- Entrées : `paysOrigineKafala` (String ISO 3166-1 alpha-2), `dateActeKafala` (date), `acteOfficielJudiciaireOuAdoul` (bool), `enfantAbandonneOuOrphelin` (bool nullable), `kafilDomicilieBelgique` (bool), `consentementParentsBiologiquesOuAutorite` (bool), `objectifEnvisage` (enum `RECUEIL_PROTECTION` / `TENTATIVE_ADOPTION` / `SEJOUR` / `SUCCESSION`).
- Logique verdict : (a) exclusion de tout effet adoptif (la kafala ne crée pas de lien de filiation — `TENTATIVE_ADOPTION` → orientation négative) ; (b) reconnaissance comme mesure de recueil/protection via CDIP (acte officiel + conformité ordre public belge) ; (c) effets dérivés possibles (délégation d'autorité parentale, tutelle officieuse) ; (d) renvoi vers les outils compétents pour le séjour (immigration) et la succession (`dip-be-loi-applicable-famille`).
- Verdict 4 niveaux : `RECUEIL_RECONNU_COMME_PROTECTION` / `RECONNAISSANCE_SOUS_CONDITIONS` / `EFFET_ADOPTIF_EXCLU` / `QUALIFICATION_INCOMPLETE` + motifs + actes à produire + bases juridiques annotées « (à vérifier par avocat belge — renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `objectifEnvisage` absent ou invalide | 400 |
| `paysOrigineKafala` non ISO-2 (`^[A-Z]{2}$`) | 400 |
| `dateActeKafala` future / mal formée | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `kafalaRecueilDetecte` — **déjà présent** dans `FamilleExtractedData` (ajouté F-202, ligne ~4240). Réutilisé tel quel comme `trigger_field` de visibilité CONTEXTUAL — pas de nouveau champ.
- Pré-fill (F-246) : pays d'origine et date de l'acte pré-remplis si factualisables ; sinon vides. `PREFILL_COUNT_ALWAYS_ZERO = true` toléré si rien d'extractible stable en V1 (le flag pivot existant suffit à la visibilité).

## Critères d'acceptation
- [ ] `TENTATIVE_ADOPTION` → `EFFET_ADOPTIF_EXCLU` + message (renvoi `adoption-be` / délégation AP).
- [ ] Acte officiel + conformité ordre public → `RECUEIL_RECONNU_COMME_PROTECTION`.
- [ ] Acte non officiel → `RECONNAISSANCE_SOUS_CONDITIONS` ou exclusion + motif.
- [ ] Renvois explicites séjour (immigration) et succession (`dip-be-loi-applicable-famille`).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (effet adoptif exclu + recueil reconnu + acte non officiel + renvois), IT endpoint (200 + 400 gate/ISO-2/date + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `kafala_be_recueil_legal_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217).
- Frontend : `kafala-be-recueil-legal-section.component` (+ artefacts) + `TOOL_REGISTRY` `kafala-be-recueil-legal` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `kafalaRecueilDetecte`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : flag pivot `kafalaRecueilDetecte` **réutilisé** (pas de modif record).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Adoption (`adoption-be`) ; séjour de l'enfant kafil (immigration F-IM-*) ; succession (`dip-be-loi-applicable-famille`) ; génération d'actes.
