# Mini-spec — F-172 / SF-172-01 — Backend : élargissement détection événements immigration FR aux faits imminents

## Identifiant

`F-172 / SF-172-01`

## Feature parente

`F-172` — Élargissement détection événements déclencheurs immigration FR aux faits imminents documentés

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-172-01-backend-imminent-events`

---

## Objectif

Modifier le prompt IA et le référentiel des événements déclencheurs immigration FR pour que l'IA inclue désormais explicitement les **événements imminents documentés** (soutenance de thèse programmée, mariage publié des bans, CDI signé non commencé, naissance/reconnaissance imminente) en plus des faits révolus, et lever le biais conservateur du prompt.

---

## Comportement attendu

### Cas nominal

1. L'avocat upload un dossier contenant une attestation de soutenance de thèse programmée le 15/10/2026 (futur).
2. Le pipeline IA tourne et appelle Sonnet sur le prompt enrichi.
3. La sortie JSON `trigger_events` contient désormais `{ event_code: "DOCTORAT_OBTENU", event_date: "2026-10-15", source_document: "attestation-these-paris-saclay.pdf", justification: "Soutenance programmée 15/10/2026 + Université Paris-Saclay + L.421-14 CESEDA" }` **de manière stable** (~95-100% au lieu de 50%).
4. Idem pour `MARIAGE_RESSORTISSANT_FR` quand le document mentionne "publication des bans" sans célébration encore. Idem pour `CDI_OBTENU_SALARIE` quand le contrat est signé mais date de prise de poste future. Idem pour `NAISSANCE_ENFANT_FR` quand reconnaissance de paternité signée avant naissance.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun document mentionnant un événement déclencheur | `trigger_events: []` (inchangé) |
| Document mentionne une intention vague sans preuve documentaire (ex. "le client envisage de se marier") | **Pas** d'événement détecté (la règle "preuve documentaire requise" est maintenue) |
| Pour les 6 codes factuels par nature (PACS communauté >1an, 10 ans présence, violences constatées, asile accordé, enfant 13 ans, regroupement autorisé) | Comportement inchangé — ces codes restent factuels (pas de version "imminente") |

---

## Contrat avec le frontend

Le frontend consomme déjà `immigrationTriggerEvents` dans `CaseAnalysisResponse` (champ existant). Aucun changement de schéma. Les nouveaux libellés `event_label` plus longs nécessitent que le frontend ait assez de place pour les afficher (à valider visuellement après merge).

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres pays** : Belgique. Les équivalents L.30bis (regroupement BE) / L.40bis (cohabitant UE) ont leur propre logique métier traitée par F-IM-14 et hors périmètre de cette SF — l'élargissement reste FR uniquement.
- **Autres domaines** : Travail / Famille — non applicables. Le mécanisme `trigger_events` est immigration-spécifique (F-150).
- **Autres outils décisionnels** : F-150 (où vit ce mécanisme) — modifié dans cette SF. Pas d'autre outil avec un mécanisme analogue à harmoniser.
- **Pattern UI/service partagé** : non applicable — pas de nouveau composant ni service.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Prompt `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` | Oui | Modifié dans cette SF |
| Référentiel `ImmigrationTriggerEventReferential` (4 event_label) | Oui | Modifié dans cette SF |
| Tests UT/IT | Oui | Modifiés/ajoutés dans cette SF |
| Frontend (badge "Événement programmé") | Oui | Couvert par SF-172-02 (parallèle) |
| Domaines / pays autres | Non | Justification : immigration FR uniquement par construction |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette SF
- [x] SF parallèle SF-172-02 pour le frontend
- [x] Backlog futur : extension Belgique non envisagée (F-IM-14 a sa propre logique)

---

## Impact par domaine métier

**Immigration France uniquement.** Les 6 codes factuels par nature (PACS, ENTREE_LEGALE_10ANS, VIOLENCES_CONJUGALES_CONSTATEES, DEMANDE_ASILE_ACCORDEE_OFPRA, ENFANT_NE_FR_13ANS_PRESENCE, REGROUPEMENT_FAMILIAL_AUTORISE) restent inchangés — leur sémantique est intrinsèquement révolue. Belgique : non concernée par cette SF (équivalents traités via F-IM-14). Travail / Famille : non concernés.

---

## Critères d'acceptation

- [ ] **C1** — Le prompt `LegalDomainPromptBuilder.java:196` ne contient **plus** la phrase `"c'est le cas attendu pour la plupart des dossiers de renouvellement simple"`
- [ ] **C2** — Le prompt contient explicitement une instruction d'inclusion des événements imminents documentés (soutenance programmée, bans publiés, CDI signé non commencé, reconnaissance imminente). Texte exact à appliquer : voir section "Texte exact prompt" ci-dessous
- [ ] **C3** — Les 4 `event_label` correspondants dans `ImmigrationTriggerEventReferential.java` sont mis à jour :
  - `DOCTORAT_OBTENU` → `"Doctorat obtenu ou soutenance programmée en France"`
  - `MARIAGE_RESSORTISSANT_FR` → `"Mariage célébré ou publié avec un ressortissant français"`
  - `CDI_OBTENU_SALARIE` → `"CDI signé (même non commencé)"`
  - `NAISSANCE_ENFANT_FR` → `"Naissance ou reconnaissance imminente d'un enfant français"`
- [ ] **C4** — Les 6 autres event_label (PACS_RESSORTISSANT_FR, ENTREE_LEGALE_10ANS, VIOLENCES_CONJUGALES_CONSTATEES, DEMANDE_ASILE_ACCORDEE_OFPRA, ENFANT_NE_FR_13ANS_PRESENCE, REGROUPEMENT_FAMILIAL_AUTORISE) sont **inchangés**
- [ ] **C5** — Tous les tests existants restent verts (pas de régression sur les autres champs `CaseAnalysisResponse`)
- [ ] **C6** — Tests UT du référentiel : assertion sur les 4 nouveaux libellés
- [ ] **C7** — Test unitaire ou d'intégration vérifiant que le prompt construit ne contient plus la phrase de biais (chaîne exacte) et contient la nouvelle règle d'inclusion (mots-clés "imminent" + "documenté")

---

## Texte exact prompt à appliquer

Dans `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java`, dans la définition du champ `"trigger_events"` (ligne ~196 selon la dernière inspection), remplacer la dernière phrase :

> ❌ Ancien : `"N'inventer AUCUN événement non directement mentionné dans les pièces. Tableau vide [] si aucun événement déclencheur détecté — c'est le cas attendu pour la plupart des dossiers de renouvellement simple."`

Par :

> ✅ Nouveau : `"N'inventer AUCUN événement non directement mentionné dans les pièces. Inclure aussi les événements imminents documentés : soutenance de thèse programmée avec convention d'accueil ; mariage publié des bans ; CDI signé même non commencé ; naissance déclarée et reconnaissance de paternité française. La preuve documentaire (date + acte/attestation officielle) suffit, l'événement n'a pas besoin d'être révolu. Tableau vide [] si aucun événement déclencheur détecté."`

---

## Périmètre

### Hors scope

- Frontend (badge "Événement programmé") — couvert par SF-172-02
- Extension Belgique
- Ajout de nouveaux codes `DOCTORAT_IMMINENT` séparés (le label élargi suffit)
- Confidence score `HIGH/MEDIUM/LOW` (plus tard si besoin)

---

## Technique

### Fichiers modifiés

- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` — modification du prompt IMMIGRATION_INSTRUCTION sur le champ `trigger_events`
- `backend/src/main/java/fr/ailegalcase/immigration/ImmigrationTriggerEventReferential.java` — modification de 4 `EventDefinition.eventLabel`

### Tables impactées

Aucune. La valeur `event_code` reste identique (l'enum `ImmigrationTriggerEventCode` n'est pas modifié). Seul le libellé humain et le prompt changent.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationTriggerEventReferentialTest` — assertion sur les 4 nouveaux libellés (`resolve(DOCTORAT_OBTENU).eventLabel()` etc.)
- [ ] `LegalDomainPromptBuilderTest` — assertion absence de la phrase de biais + présence des mots-clés "imminent" + "documenté" dans le prompt construit pour `domainSpecificInstruction("DROIT_IMMIGRATION")`

### Tests d'intégration

- [ ] Pas d'IT spécifique nécessaire — le pipeline IA n'est pas testé end-to-end (asynchrone + appel LLM réel)
- [ ] Les IT existants sur `CaseAnalysisResponse.extractImmigrationTriggerEvents` restent verts (lecture du JSON inchangée)

### Isolation workspace

Non applicable — la SF ne touche pas l'accès aux données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing — non
- [x] Aucune préoccupation transversale — modifications strictement métier (prompt + libellés)

### Smoke tests E2E

- Aucun smoke test ne teste actuellement l'extraction des `trigger_events` (pas de mock IA dans les smoke). Pas de régression attendue.

---

## Dépendances

### Subfeatures bloquantes

Aucune. Démarrable immédiatement en parallèle avec SF-172-02.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Décision** : ne pas créer de nouveaux codes `DOCTORAT_IMMINENT` séparés. Élargir le label suffit, évite la fragmentation du référentiel et la surcharge prompt.
- **Décision** : la règle "preuve documentaire" reste obligatoire (acte/attestation + date). Pas de fausse positive sur intention vague.
- **Note** : la stabilité de détection (~95-100%) ne peut être validée que par observation post-merge sur le dossier Chen ré-uploadé. Tests unitaires garantissent la conformité du prompt et du référentiel — la stabilité IA elle-même est un critère produit, pas un critère testable unitairement.
