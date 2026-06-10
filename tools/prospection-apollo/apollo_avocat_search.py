#!/usr/bin/env python3
"""
Sourcing AVOCATS via l'API Apollo — test PMF acquisition avocat (M-79, 3 domaines).

Cible : cabinets d'avocats MULTI-AVOCATS (pas de solos), France, sur les 3 domaines
que LegalCase sert (droit du travail, immigration/étrangers, droit de la famille).

Pré-requis : plan Apollo payant (l'API exige un plan payant).
Clé API : Apollo > Settings > Integrations > API > copier la clé. Puis, AU CHOIX :
  - export APOLLO_API_KEY="xxxx"
  - ou écrire la clé seule dans tools/prospection-apollo/.apollo_key (gitignoré)

Usage :
  python3 apollo_avocat_search.py                 # 3 domaines, 10 contacts/domaine
  python3 apollo_avocat_search.py --per-domain 15
  python3 apollo_avocat_search.py --domain immigration
  python3 apollo_avocat_search.py --no-reveal-emails

Sortie : avocat-leads.csv (domaine, nom, titre, cabinet, taille, email, linkedin).
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.request
import urllib.error

API_URL = "https://api.apollo.io/api/v1/mixed_people/api_search"
MATCH_URL = "https://api.apollo.io/api/v1/people/match"
KEY_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".apollo_key")

# Titres DÉCIDEURS uniquement (l'associé/gérant/fondateur décide de l'achat).
# Leçon export manuel 09/06 : le titre "Avocat" seul ramène tous les COLLABORATEURS
# ("Associate Lawyer", "Collaborateur libéral") → on le retire.
PERSON_TITLES = [
    "Avocat associé", "Associé", "Associé gérant", "Avocat fondateur",
    "Associé fondateur", "Managing Partner", "Gérant",
]
# Titres / entités à exclure au post-traitement (collaborateurs, notaires, ordres)
EXCLUDE_TITLE = ("collaborateur", "associate", "collaborator", "notaire", "stagiaire", "élève")
EXCLUDE_ORG = ("notaire", "ordre des avocats", "barreau")

# Cabinets multi-avocats -> on saute les solos (<10)
EMPLOYEE_RANGES = ["11,20", "21,50", "51,100", "101,200"]

LOCATIONS = ["France"]

# 3 domaines produits -> mots-clés de spécialité
DOMAINS = {
    "travail": {
        "label": "Droit du travail",
        "keywords": ["droit du travail", "droit social", "employment law", "labour law",
                     "droit de la sécurité sociale", "relations sociales", "contentieux prud'homal"],
        # élargi pour viser le volume : inclure solos/petits cabinets (le solo = décideur)
        "ranges": ["1,10", "11,20", "21,50", "51,100", "101,200"],
        "titles": ["Avocat associé", "Associé", "Avocat fondateur", "Associé fondateur",
                   "Managing Partner", "Gérant", "Avocat", "Avocat à la Cour", "Fondateur"],
        "email_status": None,
    },
    "immigration": {
        "label": "Immigration / droit des étrangers",
        "keywords": ["droit des étrangers", "droit de l'immigration", "immigration", "immigration law",
                     "asile", "nationalité", "regroupement familial", "titre de séjour", "naturalisation"],
        "ranges": ["1,10", "11,20", "21,50", "51,100", "101,200"],
        # niche restreinte -> élargir les titres (les solos SONT les décideurs) + ne pas exiger email vérifié
        "titles": ["Avocat associé", "Associé", "Avocat fondateur", "Associé fondateur",
                   "Managing Partner", "Gérant", "Avocat", "Avocat à la Cour", "Fondateur"],
        "email_status": None,
    },
    "famille": {
        "label": "Droit de la famille",
        "keywords": ["droit de la famille", "divorce", "droit patrimonial", "famille", "divorce law", "family law",
                     "successions", "régimes matrimoniaux", "autorité parentale", "droit des personnes"],
        # titres/tailles élargis (beaucoup de petits cabinets famille / solos décideurs)
        "ranges": ["1,10", "11,20", "21,50", "51,100", "101,200"],
        "titles": ["Avocat associé", "Associé", "Avocat fondateur", "Associé fondateur",
                   "Managing Partner", "Gérant", "Avocat", "Avocat à la Cour", "Fondateur"],
        "email_status": None,
    },
}


def get_api_key():
    key = os.environ.get("APOLLO_API_KEY")
    if key:
        return key.strip()
    if os.path.exists(KEY_FILE):
        with open(KEY_FILE, encoding="utf-8") as f:
            return f.read().strip()
    return None


def _fetch_page(api_key, dom, page, reveal_emails):
    payload = {
        "person_titles": dom.get("titles", PERSON_TITLES),
        "organization_num_employees_ranges": dom.get("ranges", EMPLOYEE_RANGES),
        "person_locations": LOCATIONS,
        "q_keywords": dom.get("q_keywords", "avocat"),
        "page": page,
        "per_page": 100,
        "reveal_personal_emails": reveal_emails,
    }
    # matching par tag cabinet sauf si le domaine privilégie le mot-clé profil
    if not dom.get("drop_org_tags"):
        payload["q_organization_keyword_tags"] = dom["keywords"]
    # emails vérifiés par défaut (élimine les opt-out) ; certains domaines (immigration) relâchent
    if dom.get("email_status", ["verified"]) is not None:
        payload["contact_email_status"] = dom.get("email_status", ["verified"])
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        API_URL, data=data,
        headers={"Content-Type": "application/json", "Cache-Control": "no-cache", "X-Api-Key": api_key},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))


def search_domain(api_key, domain_key, per_domain, reveal_emails, max_pages=8):
    """Paginé : récupère assez de cabinets UNIQUES pour atteindre per_domain."""
    dom = DOMAINS[domain_key]
    rows, seen = [], set()
    for page in range(1, max_pages + 1):
        try:
            body = _fetch_page(api_key, dom, page, reveal_emails)
        except urllib.error.HTTPError as e:
            print(f"[{domain_key}] HTTP {e.code} p{page} : {e.read().decode('utf-8','ignore')[:120]}", file=sys.stderr)
            break
        except Exception as e:  # noqa
            print(f"[{domain_key}] erreur p{page} : {e}", file=sys.stderr)
            break
        people = body.get("people", []) or body.get("contacts", [])
        if not people:
            break
        for p in people:
            org = p.get("organization") or {}
            title = (p.get("title", "") or "").lower()
            cabinet = org.get("name", "") or ""
            if any(k in title for k in EXCLUDE_TITLE):
                continue
            if any(k in cabinet.lower() for k in EXCLUDE_ORG):
                continue
            key = cabinet.strip().lower()
            if not key or key in seen:
                continue  # 1 contact par cabinet
            seen.add(key)
            rows.append({
                "domaine": dom["label"],
                "apollo_id": p.get("id", ""),
                "nom": (p.get("name") or f"{p.get('first_name','')} {p.get('last_name','')}").strip(),
                "titre": p.get("title", ""),
                "cabinet": cabinet,
                "taille": org.get("estimated_num_employees", ""),
                "email": p.get("email", "") or "",
                "email_status": p.get("email_status", ""),
                "linkedin": p.get("linkedin_url", ""),
            })
        print(f"[{domain_key}] p{page}: cumul {len(rows)} cabinets uniques")
        if len(rows) >= per_domain:
            break
        time.sleep(0.5)
    print(f"[{domain_key}] {dom['label']} : {len(rows)} cabinets uniques au total")
    return rows[:per_domain]


def enrich_email(api_key, row):
    """Passe d'enrichissement : révèle l'email pro via people/match (consomme 1 crédit)."""
    payload = {"reveal_personal_emails": True}
    if row.get("apollo_id"):
        payload["id"] = row["apollo_id"]
    elif row.get("linkedin"):
        payload["linkedin_url"] = row["linkedin"]
    else:
        return
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        MATCH_URL, data=data,
        headers={"Content-Type": "application/json", "Cache-Control": "no-cache", "X-Api-Key": api_key},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = json.loads(resp.read().decode("utf-8"))
        person = body.get("person") or {}
        email = person.get("email", "")
        if email and "email_not_unlocked" not in email:
            row["email"] = email
            row["email_status"] = person.get("email_status", "") or row.get("email_status", "")
    except urllib.error.HTTPError as e:
        print(f"   enrich {row['nom']}: HTTP {e.code} {e.read().decode('utf-8','ignore')[:120]}", file=sys.stderr)
    except Exception as e:  # noqa
        print(f"   enrich {row['nom']}: {e}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description="Sourcing avocats via Apollo (M-79, 3 domaines)")
    parser.add_argument("--per-domain", type=int, default=10, help="contacts par domaine (défaut 10)")
    parser.add_argument("--domain", choices=list(DOMAINS), help="limiter à un domaine")
    parser.add_argument("--no-reveal-emails", action="store_true", help="ne pas révéler les emails (économise des crédits)")
    parser.add_argument("--no-enrich", action="store_true", help="ne pas faire la passe d'enrichissement people/match")
    parser.add_argument("--out", default="avocat-leads.csv", help="fichier CSV de sortie")
    args = parser.parse_args()

    api_key = get_api_key()
    if not api_key:
        print("ERREUR : clé API absente. export APOLLO_API_KEY=\"xxx\" ou écris-la dans .apollo_key", file=sys.stderr)
        sys.exit(1)

    domains = [args.domain] if args.domain else list(DOMAINS)
    all_rows = []
    seen_cabinets = set()  # 1 seul contact par cabinet (anti Doria ×11 / Axiome ×9)
    for d in domains:
        pool = search_domain(api_key, d, args.per_domain, not args.no_reveal_emails)
        kept = 0
        for r in pool:
            key = (r["cabinet"] or r["email"]).lower()
            if key in seen_cabinets:
                continue
            seen_cabinets.add(key)
            all_rows.append(r)
            kept += 1
            if kept >= args.per_domain:
                break
        print(f"   -> {kept} retenu(s) après dédoublonnage par cabinet")
        time.sleep(1)

    if not all_rows:
        print("Aucun contact. Vérifie la clé / le plan Apollo (l'API exige un plan payant) et les quotas.")
        sys.exit(2)

    if not args.no_enrich:
        print(f"\nEnrichissement des emails ({len(all_rows)} contacts)...")
        for r in all_rows:
            if not r.get("email"):
                enrich_email(api_key, r)
                time.sleep(0.4)
        got = sum(1 for r in all_rows if r.get("email") and "@" in r["email"])
        print(f"   -> {got}/{len(all_rows)} emails révélés")

    fields = ["domaine", "nom", "titre", "cabinet", "taille", "email", "email_status", "linkedin", "apollo_id"]
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), args.out)
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(all_rows)
    print(f"\n✅ {len(all_rows)} contact(s) écrit(s) dans {out}")


if __name__ == "__main__":
    main()
