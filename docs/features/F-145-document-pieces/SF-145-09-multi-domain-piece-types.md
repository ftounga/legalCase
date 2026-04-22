# Mini-spec — F-145 / SF-145-09 Extension enum `DocumentPieceType` aux 3 domaines

## Identifiant · `F-145 / SF-145-09`
## Date · `2026-04-23` · Branche · `feat/SF-145-09-multi-domain-piece-types`

## Objectif
Corriger le biais droit-du-travail de l'enum `DocumentPieceType` posé par SF-145-01 : un avocat immigration qui uploade un titre de séjour obtient `AUTRE`, perte de sémantique. Étendre l'enum + adapter le prompt Sonnet pour qu'il propose uniquement les types pertinents selon `workspace.legalDomain`.

## Contexte / incident déclencheur
Challenge gouvernance 2026-04-23 : feature F-145 hardcoded implicitement sur le droit du travail (9 valeurs : CONTRAT, BULLETIN_PAIE, ATTESTATION…). Rien pour les 2 autres domaines V1 de l'application (immigration, famille). Ajout règle CLAUDE.md "Impact par domaine métier" systématique pour éviter la récurrence.

## Comportement ciblé
1. L'enum Java étendu (~25 valeurs au total) couvre les 3 domaines :
   - **Commun aux 3** : `PHOTO`, `LETTRE`, `EMAIL`, `SMS`, `ATTESTATION`, `PIECE_IDENTITE`, `CERTIFICAT_MEDICAL`, `AUTRE`
   - **Droit du travail (existants)** : `CONTRAT`, `BULLETIN_PAIE`
   - **Immigration (nouveaux)** : `TITRE_DE_SEJOUR`, `PASSEPORT`, `VISA`, `ACTE_NAISSANCE`, `AVIS_IMPOSITION`, `QUITTANCE_LOYER`, `PROMESSE_EMBAUCHE`, `RECEPISSE_PREFECTURE`, `DECISION_OQTF`, `RECOURS_CONTENTIEUX`, `ATTESTATION_HEBERGEMENT`
   - **Famille (nouveaux)** : `ACTE_MARIAGE`, `ACTE_NAISSANCE_ENFANT`, `JUGEMENT_DIVORCE`, `LIVRET_FAMILLE`, `JUSTIFICATIF_REVENUS`
2. Le prompt Sonnet reçoit une liste **filtrée** selon le `workspace.legalDomain` du case file du document. Un workspace immigration ne se voit pas proposer `BULLETIN_PAIE` ou `CONTRAT` (sauf s'il y a vraiment un contrat dans le doc).
3. Le service résout le domaine via `Document.caseFile.workspace.legalDomain` (déjà présent en DB).
4. Frontend : helpers `documentPieceTypeLabel` + `documentPieceTypeIcon` étendus pour les nouveaux types. Icônes Material cohérentes.

## Critères d'acceptation
- [ ] Enum `DocumentPieceType` étendu aux ~25 valeurs listées
- [ ] Nouveau helper `DocumentPieceType.applicableFor(legalDomain)` qui retourne le sous-ensemble pertinent
- [ ] `DocumentPieceDetectionService` : résout `legalDomain` via `extraction.document.caseFile.workspace.legalDomain`, injecte la liste filtrée dans `SYSTEM_PROMPT` (template dynamique)
- [ ] Frontend : `DocumentPieceType` union TS étendu, `documentPieceTypeLabel` + `documentPieceTypeIcon` couvrent tous les types
- [ ] Tests unitaires `DocumentPieceType.applicableFor` : 3 cas (travail / immigration / famille) retournent les bonnes listes
- [ ] Tests `DocumentPieceDetectionService` : prompt utilisé contient bien les types du domaine (pas de BULLETIN_PAIE pour immigration)
- [ ] Full suite backend + frontend verte

## Plan de test minimal
- U-01 : `applicableFor(DROIT_DU_TRAVAIL)` → contient CONTRAT, BULLETIN_PAIE ; ne contient pas TITRE_DE_SEJOUR
- U-02 : `applicableFor(IMMIGRATION)` → contient TITRE_DE_SEJOUR, RECEPISSE_PREFECTURE ; ne contient pas BULLETIN_PAIE
- U-03 : `applicableFor(FAMILLE)` → contient JUGEMENT_DIVORCE, LIVRET_FAMILLE ; ne contient pas BULLETIN_PAIE
- U-04 : les types communs (PHOTO, LETTRE, SMS, ATTESTATION…) présents dans les 3 listes
- U-05 : détection sur workspace immigration → prompt envoyé à Sonnet ne mentionne pas BULLETIN_PAIE
- U-06 (frontend) : helper `documentPieceTypeLabel('TITRE_DE_SEJOUR')` retourne "Titre de séjour"

## Tables / endpoints / composants impactés
### Backend
- `DocumentPieceType.java` : +16 valeurs, +méthode `applicableFor(LegalDomain)`
- `DocumentPieceDetectionService.java` : résolution domaine + template prompt dynamique
- `DocumentPieceTypeTest.java` (nouveau ou enrichi)
- `DocumentPieceDetectionServiceTest.java` enrichi

### Frontend
- `core/models/document.model.ts` : union `DocumentPieceType` élargie
- `documentPieceTypeLabel` + `documentPieceTypeIcon` : mapping étendu
- Styles SCSS `piece-chip[data-type="..."]` : étendus (cohérent avec palette existante, couleurs subtiles)

### Pas impacté
- Migration Liquibase : **aucune** — la colonne `document_pieces.type` est déjà `varchar(30)`. L'enum contraint en Java seulement.
- DTO, endpoint API : contracts préservés (juste plus de valeurs possibles)
- Tests existants : compatibles

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact direct | Adaptation nécessaire |
|---|---|---|
| **Droit du travail** (FR + BE) | Garde ses 9 types + ajout généraux (CERTIFICAT_MEDICAL, JUSTIFICATIF_REVENUS utiles pour prud'hommes) | Compat 100 % avec F-145 livré |
| **Immigration** (FR + BE) | Devient pleinement fonctionnel : TITRE_DE_SEJOUR, PASSEPORT, RECEPISSE_PREFECTURE, DECISION_OQTF etc. | Nouveaux types dédiés |
| **Famille** (FR + BE) | Devient pleinement fonctionnel : ACTE_MARIAGE, JUGEMENT_DIVORCE, LIVRET_FAMILLE etc. | Nouveaux types dédiés |

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-145 SF-145-01/02 | SF-145-09 enrichit sans casser (les enums existants restent valides) | Intégré |
| F-146 (futur) | Bénéficie automatiquement : la source précise citera les bons types pour chaque domaine | SF parallèle future |
| F-148 (futur) | Débloque la config `app.vision.trigger-types-by-domain` (ne peut pas la poser sans types 3 domaines) | Dépendance débloquée |
| Autres enums/listes hardcodés dans le code ? | À scanner : ExtractionFailureReason, JobType, AnalysisStatus — RAS, neutres par domaine | Intégré |

## Préoccupations transversales
- **Plans / limites** : aucun impact
- **Auth / Principal** : aucun impact
- **Workspace context** : dépendance croisée — le détecteur doit lire `workspace.legalDomain` (existant déjà, pas de nouvelle gate)
- **Navigation / routing** : aucun impact

## Hors scope
- Renommage / refonte des types existants (CONTRAT reste, pas de split en CONTRAT_TRAVAIL + CONVENTION)
- Persistance du domaine directement sur `document_pieces` (dérivé via FK existante, cohérent avec modèle actuel)
- Rejeu automatique des détections existantes (les pièces antérieures gardent leur classification ; les nouveaux uploads bénéficient du prompt enrichi)
- Détection spécialisée sous-type (ex: distinguer visa court séjour vs long séjour) — granularité trop fine pour V1
