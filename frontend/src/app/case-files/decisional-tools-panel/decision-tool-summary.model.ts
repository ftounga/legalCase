/**
 * F-177 SF-177-01 — Modèle de verdict synthétique exposé par chaque outil décisionnel.
 *
 * Chaque composant `*-section` du panel F-IA-04 produit un `DecisionToolSummary`
 * (ou `null` si l'outil n'a pas de verdict — générateurs de document, checklists pures).
 * Le composant `<app-decision-tool-card>` formate ce summary pour l'affichage card.
 */

export type MetierAlertLevel = 'OK' | 'WARNING' | 'ALERT';

export interface DecisionToolSummary {
  /** Libellé court à gauche du verdict (ex : "Verdict", "Délai", "Indemnité"). */
  label: string;
  /** Valeur principale affichée (ex : "65 %", "3 200 €", "J-7"). Vide → fallback "Cliquer pour utiliser". */
  primaryValue: string;
  /** Précision optionnelle (ex : "MOYEN", "(brut)"). */
  secondaryValue?: string;
  /** Niveau d'alerte métier dérivé du résultat — colore la bordure gauche de la card. */
  alertLevel?: MetierAlertLevel;
}

/**
 * Formate un `DecisionToolSummary` pour l'affichage card.
 * Retourne `null` si le summary est vide ou si la `primaryValue` est vide → consommateur affiche le fallback.
 */
export function formatSummary(summary: DecisionToolSummary | null): string | null {
  if (!summary || !summary.primaryValue) {
    return null;
  }
  const main = `${summary.label} : ${summary.primaryValue}`;
  return summary.secondaryValue ? `${main} ${summary.secondaryValue}` : main;
}
