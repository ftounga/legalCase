# MARKETING_BACKLOG.md — AI LegalCase

Backlog des actions marketing, vente et communication.
Mis à jour au fil des conversations. Priorisé par impact estimé.

---

## Définition de Terminé — règle impérative

**Une tâche marketing n'est marquée `Terminé` que si elle est entièrement opérationnelle.**

| Type de tâche | Ce que "Terminé" signifie |
|---------------|--------------------------|
| Email / séquence automatique | Code implémenté, déployé en production, emails envoyés réellement |
| Page web / landing | Composant déployé en production, accessible à l'URL publique |
| Vidéo / visuel | Fichier livré, publié sur le canal cible |
| Document (pitch, CGU…) | Document finalisé ET publié / transmis |
| Tracking / analytics | Tag en place, données qui remontent en production |

**Statuts intermédiaires :**

| Statut | Signification |
|--------|---------------|
| `À faire` | Pas encore démarré |
| `Rédigé` | Contenu produit (texte, brief, maquette) — pas encore implémenté |
| `En cours` | Implémentation en cours |
| `Terminé` | Opérationnel en production — vérifié |
| `Bloqué` | En attente d'un prérequis externe |

---

## 🌐 Site & Landing page

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-01 | Landing page — mise à jour multi-domaines, FAQ, sections V2 | Haute | `Terminé` | Fait le 2026-03-29 : domaines (travail/immigration/famille), 8 feature cards, FAQ 6 questions, CTA bas de page |
| M-02 | Mentions légales — rédaction | Haute | `Terminé` | Déployé en staging — /mentions-legales |
| M-03 | Politique de confidentialité — rédaction | Haute | `Terminé` | Déployé en staging — /privacy |
| M-04 | CGU — rédaction | Haute | `Terminé` | Déployé en staging — /cgu |
| M-05 | Page contact — formulaire email | Moyenne | `Terminé` | F-78 déployée en production 2026-03-30 — /contact opérationnel sur legalcase.ng-itconsulting.com. |
| M-06 | SEO — balises meta, Open Graph, sitemap | Moyenne | `Terminé` | PR #165 mergée 2026-03-30 — déployé en production via CI/CD |
| M-07 | Google Analytics / Plausible — intégration tracking | Moyenne | `Terminé` | SF-77-01 déployée en production 2026-03-30 — bannière consentement RGPD + GA4 G-2JPL8JTXE7 opérationnel sur legalcase.ng-itconsulting.com. Validé manuellement. |

---

## 🎥 Vidéo promotionnelle

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-10 | Brief designer — livré | Haute | `Terminé` | PDF + captures HD 8 écrans livrés le 2026-03-29 |
| M-11 | Réalisation vidéo 60-90s (format 16:9) | Haute | `À faire` | Basé sur le brief designer |
| M-12 | Déclinaison carré LinkedIn/Instagram (1:1) | Haute | `À faire` | Après livraison M-11 |
| M-13 | Déclinaison Stories/Reels (9:16) | Moyenne | `À faire` | Après livraison M-11 |
| M-14 | Teaser 15s pour bannière web | Moyenne | `À faire` | Après livraison M-11 |

---

## 📧 Email marketing

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-20 | Séquence onboarding — email J+0 (bienvenue) | Haute | `Terminé` | Validé en staging le 2026-03-29 |
| M-21 | Séquence onboarding — email J+2 (tip analyse) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-22 | Séquence onboarding — email J+5 (tip partage client) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-23 | Séquence onboarding — email J+12 (avant expiration trial) | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-24 | Email de conversion — FREE expiré | Haute | `Terminé` | Scheduler nightly 8h opérationnel en staging |
| M-25 | Choix d'un outil d'emailing + intégration | Haute | `Terminé` | Brevo déjà intégré via JavaMailSender SMTP — utilisé pour invitations, vérification email, reset MDP. Aucune action supplémentaire requise. |
| M-26 | Newsletter mensuelle — template | Basse | `Terminé` | PR #207 mergée 2026-04-01 — scheduler mensuel 1er du mois 8h, stats dynamiques par workspace, 24 features en rotation déterministe (2 ans), déduplication via email_sends |

---

## 💼 LinkedIn & réseaux sociaux

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-30 | Page entreprise LinkedIn — création | Haute | `Terminé` | Page créée le 2026-03-30 — URL : linkedin.com/company/ai-legalcase — bannière 1128×191px livrée |
| M-31 | Post de lancement LinkedIn (texte + vidéo) | Haute | `À faire` | Après M-11 |
| M-32 | Post "Comment ça marche" — carousel 5 slides | Haute | `Terminé` | PDF 6 slides publié le 2026-03-30 — docs/marketing/m32-carousel-comment-ca-marche.pdf |
| M-33 | Post témoignage / démo utilisateur | Moyenne | `À faire` | Dès les premiers clients |
| M-34 | Stratégie de contenu LinkedIn — planning 3 mois | Moyenne | `Terminé` | 28 posts planifiés avril-juin 2026 — intégré dans le workflow n8n M-38 |
| M-35 | Compte Twitter/X — création et premiers posts | Basse | `À faire` | Audience tech/legal |
| M-38 | Workflow n8n — auto-génération et publication LinkedIn | Haute | `Terminé` | Importé, activé et testé dans n8n — premier post publié avec succès sur la page LinkedIn AI LegalCase. |

---

## 🤝 Vente directe & partenariats

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-40 | Pitch deck — présentation cabinets | Haute | `Rédigé` | PPTX 11 slides livré le 2026-03-29 — docs/marketing/pitch-deck-ailegalcase.pptx |
| M-41 | Liste de 50 cabinets cibles (droit travail, Paris/IDF) | Haute | `Terminé` | 50 cabinets, 47 URLs LinkedIn vérifiées. Suivi outreach dans m41-suivi-outreach.md. 5 premiers messages envoyés le 2026-03-30. |
| M-42 | Script d'outreach LinkedIn — message de prospection | Haute | `Rédigé` | 4 templates livrés le 2026-03-29 — docs/marketing/m42-script-outreach-linkedin.md |
| M-43 | Démo en ligne — Calendly ou équivalent | Haute | `Terminé` | https://calendly.com/tounga-franck-ng-itconsulting/30min — intégré dans Template E de M-42 |
| M-44 | Partenariats barreaux / associations d'avocats | Moyenne | `À faire` | Canal d'acquisition B2B — email + cibles dans docs/marketing/m44-partenariats-barreaux.md |
| M-45 | Programme de référence — un mois offert pour parrainage | Moyenne | `À faire` | Croissance organique |
| M-53 | Partenariat éditorial Village de la Justice | Haute | `En cours` | Newsletter 60 000 professionnels du droit. Email de contact envoyé le 2026-04-01 via village-justice.com/articles/publiez-vous-publiez-ici-charte-publication,2846.html. En attente de réponse. Prochain message : brief de la tribune (angle "ce qui fonctionne vraiment en cabinet, ce qui ne fonctionne pas encore", 800-1 200 mots) — rédigé et prêt dans docs/marketing/m53-tribune-brief.md |

---

## 📊 Mesure & analytics

| ID | Action | Priorité | Statut | Notes |
|----|--------|----------|--------|-------|
| M-50 | Dashboard de conversion — inscription / activation / paiement | Haute | `Terminé` | Couvert par F-76 (super-admin) : totalWorkspaces, trialWorkspaces, paidWorkspaces, conversionRatePct, activeWorkspaces30d — déployé en production 2026-03-31 |
| M-51 | Tracking événements clés (analyse lancée, PDF exporté, lien partagé) | Moyenne | `Terminé` | analysis_launched (STANDARD+ENRICHED), pdf_exported, upgrade_clicked, docx_exported, chat_message_sent, share_link_created — déployé en prod via CI |
| M-52 | NPS — enquête satisfaction client | Basse | `À faire` | Dès 10 clients actifs |

---

## 📋 Priorités recommandées pour commencer

1. **M-02/03/04** — Mentions légales, CGV, RGPD (obligatoires légalement)
2. **M-25/20/21/22/23** — Séquence email onboarding (impact immédiat sur la conversion)
3. **M-40** — Pitch deck pour démos cabinets
4. **M-30/31** — Page LinkedIn + post de lancement
5. **M-11** — Vidéo promo (après le brief livré)
