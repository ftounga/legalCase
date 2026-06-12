#!/usr/bin/env python3
"""
Pipeline DRH (vague employeur) : source Apollo (5 secteurs) -> exclut hôpitaux PUBLICS
+ déjà-contactés -> enrich emails + spécialités -> CSV domfit.
Cible : DRH/DAS/Resp RH, ETI 201-2000, France. Réutilise get_api_key + endpoint api_search.

--no-enrich : sourcing seul (gratuit) pour jauger les viviers par secteur.
"""
import argparse, base64, csv, json, os, sys, time, urllib.request, urllib.error
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from apollo_avocat_search import get_api_key

API_URL = "https://api.apollo.io/api/v1/mixed_people/api_search"
MATCH_URL = "https://api.apollo.io/api/v1/people/match"
UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/124.0 Safari/537.36"

TITLES = ["Directeur des Ressources Humaines", "DRH", "Directrice des Ressources Humaines",
          "Directeur des Affaires Sociales", "Responsable Ressources Humaines", "Responsable RH",
          "DRH Groupe", "Directeur des Relations Sociales"]
RANGES = ["201,500", "501,1000", "1001,2000"]

SECTORS = {
    "securite": {"label": "Sécurité privée / surveillance",
                 "keywords": ["sécurité privée", "gardiennage", "surveillance humaine", "security services", "sûreté"]},
    "proprete": {"label": "Propreté / facility",
                 "keywords": ["propreté", "nettoyage", "facility management", "facility services", "nettoyage industriel"]},
    "transport": {"label": "Transport / logistique",
                  "keywords": ["transport", "logistique", "logistics", "supply chain", "messagerie"]},
    "restauration": {"label": "Restauration / hôtellerie",
                     "keywords": ["restauration", "restaurant", "hôtellerie", "hospitality", "restauration collective"]},
    "medico": {"label": "Médico-social privé",
               "keywords": ["ehpad", "médico-social", "clinique", "maison de retraite", "mutualité", "soins"]},
}
# noms d'orga = secteur PUBLIC -> exclure (fonction publique hospitalière, hors prud'hommes)
EXCLUDE_PUBLIC = ["centre hospitalier", "chu ", "chu de", "chs ", "hôpital", "hopital", "ch de", "ch d'",
                  "assistance publique", "ap-hp", "aphp", "groupe hospitalier", "ght "]


LOCATION = ["France"]  # surchargé par --country (BE = Belgique francophone)


def fetch(key, sector, page):
    payload = {"person_titles": TITLES, "organization_num_employees_ranges": RANGES,
               "person_locations": LOCATION, "q_organization_keyword_tags": sector["keywords"],
               "q_keywords": "ressources humaines", "page": page, "per_page": 100}
    req = urllib.request.Request(API_URL, data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json", "X-Api-Key": key, "User-Agent": UA}, method="POST")
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read())


def search_sector(key, skey, target, max_pages=5):
    sec = SECTORS[skey]
    rows, seen = [], set()
    for page in range(1, max_pages + 1):
        try:
            body = fetch(key, sec, page)
        except Exception as e:  # noqa
            print(f"[{skey}] p{page} err: {str(e)[:80]}", file=sys.stderr); break
        people = body.get("people", []) or []
        if not people:
            break
        for p in people:
            org = p.get("organization") or {}
            name = (org.get("name", "") or "")
            low = name.lower()
            if any(x in low for x in EXCLUDE_PUBLIC):
                continue  # hôpital public
            k = low.strip()
            if not k or k in seen:
                continue
            seen.add(k)
            rows.append({"secteur": sec["label"], "firstName": p.get("first_name", "") or (p.get("name", "") or "").split(" ")[0],
                         "companyName": name, "apollo_id": p.get("id", ""),
                         "email": "", "linkedinUrl": p.get("linkedin_url", ""), "specialites": ""})
        if len(rows) >= target:
            break
        time.sleep(0.4)
    print(f"[{skey}] {sec['label']} : {len(rows)} (hors public)")
    return rows[:target]


def enrich(key, apollo_id):
    if not apollo_id:
        return "", ""
    req = urllib.request.Request(MATCH_URL, data=json.dumps({"id": apollo_id, "reveal_personal_emails": True}).encode(),
        headers={"Content-Type": "application/json", "X-Api-Key": key, "User-Agent": UA}, method="POST")
    try:
        b = json.loads(urllib.request.urlopen(req, timeout=30).read())
        p = b.get("person") or {}
        em = p.get("email", "") or ""
        if "email_not_unlocked" in em:
            em = ""
        org = p.get("organization") or {}
        kws = org.get("keywords") or []
        return em, (", ".join(kws[:8]) if kws else org.get("industry", "") or "")
    except Exception:  # noqa
        return "", ""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--per-sector", type=int, default=20)
    ap.add_argument("--country", default="FR", choices=["FR", "BE"])
    ap.add_argument("--no-enrich", action="store_true")
    ap.add_argument("--out", default="drh-wave-domfit.csv")
    args = ap.parse_args()
    global LOCATION
    LOCATION = ["Belgium"] if args.country == "BE" else ["France"]
    print(f"Pays : {args.country} (localisation {LOCATION})")
    key = get_api_key()
    if not key:
        print("clé Apollo absente", file=sys.stderr); sys.exit(1)

    # exclure les déjà-contactés (1re vague DRH)
    here = os.path.dirname(os.path.abspath(__file__))
    already = set()
    for fn in ["drh-leads.csv"]:
        fp = os.path.join(here, fn)
        if os.path.exists(fp):
            for r in csv.DictReader(open(fp, encoding="utf-8")):
                already.add((r.get("entreprise", "") or r.get("companyName", "")).strip().lower())

    pool = []
    for s in SECTORS:
        for r in search_sector(key, s, args.per_sector):
            if r["companyName"].strip().lower() in already:
                continue
            pool.append(r)
    print(f"\nPool total (hors public + hors déjà-contactés): {len(pool)}")
    if args.no_enrich:
        from collections import Counter
        print("Par secteur:", dict(Counter(r["secteur"] for r in pool)))
        print("(--no-enrich : pas d'email/spécialité, pas de crédit dépensé)")
        return

    print("Enrichissement (emails + spécialités)...")
    for i, r in enumerate(pool, 1):
        r["email"], r["specialites"] = enrich(key, r["apollo_id"])
        if i % 30 == 0:
            print(f"  ...{i}/{len(pool)}")
        time.sleep(0.35)
    out = args.out if os.path.isabs(args.out) else os.path.join(here, args.out)
    fields = ["secteur", "firstName", "companyName", "email", "linkedinUrl", "specialites", "apollo_id"]
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader(); w.writerows(pool)
    got = sum(1 for r in pool if "@" in (r["email"] or ""))
    print(f"\n✅ {len(pool)} DRH ({got} avec email) -> {out}")


if __name__ == "__main__":
    main()
