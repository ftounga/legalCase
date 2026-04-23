# SF-IM-07-04 — Scinder CARTE_PLURIANNUELLE + ajouter CST_VPF_CONJOINT_FR

## Objectif
Corriger 2 incohérences de F-IM-07 droit au travail remontées sur le
dossier Chen Wei (doctorant, carte pluriannuelle Étudiant-Recherche) :
1. `CARTE_PLURIANNUELLE` retourne "OUI plein droit" sans distinguer ses
   4 sous-motifs juridiques à régime de travail différent.
2. `CST_VPF_CONJOINT_FR` (L.423-1), utilisé par F-IM-01 checklist, n'existe
   pas côté F-IM-07 → impossible à sélectionner pour simuler le gain d'un
   passage conjoint de Français.

## Comportement nominal
**Nouveaux codes IMMIGRATION_WORK_RIGHTS (FRANCE)** :

| Code | Droit travail | Conditions principales | Base CESEDA |
|------|---------------|-----------------------|-------------|
| `CARTE_PLURIANNUELLE` (existant, requalifié) | CONDITIONNEL | "Dépend du motif — sélectionnez le sous-type" | L.421-9 |
| `CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE` | CONDITIONNEL | **964 h/an (60 %)**, pas d'autorisation préalable | L.421-9 + L.422-1 |
| `CARTE_PLURIANNUELLE_SALARIE` | OUI | Plein droit, tout employeur | L.421-9 |
| `CARTE_PLURIANNUELLE_PASSEPORT_TALENT` | OUI | Plein droit dans l'activité mentionnée | L.421-9 à L.421-22 |
| `CARTE_PLURIANNUELLE_VPF` | OUI | Plein droit sans restriction | L.421-9 + L.423-1 |
| `CST_VPF_CONJOINT_FR` | OUI | Plein droit dès délivrance | **L.423-1** |

Le sélecteur F-IM-07 (frontend) expose les 6 codes supplémentaires en FR
avec des labels explicites.

## Pourquoi
- L'invariant "outil décisionnel = une situation métier"
  (`feedback_decision_tools_one_per_situation`) interdit qu'une entrée du
  référentiel mélange plusieurs régimes juridiques. La carte pluriannuelle
  est un **conteneur** qui regroupe 4 régimes différents — chacun doit
  avoir sa propre entrée.
- Cas Chen Wei (doctorant, pluriannuelle Étudiant-Recherche) : l'outil
  affichait "OUI plein droit" alors que Chen est en réalité plafonné à
  964 h/an. Bug bloquant pour conseiller un passage L.423-1 (plein temps).
- F-IM-01 et F-IM-07 doivent connaître les mêmes codes — sinon
  `inferChecklistType` renvoie `CST_VPF_CONJOINT_FR` mais l'outil work
  rights ne peut pas le simuler.

## Cas d'erreur
- **Dossier existant stocké en `CARTE_PLURIANNUELLE`** : continue de
  fonctionner, affiche le nouveau message "Sélectionnez le sous-type
  précis". L'avocat requalifie manuellement → stocke un sous-type précis.
- **Code inconnu au référentiel** : `getWorkRight()` retourne null →
  400 "Titre inconnu pour ce pays".

## Critères d'acceptation
- [x] Migration Liquibase 102 : UPDATE générique + 5 INSERT sous-types
- [x] UUIDs 301-305 (range libre, pas de collision avec 067)
- [x] `ImmigrationWorkRightReferentiel` (Java fallback) aligné sur la DB
- [x] `FR_TITRE_CODES` (frontend whitelist) étendu aux 5 nouveaux codes
- [x] Sélecteur `titresFrance` expose les 5 nouvelles options
- [x] Labels explicites dans le sélecteur (Étudiant-Recherche / Salarié / Passeport Talent / VPF)
- [x] Descriptions riches (colonne `description` obligatoire SF-140-03)
- [x] 42 tests backend verts (dont `ImmigrationWorkRightControllerIT` 8 + `LegalReferentialDescriptionIntegrityIT` 2)
- [x] 18 tests frontend `immigration-work-right-section` verts
- [x] Build frontend OK

## Plan de test
- **Unit/Integration** : tests existants non régressés (aucun test modifié)
- **Intégration (staging — dossier Chen Wei)** :
  1. Ouvrir F-IM-07 → sélectionner `CARTE_PLURIANNUELLE` générique →
     réponse "CONDITIONNEL, sélectionnez le sous-type"
  2. Sélectionner `CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE` → réponse
     **"964 h/an (60 %)"**
  3. Sélectionner `CST_VPF_CONJOINT_FR` → réponse **"plein droit"**
  4. Comparaison visible du gain du changement de statut L.423-1

## Hors périmètre
- **Auto-détection du sous-motif** : pas d'IA qui lit la mention
  "Étudiant-Recherche" sur la carte pour pré-sélectionner — F-IM-07 reste
  un simulateur (feedback memory `decision_tools_are_simulators`).
- **Belgique** : pas d'équivalent à scinder — les cartes A/B/C belges
  sont déjà distinctes par motif dans le référentiel.
- **F-IM-01 checklist** : déjà distingue `CST_VPF_CONJOINT_FR` depuis
  migration 101. Cette SF ne touche pas la checklist.
- **CST_VPF** (générique) conservé pour les dossiers legacy qui le
  référencent — le code `CST_VPF_CONJOINT_FR` est additif.

## Tables / endpoints / composants impactés
**Backend** :
- `legal_referentials` : +5 entrées, 1 UPDATE (migration 103)
- `ImmigrationWorkRightReferentiel` : +5 entrées fallback
- Aucun endpoint modifié (DB-first inchangé)

**Frontend** :
- `immigration-work-right-section.component.ts` :
  - `FR_TITRE_CODES` whitelist étendue (5 codes)
  - `titresFrance` sélecteur étendu (5 options)

## Impact par domaine métier
Spécifique **DROIT_IMMIGRATION** / **FRANCE**. Belgique non impactée
(régimes déjà distincts). Droit du travail et Famille non impactés.

## Analyse de cohérence transversale
- **Invariant "un outil = une situation"** ✅ respecté : chaque sous-motif
  = une entrée distincte.
- **Cohérence F-IM-01 ↔ F-IM-07** ✅ : `CST_VPF_CONJOINT_FR` maintenant
  présent dans les 2 outils.
- **Autres outils décisionnels scannés** :
  - F-IM-05 arbre décisionnel titre : produit des codes cible, n'utilise
    pas directement le référentiel work rights. Aucun changement requis.
  - F-IM-06 recours : indépendant (bases légales différentes). Pas d'impact.
- **Règles DB-first + description** (F-140 SF-140-03) ✅ : chaque INSERT
  porte une colonne `description` non vide.
- **Règles Java vs DB alignment** (CLAUDE.md) ✅ : fallback Java mis à
  jour en même temps que la migration.

## Nouveau pattern UI ou service partagé
Aucun — extension d'un pattern existant (entrées référentiel +
sélecteur dropdown). Pas de composant partagé à créer.
