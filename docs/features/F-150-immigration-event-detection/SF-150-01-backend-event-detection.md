# Mini-spec — F-150 / SF-150-01 Backend détection événements déclencheurs immigration

## Identifiant · `F-150 / SF-150-01`
## Date · `2026-04-23` · Branche · `feat/SF-150-01-backend-event-detection`

## Objectif
Permettre au pipeline IA de détecter les **événements factuels** dans un dossier immigration qui ouvrent un nouveau droit de séjour, et associer chaque événement à sa base légale CESEDA + au titre adapté suggéré.

## Contexte
Niveau 7 de la hiérarchie outils décisionnels (détection d'événement déclencheur). Équivalent immigration de F-DT-09-05 + F-DT-10 (détection type_rupture auto pour choisir l'outil approprié). Rattrape l'écart de parité entre les 3 domaines identifié 2026-04-23.

## Comportement nominal

### A — Référentiel `ImmigrationTriggerEvent`
Enum Java statique avec 10 événements déclencheurs + mapping base légale + titre suggéré :

| Code événement | Base légale CESEDA | Titre suggéré |
|---|---|---|
| `MARIAGE_RESSORTISSANT_FR` | Art. L.423-1 | CST_VPF |
| `PACS_RESSORTISSANT_FR` (>1 an) | Art. L.423-1 | CST_VPF |
| `NAISSANCE_ENFANT_FR` | Art. L.423-7 | CST_VPF (parent d'enfant français) |
| `DOCTORAT_OBTENU` | Art. L.421-14 | CARTE_PLURIANNUELLE (mention passeport-talent chercheur) |
| `CDI_OBTENU_SALARIE` | Art. L.421-1 | CST_SALARIE |
| `ENTREE_LEGALE_10ANS` | Art. L.426-1 | CARTE_RESIDENT |
| `VIOLENCES_CONJUGALES_CONSTATEES` | Art. L.423-3 | CST_VPF (protection conjoint victime) |
| `DEMANDE_ASILE_ACCORDEE_OFPRA` | Art. L.424-1 | CST_VPF (réfugié) |
| `ENFANT_NE_FR_13ANS_PRESENCE` | Art. L.423-23 | CARTE_RESIDENT (étranger né en FR) |
| `REGROUPEMENT_FAMILIAL_AUTORISE` | Art. L.434-7 | CST_VPF |

### B — Extension prompt IA immigration
Le prompt immigration (`LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION`) ajoute un nouveau champ à extraire :

```
"trigger_events" : tableau (éventuellement vide) des événements factuels 
identifiés dans le dossier qui ouvrent un nouveau droit de séjour. 
Chaque élément : {
  "event_code" : l'un des 10 codes exacts (MARIAGE_RESSORTISSANT_FR, 
    PACS_RESSORTISSANT_FR, NAISSANCE_ENFANT_FR, DOCTORAT_OBTENU, 
    CDI_OBTENU_SALARIE, ENTREE_LEGALE_10ANS, VIOLENCES_CONJUGALES_CONSTATEES, 
    DEMANDE_ASILE_ACCORDEE_OFPRA, ENFANT_NE_FR_13ANS_PRESENCE, 
    REGROUPEMENT_FAMILIAL_AUTORISE), null si hors liste ;
  "event_date" : date de l'événement YYYY-MM-DD (la date FACTUELLE, 
    ex. date du mariage, pas la date d'un raisonnement) ;
  "source_document" : nom du document support (ex. "acte_mariage.pdf") ;
  "justification" : phrase courte expliquant l'indice factuel 
    (ex. "Mariage célébré le 15/03/2025 mentionné dans l'acte n°127").
}
Ne JAMAIS inventer un événement non directement mentionné dans les pièces.
```

### C — DTO exposé au frontend
Nouveau record `ImmigrationTriggerEvent` exposé dans `CaseAnalysisResponse` :

```java
public record ImmigrationTriggerEvent(
    String eventCode,          // enum référentiel
    String eventLabel,          // libellé humain
    String eventDate,           // YYYY-MM-DD
    String sourceDocument,      // nom du doc support
    String justification,       // phrase courte
    String baseLegale,          // ex. "Art. L.423-1 CESEDA"
    String suggestedTitleCode,  // code titre cible (enum F-IM-05)
    String suggestedTitleLabel  // libellé titre cible
) {}
```

Champ `List<ImmigrationTriggerEvent> immigrationTriggerEvents` ajouté à `CaseAnalysisResponse`.

### D — Enrichissement à l'extraction
`CaseAnalysisResponse.from(...)` parse le tableau `trigger_events` depuis le JSON IA. Pour chaque événement valide, enrichit avec les données du référentiel (base légale + titre suggéré) via `ImmigrationTriggerEventReferential.resolve(eventCode)`. Fail-open : événement dont `event_code` hors enum → skippé silencieusement.

### E — Pas de rupture
Les dossiers sans événement déclencheur détecté → `immigrationTriggerEvents = []` (liste vide). Dossiers Travail / Famille → liste vide aussi (le prompt ne demande ce champ qu'en immigration). Rétrocompat totale.

## Critères d'acceptation
- [ ] Enum `ImmigrationTriggerEventCode` avec 10 valeurs
- [ ] Classe `ImmigrationTriggerEventReferential` mappant chaque code → (baseLegale, suggestedTitleCode, suggestedTitleLabel, libellé)
- [ ] Record `ImmigrationTriggerEvent` exposé dans `CaseAnalysisResponse`
- [ ] `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` étendu
- [ ] `CaseAnalysisResponse.from(...)` parse le tableau + fail-open sur codes inconnus
- [ ] Tests unitaires parsing : événement complet, plusieurs événements, tableau vide, code inconnu skippé, date invalide skippée
- [ ] Test intégration : prompt produit bien le champ `trigger_events`
- [ ] Full backend verte

## Plan de test minimal
- U-01 : parse 1 événement complet → record correctement enrichi avec base légale + titre suggéré
- U-02 : parse plusieurs événements → liste ordonnée
- U-03 : tableau vide / absent → liste vide, pas d'exception
- U-04 : code inconnu → skippé silencieusement
- U-05 : date invalide → événement skippé (ou eventDate null ?) — choix V1 : skippé pour garantir l'intégrité de la liste
- U-06 : référentiel.resolve() renvoie les bons mappings pour les 10 codes

## Tables / endpoints / composants impactés
### Backend
- `ImmigrationTriggerEventCode.java` (nouveau enum)
- `ImmigrationTriggerEventReferential.java` (nouveau service)
- `ImmigrationTriggerEvent.java` (nouveau record DTO)
- `CaseAnalysisResponse.java` (+champ + parsing)
- `LegalDomainPromptBuilder.java` (extension prompt immigration)
- Tests associés

### Pas impacté
- Frontend → SF-150-02
- Migration DB : aucune (liste stockée dans `analysis_result` JSON)
- F-IM-05 (arbre décisionnel type titre) : indépendant, le titre suggéré peut faire référence aux codes F-IM-05 mais sans couplage fort
- Autres domaines (Travail/Famille) : inchangés, prompt demande ce champ uniquement en immigration

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Droit de l'immigration** (FR) | 10 événements CESEDA français | Principal périmètre V1 |
| **Droit de l'immigration** (BE) | Code des étrangers belge | **Hors scope V1** — V1 couvre uniquement la France. Les 10 codes sont FR uniquement. BE à ajouter en SF-150-03 si feedback terrain (code des étrangers belge différent : carte A/B/C/D, permis unique, regroupement familial AR 08/10/1981). |
| **Droit du travail** / **Famille** | Non applicable | Le prompt immigration est seul à demander le champ `trigger_events` |

## Parité des domaines métier
**Niveau 7 — Détection d'événement déclencheur** :
- ✅ Travail : F-DT-09-05 (détection type_rupture) + F-DT-10 (scoring rupture conv)
- 🚧 Immigration : F-150 (cette SF)
- ❌ Famille : **F-152 à livrer** (détection événement → scoring divorce, couvert par la feuille de route 2026-04-23)

Cohérence : après livraison F-150 + F-152, les 3 domaines auront leur détection d'événement.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-IM-05 arbre décisionnel (type titre) | Complémentaire : F-IM-05 oriente depuis un questionnaire manuel, F-150 détecte automatiquement depuis les faits. `suggestedTitleCode` référence le même enum → cohérence | Intégré |
| F-IM-06 générateur recours | Indépendant : F-IM-06 génère un document de recours, F-150 détecte des événements d'ouverture de droit (demande initiale, pas recours) | Non applicable |
| F-IM-07 droit travail | Indépendant : F-IM-07 analyse les droits du titre ACTUEL, F-150 propose un nouveau titre | Non applicable |
| F-146 source précise | F-150 expose `sourceDocument` qui fait écho au `SourceRef` F-146. L'événement sera lié à une pièce précise via son nom de doc | Intégré |
| F-DT-09-05 détection type_rupture | Pattern inspirant : extraction d'un enum critique depuis le prompt + fallback Java si absent | Intégré (même pattern) |

## Préoccupations transversales
- **Auth / Principal** : aucun impact.
- **Workspace context** : le prompt immigration n'est sollicité que pour les workspaces `DROIT_IMMIGRATION` (filtre déjà en place).
- **Plans / limites** : aucun impact (toujours dans la même analyse).
- **Navigation / routing** : aucun nouveau chemin.

## Hors scope
- UI frontend (carte "Événement détecté" dans la synthèse) → **SF-150-02**
- Pré-remplissage automatique de F-IM-05 depuis un événement détecté → à évaluer en SF-IA-01-XX si besoin
- Codes événement Belgique → SF-150-03 si feedback terrain
- Détection de multiples événements concurrents sur un même dossier (prioritisation, conflits) → V1 : tous retournés, frontend laisse à l'avocat
