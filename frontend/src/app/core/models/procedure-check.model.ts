export type ProcedureCheckStatus = 'TO_CHECK' | 'VERIFIED' | 'NON_COMPLIANT';

export interface ProcedureCheck {
  id: string;
  ordre: number;
  description: string;
  statut: ProcedureCheckStatus;
  raison?: string | null;
  critereCode?: string | null;
  expectedValue?: string | null;
}
