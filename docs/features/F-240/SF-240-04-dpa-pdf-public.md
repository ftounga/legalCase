# Mini-spec — F-240 / SF-240-04 DPA téléchargeable — PDF + endpoint public

## Identifiant

`F-240 / SF-240-04`

## Feature parente

`F-240` — Conformité contractuelle — click-wrap CGU/CGV/DPA + traçabilité

## Statut

`draft`

## Date de création

2026-05-11

## Branche Git

`feat/SF-240-04-dpa-pdf-public`

> **Indépendante** des SF-240-02 et SF-240-03. Consomme SF-240-01 pour tracer les téléchargements.

---

## Objectif

Rédiger un Data Processing Agreement (DPA) conforme RGPD art. 28 à partir des templates open-source CNIL/EDPB, le bundler en PDF dans les ressources backend, l'exposer via un endpoint public `GET /api/v1/legal/dpa` (sans authentification), tracer chaque téléchargement côté serveur via le service de consent SF-240-01, et ajouter le lien dans le footer landing + la page `/privacy`.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur (avocat, prospect, ou auditeur d'un cabinet client) visite la landing page `legalcase.fr` ou la page `/privacy`.
2. **Nouveauté footer** : un lien « Télécharger le DPA (RGPD art. 28) » apparaît dans le footer aux côtés des liens existants (CGU, Privacy, Mentions légales, Contact).
3. **Nouveauté page `/privacy`** : un encart en bas de page « Pour les responsables de traitement (cabinets d'avocats), téléchargez notre Data Processing Agreement conforme RGPD art. 28 → [Télécharger le DPA] ».
4. Le clic sur le lien déclenche `GET /api/v1/legal/dpa` qui retourne :
   - Status 200
   - Content-Type: `application/pdf`
   - Content-Disposition: `attachment; filename="legalcase-dpa-v1.pdf"`
   - Corps : binaire PDF (~50-100 ko)
5. Le backend, en parallèle de servir le PDF, enregistre un évènement de téléchargement :
   - Si l'utilisateur est authentifié (cookie session OAuth2 actif), POST silencieux vers `consentAcceptanceService.recordAcceptance(...)` avec `consentTypes: ["DPA_DOWNLOAD"]`, `version: "2026-05-11"`, IP + UA extraits du request.
   - Si l'utilisateur n'est PAS authentifié (visiteur anonyme), pas de POST consent (pas de `user_id` à attacher) — uniquement un log applicatif pour stats brutes.
6. Le navigateur déclenche le téléchargement.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|----------------------|
| Fichier PDF absent du classpath | 500 Internal Server Error + log Sentry |
| Erreur d'enregistrement consent (utilisateur authentifié) | Téléchargement servi quand même (le tracking ne doit pas bloquer la mise à disposition légale du document), log warning |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable.
- [x] **Autres pays** : DPA V1 commun FR + BE (le RGPD est un règlement européen, identique). Spécificités belges éventuelles (loi du 30 juillet 2018) en V2 si demande terrain.
- [x] **Autres domaines** : transversal.
- [x] **Autres UI patterns** : lien footer simple — pattern existant via F-74 footer.
- [x] **Autres flows transversaux** :
  - **Auth / Principal** — endpoint **public** (pas d'auth requise) ; si utilisateur authentifié, on lit le Principal pour tracer.
  - **Workspace context** — sans impact direct.
  - **Plans / limites** — sans impact.
  - **Navigation / routing frontend** — ajout de liens dans le footer (déjà composant standalone) + dans `/privacy`.

### Cas spécifique : nouveau pattern UI ou service partagé

- L'endpoint `GET /api/v1/legal/dpa` ouvre la voie à d'autres documents légaux téléchargeables (politique cookies en PDF, CGU en PDF, etc.). Si une telle demande surgit en V2, on généralisera vers `GET /api/v1/legal/{documentSlug}` avec un registre côté backend.
- Le bouton "Télécharger le DPA" pourrait être réutilisé sur une éventuelle page `/trust` ou `/security` ultérieure (cf. F-134 V9+). En V1 pas de page dédiée, juste footer + `/privacy`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Page `/trust` ou `/security` | Hypothétique (V9+ via F-134) | Backlog — le lien dans le footer suffit pour V1 |
| Téléchargement CGU/Privacy en PDF | Pas demandé | Backlog si signal terrain (les pages HTML suffisent en V1) |
| Authentification du téléchargeur | Délibérément non implémenté | Public en V1 (besoin marketing : prospects en pré-vente doivent pouvoir le télécharger) |

### Décision

- [x] Endpoint public (pas d'auth requise). Décision documentée en D-01.
- [x] DPA mono-version FR/BE en V1.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF infra + page statique. Aucun composant décisionnel.

---

## Impact par domaine métier

- [x] Transversal — DPA identique FR/BE. Pas d'adaptation par domaine.

---

## Parité des domaines métier (niveau ≥ 5)

- [x] **Non applicable** — pas un outil décisionnel.

---

## Critères d'acceptation

- [ ] **CA-01** : un fichier `legalcase-dpa-v1.pdf` est commité dans `backend/src/main/resources/static/legal/` avec un contenu DPA conforme RGPD art. 28 rédigé à partir des templates open-source CNIL/EDPB.
- [ ] **CA-02** : le DPA contient au minimum les 7 sections obligatoires de l'art. 28.3 RGPD :
  - (1) objet, durée, nature et finalité du traitement
  - (2) types de données et catégories de personnes concernées
  - (3) obligations et droits du responsable de traitement
  - (4) obligation du sous-traitant de traiter sur instruction documentée
  - (5) confidentialité du personnel
  - (6) mesures de sécurité (art. 32) : TLS 1.3, chiffrement at-rest RDS, OAuth2, audit logs F-38, isolation workspace
  - (7) recours à des sous-traitants ultérieurs : Anthropic (FR/UE), AWS eu-west-3, Stripe Ireland, Brevo
- [ ] **CA-03** : un fichier source `legalcase-dpa-v1.md` est commité dans `docs/legal/` permettant la régénération du PDF si nécessaire (relecture, traduction NL future).
- [ ] **CA-04** : un nouveau controller `LegalDocumentsController` expose `GET /api/v1/legal/dpa` qui retourne le PDF avec Content-Type + Content-Disposition corrects. Endpoint **public** (pas dans la blocking list de Spring Security).
- [ ] **CA-05** : si l'utilisateur est authentifié, le backend enregistre silencieusement un évènement consent `DPA_DOWNLOAD` via le service de SF-240-01 (le tracking est "best effort" — n'échoue jamais le téléchargement).
- [ ] **CA-06** : ajout du lien « Télécharger le DPA » dans le footer du composant landing (`landing.component.html` ligne ~910-916).
- [ ] **CA-07** : ajout d'un encart explicatif et du lien sur la page `/privacy` (composant `privacy.component`).
- [ ] **CA-08** : tests d'intégration backend : (a) `GET /api/v1/legal/dpa` sans auth → 200 + Content-Type pdf, (b) avec auth → 200 + un consent `DPA_DOWNLOAD` créé en DB, (c) PDF taille > 1 ko (sanity check non-vide).
- [ ] **CA-09** : tests Jest frontend : (a) le footer affiche le lien, (b) la page privacy affiche l'encart. Build OK.
- [ ] **CA-10** : SSG prerendering F-116 reste vert (le lien footer + l'encart privacy sont du statique HTML, aucun runtime requis).

---

## Périmètre

### Hors scope (explicite)

- DPA en néerlandais (BE NL) — V2 si demande terrain. Le DPA FR est juridiquement opposable en Belgique francophone.
- Signature électronique du DPA par le client — V9+ via F-134.
- Versioning historique des DPA (PDF v1, v2…) — V1 mono-version, à étendre si modifications légales matérielles.
- Endpoint d'admin pour consulter les téléchargements — V2 ou via F-178.
- Page `/trust` ou `/security` dédiée — V9+ via F-134.
- Génération dynamique du PDF (JasperReports, iText, etc.) — V1 PDF statique pré-généré, suffisant et économe.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `version` du consent `DPA_DOWNLOAD` (côté backend) | `"2026-05-11"` | Constante à bumper si le PDF est modifié |
| `legalcase-dpa-v1.pdf` (fichier) | committed en binaire | Régénération via Pandoc/LibreOffice depuis `.md` source si besoin |

---

## Contraintes de validation

Sans objet (endpoint serveur pur, pas de payload utilisateur).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Notes |
|---------|-----|------|-------|
| GET | `/api/v1/legal/dpa` | **Non requise** (public) | Renvoie le binaire PDF |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `user_consent_acceptance` | INSERT (si user authentifié) | Réutilisation infrastructure SF-240-01 |

### Migration Liquibase

- [x] **Non applicable** — pas de schéma nouveau (réutilise la table SF-240-01).

### Classes Java livrées

- `fr.ailegalcase.legal.LegalDocumentsController` — controller Spring exposant `GET /api/v1/legal/dpa`.
- Configuration Spring Security : ajouter `/api/v1/legal/**` à la liste des paths publics (`permitAll()`).

### Composants Angular modifiés

- `frontend/src/app/landing/landing.component.html` — ajout du lien `<a [href]="dpaUrl">Télécharger le DPA</a>` dans le footer (ligne ~910-916 selon l'audit).
- `frontend/src/app/privacy/privacy.component.html` (ou équivalent) — ajout d'un encart explicatif + lien.

### Composants Angular créés

- Aucun.

### Ressources livrées

- `backend/src/main/resources/static/legal/legalcase-dpa-v1.pdf` — binaire PDF (~50-100 ko)
- `docs/legal/legalcase-dpa-v1.md` — source Markdown du DPA, pour relecture et régénération PDF

---

## Plan de test

### Tests unitaires

- [ ] **LD-01** `LegalDocumentsControllerTest` (Mock MVC) — `GET /api/v1/legal/dpa` sans auth → 200, Content-Type `application/pdf`, header Content-Disposition `attachment; filename="legalcase-dpa-v1.pdf"`.
- [ ] **LD-02** `LegalDocumentsControllerTest` — corps de la réponse > 1 ko (non vide).
- [ ] **LD-03** `LegalDocumentsControllerTest` — appel avec auth → 200 ET un nouvel enregistrement `DPA_DOWNLOAD` est créé en DB pour le user authentifié.
- [ ] **LD-04** `LegalDocumentsControllerTest` — si l'enregistrement consent échoue (mock service throws), le 200 est tout de même servi (best effort, audit log warning).

### Tests d'intégration

- [ ] Idem ci-dessus (Mock MVC = test d'intégration léger).
- [ ] **LD-IT-01** — accès `/api/v1/legal/dpa` en mode production (Spring Security real) sans cookie → 200 (publique).

### Tests frontend (Jest)

- [ ] **LDF-01** `LandingComponent.spec.ts` — le footer contient un lien « Télécharger le DPA » pointant vers `/api/v1/legal/dpa`.
- [ ] **LDF-02** `PrivacyComponent.spec.ts` — l'encart DPA est présent avec le lien.

### Isolation workspace

- [x] **Non applicable** — endpoint public, pas de données workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — endpoint volontairement public.
- [ ] Workspace context — sans impact.
- [ ] Plans / limites — sans impact.
- [x] **Navigation / routing frontend** — ajout de liens dans le footer + page `/privacy`. **Vérifier** : SSG prerender de la landing et de `/privacy` reste vert (F-116).

### Composants existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| `LandingComponent` | Ajout d'un lien dans le footer | `LandingComponent.spec.ts` mis à jour + SSG vérifié |
| `PrivacyComponent` | Ajout d'un encart | `PrivacyComponent.spec.ts` mis à jour + SSG vérifié |
| Spring Security `SecurityConfig` | Nouvelle whitelist `/api/v1/legal/**` | IT vérifie le 200 sans cookie |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — vérifier que l'ajout de `/api/v1/legal/**` à la whitelist Spring Security ne casse pas les patterns d'auth existants (login OAuth, redirect non-authentifié).
- [ ] Aucun autre.

---

## Dépendances

### Subfeatures bloquantes

- SF-240-01 — pour le tracking côté serveur du `DPA_DOWNLOAD`. Peut développer en stub (tracking désactivé temporairement) et activer le tracking à l'intégration.

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

### Décision D-01 — Endpoint public (pas d'auth)

Le DPA doit être téléchargeable par des prospects en pré-vente (un avocat évalue plusieurs SaaS avant signature et compare leurs DPA). Imposer une authentification = friction commerciale qui réduit les conversions et envoie un signal de manque de transparence (les concurrents matures publient leur DPA en accès libre : Doctrine, Notion, Slack, etc.).

**Risque mitigé** : le DPA est un document public destiné à être lu — pas une donnée sensible. Le tracking se fait quand même côté serveur (au moins logs applicatifs pour analytics) sans bloquer l'accès.

### Décision D-02 — PDF statique pré-généré, pas dynamique

Pas de génération à la volée via JasperReports ou iText. Raisons :

1. Le DPA évolue très rarement (changement légal majeur ou changement de sous-traitants).
2. Un PDF statique commit en binaire (~50-100 ko) est plus simple, plus rapide, et opposable juridiquement (hash de fichier vérifiable).
3. Évite les dépendances Maven supplémentaires.

Régénération : si modification du DPA en V2, on régénère le PDF depuis le `.md` source via Pandoc ou LibreOffice headless, puis commit le nouveau binaire avec un nouveau nom (`legalcase-dpa-v2.pdf`) et un nouveau path `/api/v1/legal/dpa-v2` (ou bien on remplace en place avec une variable `CURRENT_DPA_VERSION`).

### Décision D-03 — Rédaction par l'opérateur depuis templates CNIL/EDPB

Décision opérateur 2026-05-11 — la rédaction de la v1 sera faite par l'opérateur (Franck Tounga) à partir des templates open-source CNIL (Modèle clauses contractuelles types responsable de traitement → sous-traitant) et EDPB (Standard Contractual Clauses). Pas d'externalisation avocat en V1 (surdimensionné pour un précurseur opérationnel pragmatique).

**Élément de validation interne** : le document devrait être relu par un avocat ou par Maître Mengue (interlocuteur juridique connu du projet) avant publication, mais ce n'est pas bloquant pour le merge SF-240-04 — la relecture peut intervenir post-prod pour ajustements mineurs.

### Décision D-04 — Pas d'authentification de téléchargement, mais traçabilité best-effort

Conséquence : pour les prospects anonymes, on a une trace dans les logs applicatifs (IP + UA + timestamp via les access logs Spring) mais pas dans la BD `user_consent_acceptance`. Pour les utilisateurs authentifiés, trace complète en BD. Cohérent avec l'objectif : faciliter l'accès en pré-vente tout en gardant un audit interne minimal.
