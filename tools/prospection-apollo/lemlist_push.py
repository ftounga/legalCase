#!/usr/bin/env python3
"""
Connecteur CSV -> Lemlist : pousse des leads (avec leurs variables perso) dans une
campagne Lemlist EXISTANTE via l'API — plus besoin d'import CSV manuel dans l'UI.

Usage prochaine vague :
  1) sourcer + enrichir + filtrer + personnaliser (scripts Apollo + accroches) -> un CSV
     au même format que lemlist-import-master.csv
     (colonnes : email, firstName, companyName, linkedinUrl, introPerso, subject, valueProp, domainNoun, domaine)
  2) python3 lemlist_push.py --csv ma-nouvelle-vague.csv

Pré-requis :
  - Clé API Lemlist dans tools/prospection-apollo/.lemlist_key (gitignoré)
    (Lemlist : Settings -> Integrations -> API)
  - Campagne déjà montée (séquence + sender + réglages) — ici « Avocats — 3 domaines ».

Auth Lemlist = HTTP Basic, user vide, password = clé API.
Endpoint v1 : POST https://api.lemlist.com/api/campaigns/{campaignId}/leads/{email}
Les champs custom du body deviennent les variables {{introPerso}} etc.

NB : ne pousse que les leads AVEC email (la campagne actuelle est email-only).
Les LinkedIn-only nécessiteraient le plan Multichannel + un autre endpoint.
"""

import argparse
import base64
import csv
import json
import os
import sys
import time
import urllib.request
import urllib.error

API_BASE = "https://api.lemlist.com/api"
CAMPAIGN_ID = "cam_ZhtYzZwA6H8kXZmjz"  # « Avocats — 3 domaines » (team tea_k3RLqZqghX5Kq7BAX)
KEY_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".lemlist_key")

# Champs custom envoyés à Lemlist (deviennent des variables {{...}})
CUSTOM_FIELDS = ["firstName", "companyName", "linkedinUrl", "introPerso",
                 "subject", "valueProp", "domainNoun", "domaine"]


def get_key():
    k = os.environ.get("LEMLIST_API_KEY")
    if k:
        return k.strip()
    if os.path.exists(KEY_FILE):
        return open(KEY_FILE, encoding="utf-8").read().strip()
    return None


def push_lead(key, campaign_id, row):
    email = (row.get("email") or "").strip()
    if not email or "@" not in email:
        return "skip-no-email"
    url = f"{API_BASE}/campaigns/{campaign_id}/leads/{urllib.parse.quote(email)}"
    body = {f: row.get(f, "") for f in CUSTOM_FIELDS if row.get(f)}
    data = json.dumps(body).encode("utf-8")
    auth = base64.b64encode(f":{key}".encode()).decode()
    req = urllib.request.Request(
        url, data=data,
        headers={"Content-Type": "application/json", "Authorization": f"Basic {auth}"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            resp.read()
        return "ok"
    except urllib.error.HTTPError as e:
        return f"HTTP {e.code}: {e.read().decode('utf-8','ignore')[:120]}"
    except Exception as e:  # noqa
        return f"err: {e}"


def main():
    import urllib.parse  # noqa
    p = argparse.ArgumentParser(description="Push CSV leads -> campagne Lemlist")
    p.add_argument("--csv", default="lemlist-import-master.csv")
    p.add_argument("--campaign", default=CAMPAIGN_ID)
    p.add_argument("--dry-run", action="store_true", help="compter sans pousser")
    args = p.parse_args()

    key = get_key()
    if not key:
        print("ERREUR : clé API Lemlist absente. Settings>Integrations>API, puis écris-la dans .lemlist_key", file=sys.stderr)
        sys.exit(1)

    path = args.csv if os.path.isabs(args.csv) else os.path.join(os.path.dirname(os.path.abspath(__file__)), args.csv)
    with open(path, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    withmail = [r for r in rows if "@" in (r.get("email") or "")]
    print(f"{len(rows)} lignes | {len(withmail)} avec email -> campagne {args.campaign}")
    if args.dry_run:
        print("(dry-run : rien poussé)")
        return

    ok = 0
    stats = {}
    for i, r in enumerate(withmail, 1):
        res = push_lead(key, args.campaign, r)
        stats[res if res in ("ok",) else res.split(":")[0]] = stats.get(res if res == "ok" else res.split(":")[0], 0) + 1
        if res == "ok":
            ok += 1
        elif "HTTP 4" in res or "err" in res:
            print(f"  {r.get('email')}: {res}", file=sys.stderr)
        if i % 50 == 0:
            print(f"  ...{i}/{len(withmail)} (ok {ok})")
        time.sleep(0.3)
    print(f"\n✅ {ok}/{len(withmail)} leads poussés. Détail: {stats}")


if __name__ == "__main__":
    import urllib.parse  # noqa
    main()
