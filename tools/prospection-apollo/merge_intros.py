#!/usr/bin/env python3
"""
Phase PERSONNALISE (2/2) : assemble le CSV final Lemlist.
- remap des accroches par POSITION (robuste : certains agents renumérotent les gid)
- ajoute subject / valueProp / domainNoun par domaine
- corrige les accents des prénoms
Sortie : CSV (email, firstName, companyName, linkedinUrl, introPerso, subject, valueProp, domainNoun, domaine).
"""
import argparse, csv, os

# Messages par PAYS puis par domaine. FR = droit français ; BE = droit belge francophone.
SUBJ_BY = {
 "FR": {"Droit du travail": "Analyse de dossier prud'homal, du dépôt aux conclusions",
        "Droit de la famille": "Préparation de dossier de divorce : liquidation, pension, prestation compensatoire",
        "Immigration / droit des étrangers": "Qualification de dossier en droit des étrangers, délais sécurisés"},
 "BE": {"Droit du travail": "Analyse de dossier devant le tribunal du travail, du dépôt aux conclusions",
        "Droit de la famille": "Préparation de dossier de divorce : liquidation, pension, contribution alimentaire",
        "Immigration / droit des étrangers": "Qualification de dossier en droit des étrangers (loi du 15/12/1980), délais sécurisés"},
}
VP_BY = {
 "FR": {"Droit du travail": "LegalCase analyse les pièces d'un dossier prud'homal pour chiffrer l'exposition (indemnités, barème), repérer les vices de procédure et préparer des conclusions argumentées, jurisprudence à l'appui — un gain de temps concret sur la préparation, au service de votre jugement.",
        "Droit de la famille": "LegalCase analyse les pièces d'un dossier familial pour structurer la liquidation de communauté, chiffrer les fourchettes de prestation compensatoire et de pension alimentaire (barèmes), et cadrer la procédure de divorce — un gain de temps concret sur la préparation, au service de votre jugement.",
        "Immigration / droit des étrangers": "LegalCase analyse les pièces d'un dossier de droit des étrangers pour qualifier la situation (titre de séjour, recours), vérifier les conditions de validité et sécuriser les délais (CESEDA), puis préparer des écritures argumentées — un gain de temps concret sur des dossiers volumineux et répétitifs."},
 "BE": {"Droit du travail": "LegalCase analyse les pièces d'un dossier porté devant le tribunal du travail pour chiffrer l'exposition (indemnité de préavis — loi du 26/12/2013 — et indemnité pour licenciement manifestement déraisonnable — CCT 109), repérer les fragilités de procédure et préparer des conclusions argumentées — un gain de temps concret sur la préparation, au service de votre jugement.",
        "Droit de la famille": "LegalCase analyse les pièces d'un dossier familial pour structurer la liquidation du régime matrimonial, chiffrer les fourchettes de contribution alimentaire et de prestations, et cadrer la procédure devant le tribunal de la famille — un gain de temps concret sur la préparation, au service de votre jugement.",
        "Immigration / droit des étrangers": "LegalCase analyse les pièces d'un dossier de droit des étrangers pour qualifier la situation (séjour, recours au CCE), vérifier les conditions de validité et sécuriser les délais (loi du 15/12/1980), puis préparer des écritures argumentées — un gain de temps concret sur des dossiers volumineux et répétitifs."},
}
NOUN = {"Droit du travail": "droit social", "Droit de la famille": "droit de la famille",
        "Immigration / droit des étrangers": "droit des étrangers"}
FIX = {"Aurelie": "Aurélie", "Frederic": "Frédéric", "Sebastien": "Sébastien", "Andrea": "Andréa",
       "Helene": "Hélène", "Eloise": "Éloïse", "Eleonore": "Éléonore", "Cecile": "Cécile",
       "Stephane": "Stéphane", "Stephanie": "Stéphanie", "Jerome": "Jérôme", "Herve": "Hervé",
       "Benedicte": "Bénédicte", "Gaelle": "Gaëlle", "Agnes": "Agnès", "Remi": "Rémi",
       "Andre": "André", "Aurelien": "Aurélien", "Celine": "Céline", "Anais": "Anaïs"}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", default="avocat-wave-domfit.csv")
    ap.add_argument("--out", default="avocat-wave-lemlist.csv")
    ap.add_argument("--country", default="FR", choices=["FR", "BE"])
    args = ap.parse_args()
    SUBJ = SUBJ_BY[args.country]
    VP = VP_BY[args.country]
    here = os.path.dirname(os.path.abspath(__file__))
    bdir = os.path.join(here, "batches")
    cpath = args.csv if os.path.isabs(args.csv) else os.path.join(here, args.csv)

    # remap accroches par position dans chaque lot
    intro = {}
    n = 1
    while os.path.exists(os.path.join(bdir, f"batch_{n}.tsv")):
        src = open(os.path.join(bdir, f"batch_{n}.tsv"), encoding="utf-8").read().splitlines()
        outp = os.path.join(bdir, f"batch_{n}_out.tsv")
        out = open(outp, encoding="utf-8").read().splitlines() if os.path.exists(outp) else []
        out = [l for l in out if "\t" in l]
        for s, o in zip(src, out):
            gid = int(s.split("\t")[0])
            intro[gid] = o.split("\t", 1)[1].strip()
        n += 1

    with open(cpath, encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    fields = ["email", "firstName", "companyName", "linkedinUrl", "introPerso", "subject", "valueProp", "domainNoun", "domaine"]
    res, miss = [], 0
    for gid, r in enumerate(rows):
        ip = intro.get(gid)
        if not ip:
            miss += 1; continue
        d = r["domaine"]
        res.append({"email": r["email"] if "@" in (r["email"] or "") else "",
                    "firstName": FIX.get(r["firstName"], r["firstName"]),
                    "companyName": r["companyName"], "linkedinUrl": r["linkedinUrl"],
                    "introPerso": ip, "subject": SUBJ.get(d, ""), "valueProp": VP.get(d, ""),
                    "domainNoun": NOUN.get(d, ""), "domaine": d})
    out = args.out if os.path.isabs(args.out) else os.path.join(here, args.out)
    with open(out, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields); w.writeheader(); w.writerows(res)
    print(f"✅ {len(res)} lignes -> {out} (sans accroche: {miss})")


if __name__ == "__main__":
    main()
