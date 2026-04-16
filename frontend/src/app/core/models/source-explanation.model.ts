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
  | 'SCROLL_QA'
  | 'SCROLL_F96'
  | 'OPEN_CHAT'
  | 'MISSING_PIECE'
  | 'NONE';

export interface SourceExplanation {
  sourceKey: string;
  sourceType: SourceType;
  label: string;
  sentence: string | null;
  actionType: ActionType;
  actionTarget: string | null;
}
