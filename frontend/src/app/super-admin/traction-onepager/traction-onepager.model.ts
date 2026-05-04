/**
 * F-187 SF-187-02 — Modèle TypeScript miroir du DTO backend `TractionOnePagerInput`.
 *
 * Le contrat est figé dans la mini-spec SF-187-01-backend.md.
 * Toute modification doit être synchronisée avec le backend.
 */

/** Map booléenne des 7 KPIs F-76 — quels KPIs inclure dans le PDF. */
export interface IncludedKpis {
  totalWorkspaces: boolean;
  trialWorkspaces: boolean;
  paidWorkspaces: boolean;
  conversionRatePct: boolean;
  activeWorkspaces30d: boolean;
  analysesLast7Days: boolean;
  analysesLast30Days: boolean;
}

/** Verbatim client (témoignage). */
export interface Verbatim {
  quote: string;
  author: string;
}

/** Contact à afficher dans le footer du PDF. */
export interface Contact {
  name: string;
  email: string;
  url: string;
}

/** Body envoyé à `POST /api/v1/super-admin/traction-onepager/pdf`. */
export interface TractionOnePagerInput {
  includeKpis: IncludedKpis;
  verbatims: Verbatim[];   // 0-2
  partners: string[];      // 0-10
  headline: string;        // ≤ 200, requis
  contact: Contact;
}
