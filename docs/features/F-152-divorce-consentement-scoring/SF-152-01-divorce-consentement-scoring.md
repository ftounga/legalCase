# Mini-spec — F-152 / SF-152-01 Scoring divorce consentement mutuel famille

## Identifiant · `F-152 / SF-152-01`
## Date · `2026-04-23` · Branche · `feat/SF-152-01-divorce-consentement-scoring`

## Objectif
Évaluer la validité d'un projet de divorce par consentement mutuel avec un score 0-100 et un verdict de risque d'annulation. Pattern miroir de F-DT-08 (validité licenciement) et F-DT-10 (validité rupture conventionnelle).

## Contexte
Niveau 5 de la hiérarchie — rattrapage de parité entre domaines. Livré en parallèle de F-150/F-151 (immigration) pour aligner les 3 domaines.

## Comportement nominal

### A — Référentiel des critères (art. 229-1 à 229-4 Cciv)
7 critères évalués par l'IA :

| Code | Description | Source légale |
|---|---|---|
| `DC_MAJORITE` | Les deux époux sont majeurs (pas de mineur émancipé) | Art. 229-1 Cciv |
| `DC_CONSENTEMENT_LIBRE` | Absence de vice (pression, dol, violences conjugales récentes non purgées) | Art. 1130-1144 Cciv + 233 |
| `DC_CONVENTION_EQUITABLE` | Convention ne lèse manifestement aucun époux (liquidation équilibrée, pension proportionnée) | Art. 229-3 Cciv |
| `DC_ENFANT_MINEUR_ENTENDU` | Si enfant mineur ≥ âge discernement, il a été informé du droit à être entendu | Art. 388-1 Cciv + 229-2 |
| `DC_DELAI_REFLEXION_15J` | Délai de réflexion de 15 jours calendaires entre réception de la convention et signature | Art. 229-4 Cciv |
| `DC_NOTAIRE_DEPOT` | Convention déposée chez un notaire pour obtention de la date certaine | Art. 229-1 Cciv |
| `DC_INDEPENDANCE_AVOCATS` | Chaque époux a son propre avocat (pas d'avocat unique pour les deux) | Art. 229-1 Cciv |

### B — Extraction IA
Extension prompt famille (`LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`, à créer si absent) : champ `divorce_consentement_validity_detection: {detections: Map<code, {reponse: OUI|NON|INCONNU, justification}>}`.

### C — Calcul du score
`DivorceConsentementValidityAnalyzer` :
- Score = (nombre de critères `OUI` / 7) × 100, arrondi entier
- Critères `INCONNU` traités comme manquants (pénalisent le score)
- Verdict :
  - score ≥ 85 : `VALIDE`
  - score 50-84 : `RISQUE_MOYEN`
  - score < 50 : `RISQUE_ELEVE_NULLITE`

### D — DTO exposé au frontend
Record `DivorceConsentementValidityDetection(Map<String, DetectedAnswer> detections)` dans `CaseAnalysisResponse` (même pattern que Licenciement/RuptureConv).

Record `DivorceConsentementScoring(int score, String verdict, List<String> criteresValides, List<String> criteresNonValides, List<String> criteresInconnus)` également exposé (pré-calculé côté backend au moment du rendu).

### E — Frontend
Nouveau composant `DivorceConsentementScoringSectionComponent` affiché dans la synthèse famille quand détections non vides. Miroir visuel du composant existant `licenciement-section` (jauge SVG + checklist critères + badge verdict).

## Critères d'acceptation
- [ ] Set `DIVORCE_CONSENTEMENT_CRITERE_CODES` (7 codes)
- [ ] Record `DivorceConsentementValidityDetection` dans `CaseAnalysisResponse`
- [ ] Record `DivorceConsentementScoring` exposé
- [ ] Analyzer qui calcule score + verdict
- [ ] Extension prompt IA famille
- [ ] Parseur fail-open
- [ ] Frontend interface TS + composant + intégration synthesis
- [ ] Tests backend (parser + analyzer)
- [ ] Tests frontend composant
- [ ] Full suites vertes

## Plan de test minimal
**Backend :**
- U-01 : parse détection complète avec 7 critères OUI → score 100, verdict VALIDE
- U-02 : 3 OUI / 4 NON → score 43, verdict RISQUE_ELEVE_NULLITE
- U-03 : critère code inconnu → skippé silencieusement
- U-04 : INCONNU traité comme manquant dans le calcul
- U-05 : détection vide/absente → verdict null

**Frontend :**
- U-06 : affiche la jauge avec le bon score
- U-07 : affiche la checklist des 7 critères avec statut
- U-08 : badge verdict coloré (vert/orange/rouge)
- U-09 : section cachée si détection absente

## Tables / endpoints / composants impactés
### Backend
- `CaseAnalysisResponse.java` (+`DivorceConsentementValidityDetection`, `DivorceConsentementScoring`, +`DIVORCE_CONSENTEMENT_CRITERE_CODES`, +parseur)
- `DivorceConsentementValidityAnalyzer.java` (nouveau)
- `LegalDomainPromptBuilder.java` (extension prompt famille — ajouter si absent, sinon étendre IMMIGRATION_INSTRUCTION pattern adapté)

### Frontend
- `core/models/case-analysis.model.ts` (+interface)
- `case-files/divorce-consentement-scoring-section/` (nouveau composant)
- `case-files/synthesis/` (intégration)

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact |
|---|---|
| **Famille FR** | 7 critères art. 229 Cciv |
| **Famille BE** | Hors scope V1. Le divorce par consentement mutuel belge (art. 1254-1310 CJ + dépôt tribunal famille) a des critères différents. Ajouter en SF-152-02 si feedback terrain. |
| **Travail / Immigration** | Non applicable |

## Parité des domaines métier
**Niveau 5 — Scoring / analyse validité** :
- ✅ Travail : F-DT-08 (licenciement) + F-DT-10 (rupture conv)
- ✅ Immigration : couvert conceptuellement par F-151 (risque par scénario)
- 🚧 Famille : F-152 (cette SF)

Après livraison : les 3 domaines ont un niveau 5.

## Analyse de cohérence transversale
- Pattern F-DT-08/F-DT-10 : réutilisé directement (DetectedAnswer + Map, analyzer 0-100, verdict 3 paliers)
- F-FA-07 checklist divorce (étapes procédurales) : complémentaire, non redondant — F-FA-07 suit l'avancement procédural, F-152 évalue la validité juridique

## Préoccupations transversales
Aucune.

## Hors scope
- Belgique (SF-152-02 plus tard)
- Divorce pour faute / altération du lien conjugal / acceptation principe (autres types de divorce FR non couverts par cette SF)
