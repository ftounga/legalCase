# Mini-spec — M-26 / SF-M26-01 Newsletter mensuelle automatique

> Statut : ready
> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`M-26 / SF-M26-01`

## Feature parente

`M-26` — Newsletter mensuelle — template + envoi automatique

## Statut

`ready`

## Date de création

2026-04-01

## Branche Git

`feat/SF-M26-01-newsletter-mensuelle`

---

## Objectif

Envoyer automatiquement le 1er de chaque mois à 8h un email de newsletter personnalisé à chaque OWNER de workspace payant (SOLO, TEAM, PRO), incluant les stats du mois écoulé et une feature mise en avant par rotation déterministe.

---

## Comportement attendu

### Cas nominal

1. Le scheduler se déclenche le 1er du mois à 8h (`@Scheduled(cron = "0 0 8 1 * *")`)
2. Pour chaque workspace avec `subscription.planCode` IN (`SOLO`, `TEAM`, `PRO`) :
   a. Récupérer l'OWNER du workspace
   b. Vérifier qu'aucun enregistrement `email_sends` de type `NEWSLETTER_MONTHLY` n'existe pour cet utilisateur dans le mois courant (déduplication)
   c. Calculer les stats du mois écoulé pour le workspace :
      - Nombre d'analyses lancées (analyses créées dans le mois écoulé)
      - Nombre de dossiers actifs (non archivés)
      - Nombre de documents uploadés dans le mois écoulé
   d. Sélectionner la feature du mois par rotation déterministe : `LocalDate.now().getMonthValue() % featureList.size()`
   e. Envoyer l'email via `EmailService`
   f. Enregistrer dans `email_sends` (NEWSLETTER_MONTHLY, userId, sentAt = now())
3. En cas d'erreur pour un workspace donné : log.warn + continue (fail-open)

### Contenu de l'email

**Sujet :** `[AI LegalCase] Votre récapitulatif de [mois] [année]`

**Corps (texte) :**
```
Bonjour [prénom],

Voici votre récapitulatif pour le mois de [mois] :

📊 Votre activité
- [N] analyse(s) lancée(s)
- [N] dossier(s) actif(s)
- [N] document(s) uploadé(s)

✨ Feature du mois : [titre de la feature]
[Description courte — 2 lignes max]

Accédez à votre espace : [frontendUrl]

L'équipe AI LegalCase
```

### Liste des features (rotation déterministe, 12 entrées)

```java
private static final List<String[]> FEATURES = List.of(
    new String[]{"Score de risque global", "Chaque dossier est maintenant noté de 0 à 100 avec un niveau de risque FAIBLE, MOYEN ou ÉLEVÉ calculé par l'IA."},
    new String[]{"Traçabilité des sources IA", "Chaque point de la synthèse indique désormais le document source exact qui a fondé l'analyse."},
    new String[]{"Stepper de progression", "Un indicateur visuel vous guide pas à pas dans le traitement de chaque dossier."},
    new String[]{"Checklist de conformité procédurale", "Vérifiez automatiquement que toutes les étapes procédurales sont respectées."},
    new String[]{"Export PDF personnalisé", "Générez un rapport PDF professionnel de la synthèse en un clic."},
    new String[]{"Export DOCX", "Exportez la synthèse au format Word pour la modifier et l'intégrer à vos documents."},
    new String[]{"Partage sécurisé avec votre client", "Partagez la synthèse via un lien temporaire et révocable, sans compte nécessaire."},
    new String[]{"Chat avec l'IA sur votre dossier", "Posez des questions libres à l'IA directement sur le contenu de vos documents."},
    new String[]{"Analyse enrichie multi-sources", "L'IA croise tous vos documents pour produire une synthèse approfondie avec sources croisées."},
    new String[]{"Délais légaux automatiques", "Les délais importants de votre dossier sont détectés et affichés avec leur date limite."},
    new String[]{"Questions interactives de l'IA", "L'IA peut vous poser des questions ciblées pour affiner son analyse quand des informations manquent."},
    new String[]{"Tableau de bord super-admin", "Suivez les métriques de conversion et d'usage de toute la plateforme en temps réel."}
);
```

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `mailEnabled = false` | Aucun email envoyé (géré par EmailService) |
| Workspace sans OWNER | Log warn, skip workspace |
| Erreur SMTP sur un envoi | Log warn, continuer avec le workspace suivant |
| Email déjà envoyé ce mois (déduplication) | Skip silencieux |
| Workspace FREE | Exclu de la requête, jamais traité |

---

## Critères d'acceptation

- [ ] Le scheduler se déclenche le 1er du mois à 8h
- [ ] Seuls les workspaces SOLO/TEAM/PRO reçoivent l'email
- [ ] Les stats (analyses, dossiers, documents) sont calculées pour le mois courant du workspace
- [ ] La feature du mois est sélectionnée par `monthValue % 12` (déterministe, reproductible)
- [ ] La déduplication empêche un double envoi si le scheduler tourne deux fois le même mois
- [ ] L'échec sur un workspace n'interrompt pas le traitement des autres
- [ ] Le type `NEWSLETTER_MONTHLY` est ajouté à l'enum `EmailType`
- [ ] Un test unitaire vérifie la logique de déduplication et de rotation

---

## Périmètre

### Hors scope

- Template HTML riche (email en texte simple comme les autres emails existants)
- Lien de désinscription (V2 — RGPD best effort pour V1)
- Envoi manuel / triggered par l'admin
- Personnalisation du contenu par workspace
- Statistiques globales de la plateforme (pas de données cross-workspace)

---

## Technique

### Endpoint(s)

Aucun endpoint HTTP — traitement purement backend/scheduler.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `email_sends` | INSERT + SELECT | Déduplication : type=NEWSLETTER_MONTHLY, userId, sentAt dans le mois courant |
| `workspace` | SELECT | Jointure avec subscription pour filtrer SOLO/TEAM/PRO |
| `workspace_member` | SELECT | Trouver l'OWNER du workspace |
| `case_file` | SELECT COUNT | Dossiers actifs (non archivés) |
| `case_analysis` | SELECT COUNT | Analyses du mois écoulé |
| `document` | SELECT COUNT | Documents uploadés le mois écoulé |

### Migration Liquibase

- [ ] Non applicable — `EmailType` est `@Enumerated(EnumType.STRING)`, ajouter `NEWSLETTER_MONTHLY` à l'enum Java suffit.

### Composants Angular

Aucun — feature purement backend.

---

## Plan de test

### Tests unitaires

- [ ] `MonthlyNewsletterScheduler` — `shouldSkipIfAlreadySentThisMonth` : si email_sends contient NEWSLETTER_MONTHLY pour l'utilisateur ce mois, ne pas envoyer
- [ ] `MonthlyNewsletterScheduler` — `shouldSkipFreeWorkspaces` : workspace FREE exclu
- [ ] `MonthlyNewsletterScheduler` — `featureRotationIsDeterministic` : même mois → même feature
- [ ] `MonthlyNewsletterScheduler` — `continuesAfterOneFailure` : exception sur un workspace → les autres sont traités

### Tests d'intégration

Non requis pour un scheduler interne sans endpoint HTTP exposé.

### Isolation workspace

- [ ] Non applicable — le scheduler accède à tous les workspaces éligibles, aucune donnée cross-workspace n'est exposée à l'utilisateur.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — NON
- [ ] Workspace context — NON (lecture interne, pas de Principal)
- [ ] Plans / limites — NON (filtre par planCode, pas de modification de quota)
- [ ] Navigation / routing frontend — NON
- [x] **Aucune préoccupation transversale** — feature isolée backend scheduler

### Composants / endpoints existants potentiellement impactés

Aucun — ajout pur, aucun composant existant modifié sauf `EmailType` enum (valeur ajoutée, non cassante).

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — feature backend sans UI, pas de route ni guard modifié.

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Rotation déterministe** : `LocalDate.now().getMonthValue() % FEATURES.size()`. Reproductible, pas d'état à stocker. Liste de 12 features = une feature différente chaque mois de l'année.
- **Fail-open** : même pattern que `OnboardingEmailScheduler` — try/catch par workspace, log.warn en cas d'erreur.
- **Déduplication** : requête sur `email_sends` filtrée sur `userId`, `emailType = NEWSLETTER_MONTHLY`, `sentAt >= début du mois courant`.
- **Stats** : calcul pour le mois en cours au moment de l'envoi (1er du mois → stats du mois précédent seraient plus pertinentes, mais par simplicité on calcule les 30 derniers jours glissants — plus robuste et déjà présent dans d'autres queries).
- **Texte simple** : cohérent avec les emails existants (SimpleMailMessage). Pas de HTML pour V1.
