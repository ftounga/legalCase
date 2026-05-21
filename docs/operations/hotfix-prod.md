# Hotfix prod — Tableau de bord

**Dernière analyse** : _(jamais exécuté — sera mis à jour au 1er run de la skill `prod-health-check`)_

> Ce fichier est **généré et maintenu** par la skill `ai-skills/prod-health-check.md`.
> Il liste les problèmes détectés en production que **l'humain** doit ensuite trier et corriger.
> La skill ne corrige rien — elle observe et répertorie.
>
> Pour lancer un audit : invoque la skill `/prod-health-check` ou demande *"lance le prod health check"*.

---

## 🔴 P0 — Production cassée (urgent)

_(vide)_

---

## 🟠 P1 — Dégradation significative

_(vide)_

---

## 🟡 P2 — Nuisance / bruit

_(vide)_

---

## ✅ Terminés (7 derniers jours)

_(vide)_

---

## 🗄️ Archive

Les items terminés depuis plus de 30 jours sont déplacés dans `docs/operations/hotfix-prod-archive.md`.

---

## 📋 Légende des statuts

| Statut | Signification |
|---|---|
| `À TRIER` | Nouveau, jamais examiné par l'humain |
| `À FAIRE` | Triés, hotfix à implémenter (priorisé) |
| `EN COURS` | Quelqu'un est en train de fixer (annoter avec qui + PR si dispo) |
| `IGNORÉ` | Examiné, bruit accepté ou faux positif (la skill ne le re-listera pas) |
| `✅ TERMINÉ` | Fixé. Mentionner `Fixed by #PR` permet à la skill de migrer auto |

## 📋 Légende des severities

| Severity | Critère typique |
|---|---|
| **P0** | Production cassée — pod en `CrashLoopBackOff` OU > 100 erreurs/h ininterrompues |
| **P1** | Dégradation significative — alarme prod en `ALARM` OU pattern récurrent > 20/24h |
| **P2** | Nuisance / bruit — alarme staging OU pattern < 20/24h OU dégradation < 25 % |

## 📋 Gabarit d'une entrée (rappel format)

```markdown
### HF-YYYY-MM-DD-NN — Titre court factuel
- **Détecté** : YYYY-MM-DDTHH:MM:SSZ (source : alarme ou pattern logs)
- **Première occurrence** : YYYY-MM-DD
- **Dernière occurrence** : YYYY-MM-DD
- **Occurrences 24h** : N
- **Total observé** : N
- **Signature** : `hash:abc123…`
- **Logs sample** : 2-3 lignes représentatives
- **Commit suspect** : `<sha>` ou `aucun`
- **Hypothèse** : analyse rapide
- **Status** : `À TRIER`
- **Notes** :
```

## 🔗 Voir aussi

- Skill : `ai-skills/prod-health-check.md`
- Patterns à ignorer (bruit accepté) : `docs/operations/hotfix-prod-noise-patterns.md`
- Archive : `docs/operations/hotfix-prod-archive.md`
