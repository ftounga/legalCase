import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';

/**
 * SF-208-08 / SF-246-04 — Helper partagé pour {@link VictimeViolencesL4256SectionComponent}
 * (F-IM-24-victime-violences-l4256-fr) — FR mono-pays.
 *
 * Pré-fill IA (SF-246-04) :
 *  - dateOrdonnanceProtection ← `aiData.dateOrdonnanceProtectionJaf` : date de
 *    l'ordonnance de protection rendue par le JAF (Cciv 515-9), extraite par le
 *    pipeline IA Immigration FR. Validée via `ISO_DATE_RE` avant tout pré-fill ;
 *    une valeur non ISO (format ambigu renvoyé par le LLM) est rejetée.
 *
 * Un seul champ pré-rempli : `getPrefillCount` renvoie 1 si une date ISO valide
 * est détectée et le workspace est FR, 0 sinon. Les 5 autres champs du formulaire
 * (juridiction, dureeProtectionMois, dateExpirationProtection, enfantsAcharge,
 * nationalite) restent en saisie manuelle — non factualisables de façon fiable
 * par le LLM en V1 (cf. mini-spec SF-246-04 §Périmètre).
 */

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function isFrance(input: PrefillCountInput): boolean {
  return (input.workspaceCountry ?? 'FRANCE') === 'FRANCE';
}

export const VictimeViolencesL4256PrefillRules = {
  ISO_DATE_RE,

  /**
   * Date de l'ordonnance de protection JAF — lue depuis
   * `aiData.dateOrdonnanceProtectionJaf` (champ extrait par le pipeline IA
   * Immigration FR, SF-246-04). Retourne la date si elle est au format ISO
   * `YYYY-MM-DD` strict, `null` sinon (aiData absent, champ absent, ou format
   * non ISO renvoyé par le LLM).
   */
  computeDateOrdonnanceProtection(input: PrefillCountInput): string | null {
    const raw = input.aiData?.dateOrdonnanceProtectionJaf;
    if (typeof raw !== 'string' || !ISO_DATE_RE.test(raw)) return null;
    return raw;
  },

  computePrefillCount(input: PrefillCountInput): number {
    if (!isFrance(input)) return 0;
    let n = 0;
    if (this.computeDateOrdonnanceProtection(input) !== null) n++;
    return n;
  },
} as const;
