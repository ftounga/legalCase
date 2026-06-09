#!/usr/bin/env python3
"""
Sourcing DRH via l'API Apollo — pour le TEST discovery LegalCase Employeur.

Objectif : récupérer ~2 DRH par secteur (5 secteurs) = ~10 contacts à qui
envoyer les messages d'approche « je ne vends rien, je veux votre avis ».
Discovery, PAS acquisition de masse. Reste volontairement à petit volume.

Pré-requis :
  - Un compte Apollo + une clé API (Settings > Integrations > API).
  - export APOLLO_API_KEY="xxxxx"

Usage :
  python3 apollo_drh_search.py                # tous les secteurs, 2 par secteur
  python3 apollo_drh_search.py --per-sector 4 # 4 par secteur
  python3 apollo_drh_search.py --sector securite

Sortie : drh-leads.csv (nom, titre, entreprise, taille, LinkedIn, secteur).
Note : l'email pro n'est PAS révélé par la recherche seule. Apollo verrouille
l'email tant qu'on ne consomme pas un crédit d'enrichissement (--reveal-emails).
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.request
import urllib.error

API_URL = "https://api.apollo.io/api/v1/mixed_people/search"

# Titres DRH ciblés (Apollo fait du matching flou, on ratisse FR + EN)
PERSON_TITLES = [
    "Directeur des Ressources Humaines",
    "DRH",
    "Directrice des Ressources Humaines",
    "Responsable Ressources Humaines",
    "Directeur des Affaires Sociales",
    "Responsable des Relations Sociales",
    "HR Director",
    "Human Resources Director",
    "Head of HR",
]

# ETI 200-1500 salariés -> tranches Apollo
EMPLOYEE_RANGES = ["201,500", "501,1000", "1001,2000"]

LOCATIONS = ["France"]

# 5 secteurs du plan discovery -> mots-clés organisation Apollo
SECTORS = {
    "securite": {
        "label": "Sécurité privée / surveillance",
        "keywords": ["sécurité privée", "surveillance", "gardiennage", "security services"],
    },
    "proprete": {
        "label": "Propreté / facility management",
        "keywords": ["propreté", "nettoyage", "facility management", "facility services"],
    },
    "transport": {
        "label": "Transport / logistique",
        "keywords": ["transport", "logistique", "logistics", "supply chain"],
    },
    "restauration": {
        "label": "Restauration de chaîne / hôtellerie",
        "keywords": ["restauration", "hôtellerie", "restaurant chain", "hospitality"],
    },
    "medico_social": {
        "label": "Médico-social / EHPAD privés",
        "keywords": ["EHPAD", "médico-social", "maison de retraite", "soins"],
    },
}


def search_sector(api_key, sector_key, per_sector, reveal_emails):
    sector = SECTORS[sector_key]
    payload = {
        "person_titles": PERSON_TITLES,
        "organization_num_employees_ranges": EMPLOYEE_RANGES,
        "person_locations": LOCATIONS,
        "q_organization_keyword_tags": sector["keywords"],
        "page": 1,
        "per_page": max(per_sector, 5),
        "reveal_personal_emails": reveal_emails,
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        API_URL,
        data=data,
        headers={
            "Content-Type": "application/json",
            "Cache-Control": "no-cache",
            "X-Api-Key": api_key,
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        msg = e.read().decode("utf-8", "ignore")
        print(f"[{sector_key}] HTTP {e.code} : {msg}", file=sys.stderr)
        return []
    except Exception as e:  # noqa
        print(f"[{sector_key}] erreur : {e}", file=sys.stderr)
        return []

    people = body.get("people", []) or body.get("contacts", [])
    rows = []
    for p in people[:per_sector]:
        org = p.get("organization") or {}
        rows.append({
            "secteur": sector["label"],
            "nom": (p.get("name") or f"{p.get('first_name','')} {p.get('last_name','')}").strip(),
            "titre": p.get("title", ""),
            "entreprise": org.get("name", ""),
            "taille": org.get("estimated_num_employees", ""),
            "email": p.get("email", "") or "(verrouillé — enrichir)",
            "linkedin": p.get("linkedin_url", ""),
        })
    print(f"[{sector_key}] {sector['label']} : {len(rows)} contact(s)")
    return rows


def main():
    parser = argparse.ArgumentParser(description="Sourcing DRH via Apollo (discovery LegalCase Employeur)")
    parser.add_argument("--per-sector", type=int, default=2, help="contacts par secteur (défaut 2)")
    parser.add_argument("--sector", choices=list(SECTORS), help="limiter à un secteur")
    parser.add_argument("--reveal-emails", action="store_true", help="révéler les emails (consomme des crédits Apollo)")
    parser.add_argument("--out", default="drh-leads.csv", help="fichier CSV de sortie")
    args = parser.parse_args()

    api_key = os.environ.get("APOLLO_API_KEY")
    if not api_key:
        print("ERREUR : exporte d'abord ta clé -> export APOLLO_API_KEY=\"xxxx\"", file=sys.stderr)
        sys.exit(1)

    sectors = [args.sector] if args.sector else list(SECTORS)
    all_rows = []
    for s in sectors:
        all_rows.extend(search_sector(api_key, s, args.per_sector, args.reveal_emails))
        time.sleep(1)  # courtoisie rate-limit

    if not all_rows:
        print("Aucun contact récupéré. Vérifie la clé API / le plan Apollo (l'API exige un plan payant).")
        sys.exit(2)

    fields = ["secteur", "nom", "titre", "entreprise", "taille", "email", "linkedin"]
    with open(args.out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(all_rows)
    print(f"\n✅ {len(all_rows)} contact(s) écrit(s) dans {args.out}")


if __name__ == "__main__":
    main()
