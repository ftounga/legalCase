export type SourceType =
  | 'DOCUMENT'
  | 'QUESTION_AI'
  | 'CHECKLIST_F96'
  | 'CHAT'
  | 'MISSING_PIECE'
  | 'ANALYSIS_DETECTION'
  | 'MULTI';

export type ActionType =
  | 'OPEN_DOCUMENT'
  | 'OPEN_DOCUMENTS_LIST'
  | 'SCROLL_QA'
  | 'OPEN_QUESTIONS'
  | 'SCROLL_F96'
  | 'OPEN_F96_LIST'
  | 'OPEN_CHAT'
  | 'OPEN_MISSING_PIECES'
  | 'NONE';

export interface SourceExplanation {
  sourceKey: string;
  sourceType: SourceType;
  label: string;
  sentence: string | null;
  secondaryText: string | null;
  actionType: ActionType;
  actionTarget: string | null;
}
