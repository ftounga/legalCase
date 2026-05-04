# Mini-spec — F-187 / SF-187-01 — Backend Export traction commerciale (PDF)

## Identifiant

`F-187 / SF-187-01-backend`

## Feature parente

`F-187` — Export traction commerciale (one-pager PDF auto) — super-admin

## Statut

`ready`

## Date de création

2026-05-04

## Branche Git

`feat/SF-187-01-backend`

---

## Objectif

Exposer un endpoint super-admin qui génère un one-pager PDF (traction commerciale) en composant les KPIs courants du dashboard F-76 et des inputs textuels variables (verbatims, partenaires, accroche, contact).

---

## Comportement attendu

### Cas nominal

1. Le super-admin appelle `POST /api/v1/super-admin/traction-onepager/pdf` avec un body JSON contenant :
   - `includeKpis` : map booléenne des 7 KPIs F-76 (lesquels inclure dans le PDF)
   - `verbatims` : liste de 0 à 2 verbatims `{ quote: string, author: string }`
   - `partners` : liste de noms de partenaires à afficher (string libre)
   - `headline` : accroche commerciale (texte court < 200 chars)
   - `contact` : `{ name, email, url }` (toujours présent — par défaut, pré-rempli côté frontend depuis le user connecté)
2. Le service lit les KPIs courants via `SuperAdminMetricsService.getMetrics()` (déjà disponible F-76).
3. Le service compose le PDF via Apache PDFBox avec un layout fixé (charte navy `#1A3A5C` + or `#C9973A`, JetBrains Mono pour les chiffres et IDs, Inter pour le texte courant).
4. La réponse est `application/pdf`, code 200, body = stream PDF, header `Content-Disposition: attachment; filename="legalcase-traction-YYYY-MM-DD.pdf"`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non super-admin | Refus accès | 403 |
| Utilisateur non authentifié | Refus accès | 401 |
| `headline` manquant ou vide après trim() | Erreur validation | 400 |
| `contact.email` invalide format | Erreur validation | 400 |
| `verbatims.length > 2` | Erreur validation (max 2) | 400 |
| `verbatims[i].quote` > 500 chars | Erreur validation | 400 |
| `partners.length > 10` | Erreur validation (max 10) | 400 |
| `headline.length > 200` | Erreur validation | 400 |
| Exception PDFBox lors de la génération | Erreur serveur, log ERROR avec stack, message générique côté client | 500 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable (outil interne super-admin, pas un outil décisionnel métier)
- [x] **Autres pays** — non applicable (outil interne, FR uniquement V1)
- [x] **Autres domaines** — non applicable (transversal)
- [x] **Autres UI patterns** — exports PDF existants : aucun export PDF côté super-admin actuellement. Pattern de référence pour `attachment` : `CaseAnalysisPdfExportController` (export synthèse PDF dossier) — réutiliser le même pattern de réponse stream `ResponseEntity<byte[]>` avec `MediaType.APPLICATION_PDF`.
- [x] **Autres flows transversaux** — gate super-admin : pattern existant `assertSuperAdmin` (F-76, F-178). Réutilisé tel quel.

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** — DTO frontend miroir du DTO backend (cf. SF-187-02)
- [x] **Record / DTO backend** — `TractionOnePagerInput`, `IncludedKpis`, `VerbatimDto`, `ContactDto` (records Java)
- [x] **Service / logique métier** — `TractionOnePagerService` (composition PDF + lecture KPIs)
- [x] **Entité JPA + schéma DB** — aucune entité, aucune table (génération à la volée, pas de stockage)
- [x] **Tests existants** — `SuperAdminControllerTest` (gate super-admin), pattern `CaseAnalysisPdfExportControllerTest` (PDF stream)

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — outil interne, pas un outil décisionnel métier.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern UI pourrait-il être réutilisé ?** — la composition PDF Apache PDFBox est nouvelle pour ce projet (les exports synthèse utilisent un autre layout). Le code est isolé dans `TractionOnePagerService` et son layout est *spécifique au one-pager* — aucune ambition de réutilisation. Pas de dette de convergence à gérer.
- [x] **Y a-t-il des patterns concurrents ?** — pour les exports PDF côté backend, le pattern `iText` n'est pas utilisé (PDFBox déjà la lib retenue). Aucun conflit.
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** — non, `TractionOnePagerService` est strictement spécifique au super-admin. Pas d'extraction nécessaire.
- [x] **Le nouveau composant a-t-il un équivalent design ?** — non, aucun équivalent.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern export PDF Apache PDFBox | Oui | Intégré dans cette SF — service isolé, layout dédié, pas de réutilisation prévue |
| Gate super-admin | Oui | Réutilise `assertSuperAdmin` existant |
| Lecture KPIs F-76 | Oui | Réutilise `SuperAdminMetricsService.getMetrics()` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Impact par domaine métier

Cette SF est-elle sensible au domaine (droit du travail / immigration / famille) ?
**Non — transversal**. F-187 est un outil interne super-admin pour le pilotage commercial. Aucune adaptation par domaine métier ni par pays (FR / BE).

---

## Critères d'acceptation

- [ ] `POST /api/v1/super-admin/traction-onepager/pdf` retourne un PDF valide (≥ 1 page, header PDF correct `%PDF-1.x`)
- [ ] Le PDF contient les 7 KPIs si tous cochés `true` dans `includeKpis`
- [ ] Si un KPI est `false`, il n'apparaît pas dans le PDF (vérifier en relisant le PDF généré)
- [ ] Le PDF contient le `headline` en titre principal
- [ ] Le PDF contient les verbatims (jusqu'à 2) avec leur `author` mis en italique
- [ ] Le PDF contient les partenaires sous forme de liste
- [ ] Le PDF contient le footer `contact.name`, `contact.email`, `contact.url`
- [ ] La date du jour apparaît dans le footer (`YYYY-MM-DD`)
- [ ] Header response : `Content-Type: application/pdf` + `Content-Disposition: attachment; filename="legalcase-traction-YYYY-MM-DD.pdf"`
- [ ] Utilisateur non super-admin → 403
- [ ] Utilisateur non authentifié → 401
- [ ] Validation : `headline` vide → 400
- [ ] Validation : `verbatims.length > 2` → 400
- [ ] Validation : `partners.length > 10` → 400
- [ ] Charte appliquée : navy `#1A3A5C` pour le header / titre, or `#C9973A` pour les accents (vérification manuelle visuelle)

---

## Périmètre

### Hors scope (explicite)

- WYSIWYG / preview PDF live côté backend
- Stockage du PDF en base ou sur S3 (pas de versioning, pas d'historique)
- Multilingue (FR uniquement V1)
- Personnalisation des couleurs (charte fixée)
- Graphiques (charts) — uniquement chiffres + texte
- URL publique téléchargeable (le PDF n'est généré que via super-admin authentifié)
- Export PowerPoint / Word

---

## Contrat API

> **Section obligatoire pour parallélisation backend / frontend (CLAUDE.md règle §3).**
> Cette section est **figée** avant le dev. Toute modification en cours de dev requiert l'arrêt des 2 agents et une mise à jour synchrone de SF-187-01 + SF-187-02.

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/super-admin/traction-onepager/pdf` | Oui (Bearer JWT ou Cookie session) | Super-admin (`is_super_admin = true`) |

### Body de requête

`Content-Type: application/json`

```json
{
  "includeKpis": {
    "totalWorkspaces": true,
    "trialWorkspaces": true,
    "paidWorkspaces": false,
    "conversionRatePct": true,
    "activeWorkspaces30d": true,
    "analysesLast7Days": true,
    "analysesLast30Days": true
  },
  "verbatims": [
    { "quote": "On a gagné 2 h par dossier en 1 mois", "author": "Maître X, Cabinet Paris 8e" }
  ],
  "partners": ["Village de la Justice", "ACE"],
  "headline": "L'IA juridique souveraine pour les avocats droit du travail",
  "contact": {
    "name": "Franck Tounga",
    "email": "franck@ng-itconsulting.com",
    "url": "https://legalcase.ng-itconsulting.com"
  }
}
```

### Validation des champs

| Champ | Obligatoire | Type | Contraintes |
|-------|-------------|------|-------------|
| `includeKpis` | Oui | object | doit contenir les 7 clés booléennes ci-dessus |
| `includeKpis.totalWorkspaces` | Oui | boolean | — |
| `includeKpis.trialWorkspaces` | Oui | boolean | — |
| `includeKpis.paidWorkspaces` | Oui | boolean | — |
| `includeKpis.conversionRatePct` | Oui | boolean | — |
| `includeKpis.activeWorkspaces30d` | Oui | boolean | — |
| `includeKpis.analysesLast7Days` | Oui | boolean | — |
| `includeKpis.analysesLast30Days` | Oui | boolean | — |
| `verbatims` | Non | array | 0 à 2 éléments |
| `verbatims[i].quote` | Oui (si verbatim) | string | non vide après trim, ≤ 500 chars |
| `verbatims[i].author` | Oui (si verbatim) | string | non vide après trim, ≤ 200 chars |
| `partners` | Non | array of string | 0 à 10 éléments, chaque string ≤ 100 chars |
| `headline` | Oui | string | non vide après trim, ≤ 200 chars |
| `contact.name` | Oui | string | non vide après trim, ≤ 100 chars |
| `contact.email` | Oui | string | format email valide, ≤ 200 chars |
| `contact.url` | Oui | string | format URL valide (http(s)://), ≤ 300 chars |

### Réponse — succès (200 OK)

| Header | Valeur |
|--------|--------|
| `Content-Type` | `application/pdf` |
| `Content-Disposition` | `attachment; filename="legalcase-traction-YYYY-MM-DD.pdf"` (où `YYYY-MM-DD` = date du jour) |
| `Cache-Control` | `no-store` |

Body : binaire PDF (≥ 1 page, header `%PDF-1.x`, ≥ 1 KB).

### Réponses — erreurs

| Code | Cause | Body |
|------|-------|------|
| 400 | Validation échec (champ manquant, longueur, format) | `{ "code": "VALIDATION_ERROR", "message": "<détail du champ fautif>" }` |
| 401 | Non authentifié | (pattern Spring Security existant) |
| 403 | Authentifié mais pas super-admin | `{ "code": "FORBIDDEN", "message": "Super-admin access required" }` |
| 500 | Exception PDFBox / interne | `{ "code": "INTERNAL_ERROR", "message": "PDF generation failed" }` (détail dans logs serveur uniquement) |

### Notes contractuelles

- Les 7 KPIs sont lus côté serveur depuis `SuperAdminMetricsService.getMetrics()` au moment de l'appel — l'avocat n'a **pas** à les fournir dans le body. Le frontend les affiche en lecture seule mais ne les envoie pas.
- Si `includeKpis.<key> = false`, le KPI est totalement omis du PDF (pas affiché en grisé).
- La date `YYYY-MM-DD` du nom de fichier et du footer = date serveur UTC.

---

## Valeurs initiales

Aucune entité créée — pas de valeur initiale à définir.

---

## Contraintes de validation

Voir tableau "Validation des champs" dans la section Contrat API ci-dessus. Validation implémentée via Bean Validation (`@NotNull`, `@NotBlank`, `@Email`, `@Size`, `@Valid` sur listes imbriquées).

---

## Technique

### Endpoints exposés

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/super-admin/traction-onepager/pdf` | Oui | Super-admin |

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable — aucune entité, aucune table

### Dépendances Maven

- [x] **Apache PDFBox** : déjà présent dans `pom.xml` ? À vérifier au démarrage du dev. Si absent : ajouter `org.apache.pdfbox:pdfbox` dernière stable (3.x) compatible Java 21. Note : il est possible que le projet utilise déjà `iText` ou une autre lib PDF — dans ce cas, **utiliser celle déjà présente** plutôt que d'introduire PDFBox. Vérifier `grep -r "pdfbox\|itextpdf\|com.lowagie" backend/pom.xml` avant le commit dépendance.

### Composants backend

- `TractionOnePagerController` — endpoint REST, gate super-admin, validation, appel service, retour stream PDF.
- `TractionOnePagerService` — composition PDF (lecture KPIs + layout) ; isolé, pas d'injection inutile.
- `TractionOnePagerInput` — record DTO body (incluant nested `IncludedKpis`, `VerbatimDto`, `ContactDto`).
- Annotations Bean Validation sur les records.

### Pattern de référence

`CaseAnalysisPdfExportController` ou équivalent : retour `ResponseEntity<byte[]>` avec headers explicites. Le grep `grep -r "MediaType.APPLICATION_PDF" backend/src/main` au début du dev pour identifier le pattern exact en usage.

---

## Plan de test

### Tests unitaires

- [ ] `TractionOnePagerService` — cas nominal : 7 KPIs cochés, 2 verbatims, 3 partners, headline + contact → produit un byte[] non vide commençant par `%PDF-`
- [ ] `TractionOnePagerService` — KPI `paidWorkspaces = false` → texte "Workspaces payants" absent du PDF (parser PDF avec PDFBox `PDFTextStripper` pour vérifier)
- [ ] `TractionOnePagerService` — 0 verbatim → section "Témoignages" absente
- [ ] `TractionOnePagerService` — 0 partner → section "Partenaires" absente
- [ ] `TractionOnePagerService` — date footer = date du jour (vérifier via `Clock` injecté pour stabilité test)

### Tests d'intégration

- [ ] `POST /api/v1/super-admin/traction-onepager/pdf` super-admin → 200 + Content-Type `application/pdf` + body ≥ 1 KB
- [ ] `POST /api/v1/super-admin/traction-onepager/pdf` user normal → 403
- [ ] `POST /api/v1/super-admin/traction-onepager/pdf` non authentifié → 401
- [ ] `POST` body `headline = ""` → 400
- [ ] `POST` body `verbatims.length = 3` → 400
- [ ] `POST` body `partners.length = 11` → 400
- [ ] `POST` body `contact.email = "invalide"` → 400
- [ ] `POST` body `verbatims[0].quote = "x".repeat(501)` → 400
- [ ] Header `Content-Disposition` contient `filename="legalcase-traction-YYYY-MM-DD.pdf"` avec la date du jour

### Isolation workspace

- [x] Non applicable — endpoint super-admin only, pas de workspace context utilisateur (l'output reflète des données globales de la plateforme)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — endpoint nouvel, isolé. Pas d'auth modifiée, pas de workspace context modifié, pas de plan modifié, pas de routing frontend modifié.

### Composants / endpoints existants potentiellement impactés

Aucun. Endpoint nouveau, service nouveau, aucune modification d'entité existante.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné. Justification : endpoint isolé super-admin, pas dans les chemins critiques (auth / workspace / navigation principale).

---

## Dépendances

### Subfeatures bloquantes

- F-76 ✅ Terminée — `SuperAdminMetricsService.getMetrics()` doit exister et être appelable.
- Aucune autre.

### Questions ouvertes impactées

- [x] Aucune — toutes les décisions techniques sont tranchées dans cette mini-spec.

### Subfeatures parallèles

- **SF-187-02 frontend** — démarrée en parallèle (cf. skill `parallel-frontback-delivery.md`). Consomme le contrat API figé ci-dessus.

---

## Notes et décisions

- **Lib PDF retenue** : Apache PDFBox (Java natif, pas de surcoût front, layout programmatique). Vérification au démarrage qu'aucune autre lib PDF (iText, etc.) n'est déjà en place — si oui, utiliser l'existante plutôt qu'en introduire une nouvelle.
- **Pourquoi pas un template Thymeleaf + wkhtmltopdf** : ajout d'une dépendance binaire externe (wkhtmltopdf), risque de divergence environnement (Docker / staging / prod), surcoût installation. PDFBox est self-contained.
- **Pourquoi pas du HTML→PDF côté frontend (jsPDF, html2canvas)** : qualité variable, charte plus difficile à appliquer rigoureusement, rendu dépendant du navigateur. Le PDF doit être *deterministically* généré côté serveur.
- **Layout fixé** : titre headline en haut (Inter bold 24pt navy), KPIs en grille 2 colonnes (chiffre JetBrains Mono 36pt or, label Inter 12pt navy), verbatims en italique, partenaires en liste à puces, footer en bas (date + contact).
- **Stockage** : aucun. Le PDF est généré à la volée à chaque appel.
