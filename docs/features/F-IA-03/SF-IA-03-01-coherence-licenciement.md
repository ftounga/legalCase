# Mini-spec — F-IA-03 / SF-IA-03-01 Contrôle de cohérence sur F-DT-08

## Identifiant

`F-IA-03 / SF-IA-03-01`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-13

## Branche Git

`feat/SF-IA-03-01-coherence-licenciement`

---

## Objectif

Lorsque l'avocat coche une réponse dans la grille F-DT-08 qui contredit la détection IA déjà exposée par SF-IA-01-03, afficher une alerte visible (warning ou blocker selon la criticité du critère) avec la justification IA, sans empêcher la sauvegarde.

---

## Comportement attendu

### Cas nominal

1. Le composant `LicenciementSectionComponent` reçoit `aiData: LicenciementValidityDetection` (déjà exposé par SF-IA-01-03).
2. Pour chaque critère affiché, le composant compare la réponse avocat avec `aiData.detections[code].reponse`.
3. Si la détection IA est `OUI` ou `NON` et que la réponse avocat est différente :
   - Critère **bloquant** (`bloquant=true` côté backend) → niveau `blocker` (badge rouge "⚠ Incohérence forte").
   - Critère **non bloquant** → niveau `warning` (badge orange "⚠ Incohérence").
4. Si la détection IA est `INCONNU` ou absente → aucune alerte.
5. Si la réponse avocat est `INCONNU` (valeur initiale) → aucune alerte (l'avocat n'a pas tranché).
6. L'alerte affiche : libellé court + tooltip / panneau dépliable avec la valeur détectée par l'IA + sa justification.
7. L'avocat peut malgré tout sauvegarder (l'alerte est informative, pas bloquante).
8. Un compteur global s'affiche au-dessus de la grille : "X incohérences détectées (Y bloquantes)" si > 0.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `aiData` absent (analyse non lancée) | Aucune alerte, comportement actuel inchangé |
| Détection IA pour un critère absent du référentiel courant | Ignorée silencieusement |
| Justification IA absente ou vide | Tooltip affiche "Aucune justification fournie" |
| Détection IA contient une réponse non normalisée (déjà filtrée backend) | Traitée comme `INCONNU` côté frontend par sécurité |

---

## Critères d'acceptation

- [ ] Quand la détection IA et la réponse avocat divergent sur un critère bloquant → badge rouge visible à côté du critère.
- [ ] Quand la divergence est sur un critère non bloquant → badge orange.
- [ ] Quand l'IA est `INCONNU` ou absent → aucune alerte sur ce critère.
- [ ] Quand l'avocat n'a pas répondu (`INCONNU`) → aucune alerte (pas de divergence à signaler).
- [ ] Le tooltip / panneau de l'alerte affiche la réponse IA et sa justification.
- [ ] Un compteur global "X incohérences (Y bloquantes)" s'affiche au-dessus de la grille si > 0, masqué sinon.
- [ ] L'alerte ne bloque pas l'analyse — l'avocat peut cliquer "Analyser" malgré la divergence.
- [ ] Les alertes se mettent à jour réactivement quand l'avocat change une réponse.
- [ ] Couvre indistinctement FR et BE (test sur les deux jeux de critères).
- [ ] Tests unitaires frontend verts sur la matrice (concordance, divergence bloquante, divergence non bloquante, IA INCONNU, avocat INCONNU, aiData absent).

---

## Périmètre

### Hors scope (explicite)

- **Sources autres que la détection IA déjà exposée par SF-IA-01-03** (checklist procédurale F-96, pièces manquantes, questions IA interactives, citations documents) → traitées dans SF-IA-03-02 et suivantes.
- **Extension aux autres outils** (F-DT-07, F-DT-09, F-FA-*, F-IM-*) → SF-IA-03-03 et suivantes.
- **Pondération inter-sources** : non pertinent ici puisqu'on a une seule source.
- **Justification obligatoire en cas de blocker** : pas dans cette subfeature, sera utile quand on aura plusieurs sources de haute confiance.
- **Persistance des alertes** : les alertes sont calculées à la volée, jamais stockées.
- **Modification du calcul de score F-DT-08** : aucun impact, score reste basé sur les réponses avocat.

---

## Valeurs initiales

Aucune nouvelle entité — la subfeature est purement présentationnelle.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| Réponse comparée avocat | déjà existant | `OUI` / `NON` / `INCONNU` | inchangé |
| Réponse détectée IA | déjà existant | `OUI` / `NON` / `INCONNU` | inchangé (normalisée backend) |

---

## Technique

### Endpoint(s)

Aucun. Tout se passe côté frontend en consommant la donnée déjà exposée par `GET /case-analysis`.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable** — purement frontend.

### Composants Angular

- `LicenciementSectionComponent` :
  - Ajouter un computed `coherenceAlerts: Signal<Map<critereCode, AlertLevel>>` calculant les divergences en croisant `criteresForm()` et `aiData?.detections`.
  - Ajouter un computed `alertsSummary: Signal<{total: number, blockers: number}>`.
  - Pour distinguer bloquant/non-bloquant côté front, étendre `CritereForm` avec un champ `bloquant: boolean` initialisé depuis le référentiel local existant (déjà conforme aux poids backend).
  - Affichage : badge à droite du label + tooltip contenant la justification IA.
  - Bandeau récap : compteur visible si total > 0.

### Référentiel local frontend

Le composant possède déjà `criteresReferentiel`. Ajouter le champ `bloquant: boolean` sur chaque critère (valeurs alignées avec `LicenciementCritereReferentiel.ALL` côté backend).

| Critère | bloquant |
|---|---|
| FR_CONVOCATION | true |
| FR_ENTRETIEN | true |
| FR_DELAI_NOTIFICATION | false |
| FR_MOTIVATION | true |
| FR_MOTIF_REEL | true |
| FR_PROCEDURE_DISCIPLINAIRE | false |
| FR_ORDRE_LICENCIEMENT | false |
| BE_NOTIFICATION | true |
| BE_PREAVIS | true |
| BE_MOTIVATION | true |
| BE_AUDITION | false |
| BE_NON_DISCRIMINATION | true |
| BE_PROTECTION_SPECIALE | true |
| BE_INDEMNITE_MANIFESTE | false |

---

## Plan de test

### Tests unitaires frontend

- [ ] Concordance : avocat=OUI / IA=OUI → aucune alerte.
- [ ] Divergence bloquante : avocat=NON / IA=OUI sur critère bloquant → niveau `blocker`.
- [ ] Divergence non bloquante : avocat=NON / IA=OUI sur critère non bloquant → niveau `warning`.
- [ ] IA INCONNU : avocat=OUI / IA=INCONNU → aucune alerte.
- [ ] Avocat INCONNU : avocat=INCONNU / IA=NON → aucune alerte.
- [ ] `aiData` absent → aucune alerte, comportement inchangé.
- [ ] Compteur global : 2 divergences dont 1 bloquante → `{total:2, blockers:1}`.
- [ ] Réactivité : changement de réponse avocat met à jour l'alerte (computed reévalué).
- [ ] BE : matrice équivalente sur critères BE.

### Tests d'intégration

- [ ] Smoke E2E : non applicable (purement composant, pas de flux serveur ajouté).

### Isolation workspace

- [x] Non applicable — la subfeature n'introduit aucun accès données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension localisée d'un seul composant Angular.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `LicenciementSectionComponent` | logique d'affichage rallongée — ne doit pas casser les flux existants (loadExisting, analyze, override) | tests existants déjà verts conservés |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-01-03` (Done, mergée 2026-04-13) — fournit la donnée `licenciementValidityDetection`.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi commencer par la seule source IA detection** : elle est déjà disponible (SF-IA-01-03) ; le moteur peut être livré sans rien d'autre. Les sources F-96, pièces manquantes, etc. demanderont du mapping sémantique critère ↔ source — un travail en soi qui mérite ses propres subfeatures.
- **Pourquoi ne pas bloquer l'analyse** : règle produit — l'IA suggère, l'avocat décide. Une alerte ne doit jamais empêcher la sauvegarde, sinon elle devient un faux positif coûteux. Le caractère "blocker" est uniquement visuel.
- **Pourquoi mapper `bloquant` côté front** : le backend connaît déjà la pondération (`LicenciementCritereReferentiel.ALL`) mais ne l'expose pas via l'API. Plutôt que d'étendre la réponse pour transporter le boolean, on duplique l'info statique côté front (les poids sont stables). Si à l'avenir on veut piloter la pondération depuis la base via `legal_referentials`, on devra exposer le champ — à reconsidérer alors.
