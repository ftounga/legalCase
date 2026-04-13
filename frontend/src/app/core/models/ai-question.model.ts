export interface AiQuestion {
  id: string;
  orderIndex: number;
  questionText: string;
  answerText: string | null;
  critereCode?: string | null;
  expectedValue?: string | null;
}
