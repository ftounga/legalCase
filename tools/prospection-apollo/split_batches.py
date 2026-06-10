#!/usr/bin/env python3
"""
Phase PERSONNALISE (1/2) : découpe le CSV domfit en lots TSV pour la génération d'accroches.
Sortie : batches/batch_N.tsv  (lignes : gid<TAB>domaine<TAB>companyName<TAB>specialites).
Le gid est GLOBAL (= index ligne du CSV) -> permet le remap par position au merge.
Affiche NBATColumns=N (le workflow lit ce nombre pour fan-out).
"""
import argparse, csv, glob, math, os

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", default="avocat-wave-domfit.csv")
    ap.add_argument("--size", type=int, default=46)
    args = ap.parse_args()
    here = os.path.dirname(os.path.abspath(__file__))
    path = args.csv if os.path.isabs(args.csv) else os.path.join(here, args.csv)
    bdir = os.path.join(here, "batches")
    os.makedirs(bdir, exist_ok=True)
    for f in glob.glob(os.path.join(bdir, "batch_*.tsv")):
        os.remove(f)  # repart propre
    with open(path, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    n = max(1, math.ceil(len(rows) / args.size))
    for b in range(n):
        chunk = rows[b * args.size:(b + 1) * args.size]
        with open(os.path.join(bdir, f"batch_{b+1}.tsv"), "w", encoding="utf-8") as f:
            for i, r in enumerate(chunk):
                gid = b * args.size + i
                f.write(f"{gid}\t{r['domaine']}\t{r['companyName']}\t{r['specialites']}\n")
    print(f"{len(rows)} contacts -> {n} lots de ~{args.size}")
    print(f"NBATCHES={n}")


if __name__ == "__main__":
    main()
