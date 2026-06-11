#!/usr/bin/env python3
"""DRH — phase Personnalise (1/2) : découpe drh-wave-domfit.csv en lots TSV.
Sortie : drh_batches/batch_N.tsv (gid<TAB>secteur<TAB>entreprise<TAB>specialites). Affiche NBATCHES=N."""
import argparse, csv, glob, math, os

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", default="drh-wave-domfit.csv")
    ap.add_argument("--size", type=int, default=24)
    args = ap.parse_args()
    here = os.path.dirname(os.path.abspath(__file__))
    path = args.csv if os.path.isabs(args.csv) else os.path.join(here, args.csv)
    bdir = os.path.join(here, "drh_batches")
    os.makedirs(bdir, exist_ok=True)
    for f in glob.glob(os.path.join(bdir, "batch_*.tsv")):
        os.remove(f)
    rows = list(csv.DictReader(open(path, encoding="utf-8")))
    n = max(1, math.ceil(len(rows) / args.size))
    for b in range(n):
        chunk = rows[b * args.size:(b + 1) * args.size]
        with open(os.path.join(bdir, f"batch_{b+1}.tsv"), "w", encoding="utf-8") as f:
            for i, r in enumerate(chunk):
                f.write(f"{b*args.size+i}\t{r['secteur']}\t{r['companyName']}\t{r['specialites']}\n")
    print(f"{len(rows)} DRH -> {n} lots")
    print(f"NBATCHES={n}")

if __name__ == "__main__":
    main()
