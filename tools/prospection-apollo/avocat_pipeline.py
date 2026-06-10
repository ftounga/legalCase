#!/usr/bin/env python3
"""
Pipeline avocats (phase SOURCE du workflow) :
  source (Apollo) -> enrich emails + spécialités cabinet -> filtre domaine-fit
  -> CSV domfit (domaine, firstName, companyName, email, linkedinUrl, specialites).

Réutilise apollo_avocat_search (search_domain, DOMAINS, clé). Enrichit via people/match
en capturant les org keywords (= spécialités) qui servent au filtre domaine-fit ET aux accroches.
"""
import argparse, csv, json, os, sys, time, urllib.request, urllib.error
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from apollo_avocat_search import get_api_key, search_domain, DOMAINS, MATCH_URL

FIT_TERMS = {
    "Droit du travail": ["droit du travail", "droit social"],
    "Droit de la famille": ["droit de la famille", "divorce", "autorite parentale", "autorité parentale",
                            "famille", "family law", "child custody", "prestation compensatoire",
                            "pension alimentaire", "liquidation", "succession", "régimes matrimoniaux", "regimes matrimoniaux"],
    "Immigration / droit des étrangers": ["etrangers", "étrangers", "immigration", "asile", "oqtf",
                                          "nationalit", "titre de sejour", "titre de séjour", "regroupement"],
}


def enrich(key, apollo_id):
    if not apollo_id:
        return "", ""
    req = urllib.request.Request(
        MATCH_URL, data=json.dumps({"id": apollo_id, "reveal_personal_emails": True}).encode(),
        headers={"Content-Type": "application/json", "X-Api-Key": key}, method="POST")
    try:
        b = json.loads(urllib.request.urlopen(req, timeout=30).read())
        p = b.get("person") or {}
        email = p.get("email", "") or ""
        if "email_not_unlocked" in email:
            email = ""
        org = p.get("organization") or {}
        kws = org.get("keywords") or []
        spec = ", ".join(kws[:8]) if kws else (org.get("industry", "") or "")
        return email, spec
    except Exception:  # noqa
        return "", ""


def fit(domaine, spec):
    s = (spec or "").lower()
    return any(t in s for t in FIT_TERMS.get(domaine, []))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--per-domain", type=int, default=100)
    ap.add_argument("--out", default="avocat-wave-domfit.csv")
    args = ap.parse_args()
    key = get_api_key()
    if not key:
        print("ERREUR clé Apollo absente (.apollo_key)", file=sys.stderr); sys.exit(1)

    seen, pool = set(), []
    for d in DOMAINS:
        for r in search_domain(key, d, args.per_domain, reveal_emails=False):
            k = (r["cabinet"] or "").strip().lower()
            if k and k not in seen:
                seen.add(k); pool.append(r)
    print(f"Pool brut: {len(pool)} cabinets uniques. Enrichissement...")

    kept = []
    for i, r in enumerate(pool, 1):
        email, spec = enrich(key, r.get("apollo_id", ""))
        if fit(r["domaine"], spec):
            kept.append({"domaine": r["domaine"], "firstName": r["nom"], "companyName": r["cabinet"],
                         "email": email, "linkedinUrl": r["linkedin"], "specialites": spec})
        if i % 50 == 0:
            print(f"  ...{i}/{len(pool)} (domaine-fit: {len(kept)})")
        time.sleep(0.35)

    out = args.out if os.path.isabs(args.out) else os.path.join(os.path.dirname(os.path.abspath(__file__)), args.out)
    fields = ["domaine", "firstName", "companyName", "email", "linkedinUrl", "specialites"]
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader(); w.writerows(kept)
    withmail = sum(1 for r in kept if "@" in (r["email"] or ""))
    print(f"\n✅ {len(kept)} domaine-fit ({withmail} avec email) -> {out}")


if __name__ == "__main__":
    main()
