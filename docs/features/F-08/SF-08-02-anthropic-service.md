# Mini-spec — F-08 / SF-08-02 — AnthropicService

---

## Identifiant

`F-08 / SF-08-02`

## Feature parente

`F-08` — Analyse IA — chunk

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-08-anthropic-service`

---

## Objectif

Intégrer le SDK Anthropic Java et exposer une méthode `analyzeChunk()` qui envoie le texte d'un chunk au modèle Claude et retourne le résultat JSON de l'analyse.

---

## Comportement attendu

### Cas nominal

`AnthropicService.analyzeChunk(String chunkText)` envoie le texte au modèle `claude-sonnet-4-6` avec un prompt système orienté droit du travail. Le modèle retourne une analyse structurée en JSON. La méthode retourne ce JSON sous forme de `String`.

### Format du JSON retourné (cible)

```json
{
  "faits": ["..."],
  "points_juridiques": ["..."],
  "risques": ["..."],
  "questions_ouvertes": ["..."]
}
```

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Texte vide ou null | `IllegalArgumentException` levée avant appel API |
| Erreur API Anthropic (timeout, 429, 500) | `AnthropicApiException` propagée — la gestion retry est dans SF-08-03 |

---

## Critères d'acceptation

- [ ] Dépendance `com.anthropic:anthropic-java` ajoutée dans `pom.xml`
- [ ] `anthropic.api-key` et `anthropic.model` configurables via `application.yml` / variables d'environnement
- [ ] `AnthropicService.analyzeChunk(String chunkText)` appelle l'API Claude et retourne un String JSON
- [ ] Le prompt système est défini en constante dans le service (droit du travail, FR)
- [ ] Texte null ou vide → `IllegalArgumentException` avant appel API
- [ ] Test unitaire : mock du client Anthropic → vérification que le prompt et le texte sont transmis correctement
- [ ] Test unitaire : texte vide → `IllegalArgumentException`
- [ ] `AnthropicService` absent du profil `dev` — l'API key n'est pas requise pour les tests

---

## Périmètre

### Hors scope (explicite)

- Publication dans RabbitMQ (SF-08-03)
- Persistance du résultat (SF-08-03)
- Retry en cas d'échec API (SF-08-03)
- Prompt sémantique V2 (chunking par paragraphe)

---

## Technique

### Dépendance

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.8.0</version>
</dependency>
```

### Configuration (application.yml)

```yaml
anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-sonnet-4-6
```

### Prompt système (V1 — droit du travail)

```
Tu es un assistant juridique expert en droit du travail français.
Analyse le texte suivant extrait d'un document juridique.
Identifie et retourne en JSON : les faits, les points juridiques, les risques potentiels, et les questions ouvertes.
Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
```

### Composants créés

- `AnthropicService` — `fr.ailegalcase.analysis`
- `AnthropicProperties` — `@ConfigurationProperties("anthropic")`

### Tables impactées

Aucune — SF-08-02 ne persiste rien.

---

## Plan de test

### Tests unitaires

- [ ] `AnthropicService` — mock client : vérification prompt système + texte transmis, retour JSON attendu
- [ ] `AnthropicService` — texte vide → `IllegalArgumentException`
- [ ] `AnthropicService` — texte null → `IllegalArgumentException`

### Tests d'intégration

- [ ] Non applicable — appel API externe mocké en test

### Isolation workspace

- [ ] Non applicable — pas d'accès aux données workspace

---

## Dépendances

### Subfeatures bloquantes

- F-08 / SF-08-01 — infrastructure chunk_analyses — statut : done

### Questions ouvertes impactées

- Provider LLM : Anthropic — tranché le 2026-03-17

---

## Notes et décisions

- `@Profile("local")` sur `AnthropicService` pour ne pas requérir l'API key en tests (profil `dev`)
- Le modèle `claude-sonnet-4-6` est configurable via `anthropic.model` — changeable sans recompilation
- Le JSON retourné est stocké tel quel dans `analysis_result` (TEXT) — pas de parsing côté Java en V1
- `AnthropicProperties` utilise `@ConfigurationProperties` pour bénéficier du binding Spring et de la validation
