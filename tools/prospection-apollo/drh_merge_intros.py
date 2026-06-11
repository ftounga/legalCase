#!/usr/bin/env python3
"""DRH — phase Personnalise (2/2) : assemble le CSV final Lemlist.
- remap accroches par POSITION (robuste)
- nettoie : exclut entités PUBLIQUES, garde emails valides + prénoms valides
Sortie : drh-wave-lemlist.csv (email, firstName, companyName, linkedinUrl, introPerso, secteur)."""
import argparse, csv, os, re

PUBLIC = ["anssi", "asnr", "sncf", "ratp", "france travail", "pole emploi", "pôle emploi",
          "gendarmerie", "police", "ministère", "ministere", "préfecture", "prefecture", "mairie",
          "conseil régional", "conseil departemental", "université", "universite", "cnrs", "onf",
          "la poste", "edf", "enedis", "centre hospitalier", "chu", "chs", "hôpital", "hopital"]


def good_name(fn, co):
    if not fn or not re.match(r"^[A-Za-zÀ-ÿ][A-Za-zÀ-ÿ'\-]{1,24}$", fn):
        return False
    if fn.strip().lower() in co.strip().lower():
        return False
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", default="drh-wave-domfit.csv")
    ap.add_argument("--out", default="drh-wave-lemlist.csv")
    args = ap.parse_args()
    here = os.path.dirname(os.path.abspath(__file__))
    bdir = os.path.join(here, "drh_batches")
    cpath = args.csv if os.path.isabs(args.csv) else os.path.join(here, args.csv)

    intro, n = {}, 1
    while os.path.exists(os.path.join(bdir, f"batch_{n}.tsv")):
        src = open(os.path.join(bdir, f"batch_{n}.tsv"), encoding="utf-8").read().splitlines()
        op = os.path.join(bdir, f"batch_{n}_out.tsv")
        out = [l for l in (open(op, encoding="utf-8").read().splitlines() if os.path.exists(op) else []) if "\t" in l]
        for s, o in zip(src, out):
            intro[int(s.split("\t")[0])] = o.split("\t", 1)[1].strip()
        n += 1

    rows = list(csv.DictReader(open(cpath, encoding="utf-8")))
    keep, dp, de, dn = [], 0, 0, 0
    for gid, r in enumerate(rows):
        co = r["companyName"]
        if any(p in co.lower() for p in PUBLIC):
            dp += 1; continue
        if "@" not in (r["email"] or ""):
            de += 1; continue
        if not good_name(r["firstName"], co):
            dn += 1; continue
        ip = intro.get(gid, "")
        if not ip:
            continue
        keep.append({"email": r["email"], "firstName": r["firstName"], "companyName": co,
                     "linkedinUrl": r["linkedinUrl"], "introPerso": ip, "secteur": r["secteur"]})
    out = args.out if os.path.isabs(args.out) else os.path.join(here, args.out)
    fields = ["email", "firstName", "companyName", "linkedinUrl", "introPerso", "secteur"]
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader(); w.writerows(keep)
    print(f"✅ {len(keep)} DRH -> {out} (exclus: public {dp}, sans email {de}, prénom invalide {dn})")


if __name__ == "__main__":
    main()
