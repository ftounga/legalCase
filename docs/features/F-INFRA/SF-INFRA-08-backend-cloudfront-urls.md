# Mini-spec — F-INFRA / SF-INFRA-08 — Backend Spring Boot : URLs de téléchargement via CloudFront

> **Étape 0 (cadrage cohérence)** : EXEMPTÉE — SF infrastructure pure, pas de workflow utilisateur visible nouveau (le flux UI `GET /download → 302 → blob URL` reste strictement identique, seul change l'hôte cible de la redirection).
> **Étape 0 bis (cohérence écran)** : EXEMPTÉE — pas d'impact écran.

---

## Identifiant

`F-INFRA / SF-INFRA-08`

## Feature parente

`F-INFRA` — Travaux d'infrastructure AWS (cf. `docs/BACKLOG_INFRA.md` repo `legalcase-infra`).

## Statut

`in-progress`

## Date de création

2026-05-20

## Branche Git

`feat/SF-INFRA-08-backend-cloudfront-urls`

---

## Objectif

Brancher l'application Spring Boot sur la distribution CloudFront déployée par SF-INFRA-03 (côté `legalcase-infra`) pour que les téléchargements de documents passent par le CDN au lieu de tirer directement sur S3.

---

## Contexte

- **SF-INFRA-03** (repo `legalcase-infra`) a déployé une distribution CloudFront en frontal du bucket S3 documents avec un Origin Access Control (OAC) et une bucket policy qui interdit les accès directs S3 à toute autre identité que la distribution CloudFront.
- Domaine staging : `d2oaldre5efpif.cloudfront.net`. Prod : à compléter quand SF-INFRA-03 prod sera terminée (placeholder dans la ConfigMap).
- Aujourd'hui, `S3StorageService.presignedDownloadUrl(...)` génère une URL S3 présignée temporaire (15 min). Le contrôleur `DocumentController#download` renvoie un `302 Found` avec cette URL dans `Location:` ; le navigateur la suit anonymement.
- Côté infra, le bucket privé ne répond plus aux URLs présignées S3 directes (OAC + bucket policy). Sans patch côté Spring Boot, les téléchargements **cassent en staging/prod** dès que la bucket policy CloudFront-only est active.

---

## Comportement attendu

### Cas nominal

Quand la variable d'environnement `CLOUDFRONT_DOMAIN` est définie sur le pod backend :

1. L'avocat clique sur « Télécharger » dans le dossier → frontend `GET /api/v1/case-files/{id}/documents/{docId}/download`.
2. Le backend vérifie l'auth Spring Security + l'isolation workspace (comportement existant inchangé).
3. `S3StorageService.presignedDownloadUrl(storageKey, 15)` retourne désormais une URL signée CloudFront de la forme `https://{CLOUDFRONT_DOMAIN}/{storageKey}?Policy=...&Signature=...&Key-Pair-Id=...`.
4. Le contrôleur renvoie `302 Found` avec cette URL dans `Location:`.
5. Le navigateur suit la redirection vers CloudFront, qui sert le fichier depuis l'origine S3 via OAC.

### Cas dégradé : `CLOUDFRONT_DOMAIN` absente

Si la variable n'est pas définie (dev local, MinIO, tests ne dépendant pas du CDN) :

- `S3StorageService.presignedDownloadUrl(...)` retombe sur le comportement actuel = URL S3 présignée native.
- Ce fallback préserve le dev local et la rétrocompatibilité.

### Cas signature manquante

Si `CLOUDFRONT_DOMAIN` est défini mais que la `CLOUDFRONT_KEY_PAIR_ID` ou `CLOUDFRONT_PRIVATE_KEY` manque :

- Le bean lève `IllegalStateException` au démarrage (fail-fast) — la signature CloudFront est obligatoire en mode CDN, sinon les URLs émises seraient publiques et n'importe quel détenteur de la clé S3 (donc tout pirate disposant de l'URL) pourrait télécharger les documents.

### Cas d'erreur côté endpoint inchangés

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Pas d'auth | Redirection /login | 302 (existant) |
| `caseFileId` inexistant | Erreur 404 | 404 (existant) |
| `caseFileId` d'un autre workspace | 404 (camouflage) | 404 (existant) |
| Document inexistant ou hors `caseFile` | 404 | 404 (existant) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable (SF infra pure, pas d'outil décisionnel).
- [x] **Autres pays** : sans objet (le CDN est multi-locataires).
- [x] **Autres domaines** : sans objet.
- [x] **Autres UI patterns** : sans objet.
- [x] **Autres flows transversaux** : audit endpoint /download — seul consommateur de `StorageService.presignedDownloadUrl`. Vérifié par grep : 1 caller (`DocumentService#downloadUrl`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `DocumentService#downloadUrl` | Oui | Couvert nativement — la signature CloudFront est encapsulée dans `S3StorageService.presignedDownloadUrl` |
| `DocumentService#content` (stream same-origin) | Non | Sert les bytes via le pod backend, n'utilise pas d'URL présignée |
| Génération PDF / exports IA | Non | Aucun export hors-pod n'expose d'URL CloudFront aujourd'hui |

### Décision

- [x] Étendu à l'unique caller dans cette subfeature.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend / infrastructure pure, aucun composant Angular décisionnel modifié.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF infrastructure pure, pas d'outil décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] **C1 — Propriété Spring** : la propriété `legalcase.cloudfront.domain` est exposée via `CloudFrontProperties` (`@ConfigurationProperties("legalcase.cloudfront")`) et alimentée par l'env var `CLOUDFRONT_DOMAIN` (mapping Spring Boot relaxed binding).
- [ ] **C2 — URL CloudFront signée** : quand `legalcase.cloudfront.domain`, `key-pair-id` et `private-key` sont définis, `S3StorageService.presignedDownloadUrl(key, 15)` retourne une URL `https://{domain}/{key}?Policy=...&Signature=...&Key-Pair-Id=...` (canned policy, expiration 15 min).
- [ ] **C3 — Fallback S3 si pas de CDN configuré** : quand `legalcase.cloudfront.domain` n'est pas défini (vide ou absent), `presignedDownloadUrl` retombe sur l'URL S3 présignée native (comportement actuel).
- [ ] **C4 — Fail-fast si configuration incomplète** : si `legalcase.cloudfront.domain` est défini mais que `key-pair-id` ou `private-key` manque, le bean `CloudFrontUrlSigner` lève `IllegalStateException` au démarrage.
- [ ] **C5 — Endpoint inchangé** : `DocumentController#download` continue à renvoyer `302 Found` avec `Location:` ; les contrôles d'isolation workspace et d'auth sont inchangés. Test `download_existingDoc_returns302` continue à passer.
- [ ] **C6 — ConfigMap K8s staging** : `k8s/overlays/staging/kustomization.yaml` ajoute `CLOUDFRONT_DOMAIN=d2oaldre5efpif.cloudfront.net` dans le `configMapGenerator` `backend-config`.
- [ ] **C7 — ConfigMap K8s prod** : `k8s/overlays/production/kustomization.yaml` ajoute un placeholder commenté `CLOUDFRONT_DOMAIN_PROD_PLACEHOLDER` (à remplacer une fois SF-INFRA-03 prod terminé). Les secrets `key-pair-id` + `private-key` sont injectés via le secret K8s existant `backend-secrets` (clés `CLOUDFRONT_KEY_PAIR_ID` et `CLOUDFRONT_PRIVATE_KEY`).
- [ ] **C8 — Tests unitaires** : test `S3StorageServiceTest` couvrant (a) fallback S3 si domaine absent, (b) URL CloudFront signée si domaine + clés présents.
- [ ] **C9 — Coding rules** : pas d'`alert()`, pas de `MatDatepicker`, pas de mot `IA` ajouté au copy (sans objet — SF backend).

---

## Périmètre

### Hors scope (explicite)

- **CloudFront signed cookies** : non envisagé (la signature URL suffit pour `GET /download` ponctuel).
- **Cache invalidation API** : non couvert (les clés S3 sont immutables — UUID dans le chemin, pas de réutilisation, donc pas d'invalidation nécessaire).
- **CORS au niveau CloudFront** : non couvert ici (le 302 du backend masque CORS — la response cible est `Content-Disposition: attachment`).
- **Provisionnement Terraform de la `key_pair` CloudFront** : géré côté `legalcase-infra` (SF-INFRA-03 ou SF-INFRA-09 si pas inclus).
- **Migration du endpoint `/content`** (PDF.js same-origin) : reste sur le stream backend (cf. SF-127-01).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `legalcase.cloudfront.domain` | `null` | env var optionnelle, fallback S3 si absente |
| `legalcase.cloudfront.key-pair-id` | `null` | obligatoire **si** `domain` défini, sinon ignorée |
| `legalcase.cloudfront.private-key` | `null` | obligatoire **si** `domain` défini, sinon ignorée |

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `legalcase.cloudfront.domain` | Non | hostname FQDN sans schéma ni slash (ex. `d2oaldre5efpif.cloudfront.net`) |
| `legalcase.cloudfront.key-pair-id` | Si `domain` défini | string AWS Key Pair ID (`K2...`) |
| `legalcase.cloudfront.private-key` | Si `domain` défini | PEM RSA private key (string multilignes ou base64) |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum | Modifié ? |
|---------|-----|------|-------------|-----------|
| GET | `/api/v1/case-files/{caseFileId}/documents/{docId}/download` | Oui | MEMBER | Non (corps de réponse identique : 302 + Location) |

### Composants impactés

- `backend/src/main/java/fr/ailegalcase/storage/CloudFrontProperties.java` — **NOUVEAU** : `@ConfigurationProperties("legalcase.cloudfront")`.
- `backend/src/main/java/fr/ailegalcase/storage/CloudFrontUrlSigner.java` — **NOUVEAU** : signe les URLs CloudFront avec une canned policy + RSA.
- `backend/src/main/java/fr/ailegalcase/storage/S3StorageService.java` — **MODIFIÉ** : injecte `CloudFrontProperties` + `Optional<CloudFrontUrlSigner>`, branche `presignedDownloadUrl(...)` sur CDN si configuré, fallback S3 sinon.
- `k8s/overlays/staging/kustomization.yaml` — **MODIFIÉ** : ajoute `CLOUDFRONT_DOMAIN=d2oaldre5efpif.cloudfront.net`.
- `k8s/overlays/production/kustomization.yaml` — **MODIFIÉ** : ajoute placeholder commenté.
- `backend/src/test/java/fr/ailegalcase/storage/S3StorageServiceTest.java` — **NOUVEAU** : tests fallback S3 + URL CloudFront signée.

### Migration Liquibase

- [x] Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] `S3StorageServiceTest#presignedDownloadUrl_withoutCloudFrontDomain_returnsS3PresignedUrl` — fallback nominal.
- [ ] `S3StorageServiceTest#presignedDownloadUrl_withCloudFrontDomain_returnsSignedCloudFrontUrl` — URL CDN signée.
- [ ] `S3StorageServiceTest#presignedDownloadUrl_withCloudFrontDomain_signedUrlContainsKey` — vérifie présence de `Policy=`, `Signature=`, `Key-Pair-Id=`.

### Tests d'intégration

- [ ] `DocumentControllerIT#download_existingDoc_returns302` — non régression (continue à passer avec le mock).
- [ ] `DocumentControllerIT#download_unknownDoc_returns404` — non régression.
- [ ] `DocumentControllerIT#download_withoutAuth_returns401` — non régression.

### Isolation workspace

- [x] Couverte par les tests d'intégration existants (`download_*` dans `DocumentControllerIT`). Aucun changement d'isolation.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée : impact limité à la couche `StorageService.presignedDownloadUrl` ; l'auth, le workspace, les plans, le routing frontend ne sont pas touchés.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — l'URL retournée par `/download` change de host (S3 → CloudFront) mais le code HTTP `302` et la sémantique côté front sont strictement identiques. Le smoke ne vérifie pas le host cible.

---

## Dépendances

### Subfeatures bloquantes

- **SF-INFRA-03** (`legalcase-infra`) — statut : **Fait** (staging déployé `d2oaldre5efpif.cloudfront.net`, prod en cours).

### Pré-requis runtime

- Secret K8s `backend-secrets` doit contenir `CLOUDFRONT_KEY_PAIR_ID` et `CLOUDFRONT_PRIVATE_KEY` **avant** d'activer la variable `CLOUDFRONT_DOMAIN` dans la ConfigMap d'un environnement donné. Sans ces secrets, le pod backend refusera de démarrer (fail-fast — cf. C4).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

### Décision sécurité — pourquoi CloudFront *signed* URLs et pas CloudFront *simple*

CloudFront avec OAC sert un bucket privé : sans signature, l'URL `https://{cdn}/{s3-key}` est **publique et permanente**, accessible par n'importe quel détenteur de l'URL. Les pre-signed URLs S3 actuelles sont signées + expirent au bout de 15 min : passer à CloudFront simple **serait une régression de sécurité** (les URLs documents sortiraient potentiellement dans les logs, l'historique navigateur, etc., sans expiration).

→ Décision : CloudFront **signed URLs canned policy** avec une expiration de 15 min (parité fonctionnelle avec les pre-signed S3 actuelles). Le bean `CloudFrontUrlSigner` charge la clé privée RSA depuis l'env var `CLOUDFRONT_PRIVATE_KEY` au démarrage.

### Pourquoi pas un SF-INFRA-09 séparé pour les signed URLs

L'alternative consistait à livrer cette SF avec CloudFront *simple* puis un SF-INFRA-09 ajoutant la signature. Refusé : cela aurait introduit une fenêtre de quelques jours / semaines pendant laquelle tous les documents staging seraient accessibles à n'importe qui via une URL devinable (clé S3 = `{workspace_uuid}/{case_uuid}/{doc_uuid}/{filename}` — les UUIDs sont non devinables mais fuiteraient en cas de log/screenshot/Slack). Pour une appli RGPD avec données sensibles d'avocats, ce risque n'est pas acceptable, même transitoirement.

### Algo de signature

Canned policy AWS standard (cf. doc AWS « Creating signed URLs for CloudFront with a canned policy ») :

```
URL = https://{domain}/{key}?Expires={epoch}&Signature={base64-url(rsa_sha1(policy, private_key))}&Key-Pair-Id={kid}
```

Implémentation Java : `Signature.getInstance("SHA1withRSA")` + clé PEM lue via `PKCS8EncodedKeySpec`. Pas de dépendance externe (le SDK AWS v2 ne fournit pas de signer CloudFront direct ; on l'écrit en ~30 lignes).
