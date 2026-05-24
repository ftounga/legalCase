#!/usr/bin/env python3
"""
Génère le CSV bootstrap F-JU-01 à partir de TOOL_REGISTRY frontend.
Format : toolId,brancheCalculId,motCleRecherche,juridictionFiltre,dateMin
Limite : 200 lignes par batch (cf. SF-JU-01-06).
"""
import re
from pathlib import Path

TS_PATH = Path("/home/francky/dev/legalCase/frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts")
OUT_DIR = Path("/home/francky/dev/legalCase/docs/operations/jurisprudence-watch")
OUT_DIR.mkdir(parents=True, exist_ok=True)


def parse_registry():
    """Retourne liste de (tool_id, display_label_clean)."""
    lines = TS_PATH.read_text().splitlines()
    out = []
    for i, line in enumerate(lines):
        m = re.match(r"^\s+\['([A-Za-z0-9_-]+)',\s*\{", line)
        if not m:
            continue
        tid = m.group(1)
        label = None
        for j in range(i + 1, min(i + 6, len(lines))):
            ml = re.search(r"displayLabel:\s*'((?:[^'\\]|\\.)*)'", lines[j])
            if ml:
                raw = ml.group(1)
                label = re.sub(r"\\(.)", r"\1", raw)
                break
        out.append((tid, label or tid))
    return out


def juridiction_country(tid: str, label: str) -> str:
    """FRANCE ou BELGIQUE."""
    lbl_lower = label.lower()
    if "(be)" in lbl_lower or "belgique" in lbl_lower or "belge" in lbl_lower:
        return "BELGIQUE"
    if tid.startswith(("prescription-be", "rcc-be", "c4-onem", "contestation-c4",
                       "at-fedris", "outplacement-be", "refere-tribunal-travail-be",
                       "divorce-dc-be", "divorce-ddi", "tribunal-famille-be",
                       "pacte-successoral-be", "regime-mat-be", "liquidation-partage-be",
                       "autorite-parentale-be", "contribution-alimentaire-enfants-be",
                       "contribution-conjoint-be", "succession-be", "mariage-etranger-be",
                       "contestation-filiation-be", "protection-majeur-be")):
        return "BELGIQUE"
    if tid == "F-DT-06-requete-tribunal-travail":  # BE uniquement (cf. PRODUCT_SPEC)
        return "BELGIQUE"
    if "-be" in tid.lower() and not tid.lower().endswith("ber"):
        return "BELGIQUE"
    return "FRANCE"


def family(tid: str) -> str:
    """travail, famille, immigration, divers."""
    if tid.startswith(("F-DT-", "F-132", "F-136")) or any(s in tid for s in ("onem", "fedris", "rcc-be", "outplacement", "tribunal-travail", "prescription-be-litige-travail")):
        return "travail"
    if tid.startswith(("F-FA-", "F-152", "F-153")) or any(s in tid for s in ("divorce-", "tribunal-famille", "regime-mat", "liquidation-partage", "autorite-parentale", "contribution-", "succession-", "mariage-etranger", "contestation-filiation", "protection-majeur", "pacte-successoral")):
        return "famille"
    if tid.startswith("F-IM-"):
        return "immigration"
    return "divers"


def jurisdiction_filter(fam: str, country: str) -> str:
    """Substring matché contre arret.juridiction() par filterByJuridiction()."""
    if country == "BELGIQUE":
        return ""  # JUDILIBRE FR-only, fallback retourne candidates
    if fam == "travail":
        return "chambre sociale"
    if fam == "famille":
        return "chambre civile"
    if fam == "immigration":
        return ""  # CE + TA + Cass mixte
    return ""


def clean_label(label: str) -> str:
    """Supprime suffixes pays et arrows pour mot-clé propre."""
    s = label
    for tag in ("(FR)", "(BE)", "(FR/BE)", "(Belgique)", "(France)"):
        s = s.replace(tag, "")
    s = s.replace("—", "").replace("–", "").replace("→", "").replace("/", " ").replace(",", " ")
    s = re.sub(r"\s+", " ", s).strip()
    return s


def derive_keywords(tid, label, fam):
    """3-4 mots-clés de recherche jurisprudence."""
    base = clean_label(label)
    parts = [p for p in re.split(r"[-_]", tid) if p and not re.match(r"^[Ff]?\d+$", p) and p not in ("F", "DT", "FA", "IM", "be", "BE", "fr", "FR")]
    id_terms = " ".join(parts).replace("-", " ")

    kws = []
    # L1 : label complet
    kws.append(base)
    # L2 : termes ID nettoyés (souvent plus précis que label)
    if id_terms and id_terms.lower() != base.lower():
        kws.append(id_terms)
    # L3 : variante "label + jurisprudence" pour élargir
    if fam == "travail":
        kws.append(f"{base} Cour de cassation chambre sociale")
    elif fam == "famille":
        kws.append(f"{base} Cour de cassation chambre civile")
    elif fam == "immigration":
        kws.append(f"{base} Conseil d'État")
    else:
        kws.append(f"{base} jurisprudence")
    # Dedup & trim
    seen = set()
    out = []
    for k in kws:
        k = k.strip()
        if not k or len(k) > 500:
            continue
        kl = k.lower()
        if kl in seen:
            continue
        seen.add(kl)
        out.append(k)
    return out


def main():
    entries = parse_registry()
    print(f"{len(entries)} outils dans TOOL_REGISTRY")

    rows = []  # (toolId, brancheCalculId, motCleRecherche, juridictionFiltre, dateMin)
    fr_kept, be_kept = 0, 0
    for tid, label in entries:
        country = juridiction_country(tid, label)
        fam = family(tid)
        juri = jurisdiction_filter(fam, country)
        if country == "FRANCE":
            kws = derive_keywords(tid, label, fam)  # 3 lignes
            fr_kept += 1
        else:
            # BE : 1 ligne (JUDILIBRE ne couvre pas BE, seed pour quand backend BE arrivera)
            kws = derive_keywords(tid, label, fam)[:1]
            be_kept += 1
        for kw in kws:
            rows.append((tid, "default", kw, juri, ""))

    print(f"FR conservés: {fr_kept} outils, BE: {be_kept}")
    print(f"Total lignes CSV: {len(rows)}")

    # Découpe en batches de 200
    BATCH_SIZE = 200
    batches = [rows[i:i + BATCH_SIZE] for i in range(0, len(rows), BATCH_SIZE)]
    print(f"Batches: {len(batches)}")

    for idx, batch in enumerate(batches, start=1):
        path = OUT_DIR / f"bootstrap-batch-{idx}.csv"
        with path.open("w") as f:
            for r in batch:
                # CSV simple : toutes les valeurs ASCII-safe puisque pas de virgule injectée
                # (vérifier qu'aucun mot-clé ne contient une virgule, sinon escape)
                escaped = []
                for v in r:
                    if "," in v or '"' in v:
                        escaped.append('"' + v.replace('"', '""') + '"')
                    else:
                        escaped.append(v)
                f.write(",".join(escaped) + "\n")
        print(f"  → {path.name} ({len(batch)} lignes)")


if __name__ == "__main__":
    main()
