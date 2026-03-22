export interface ChatMessage {
  id: string;
  question: string;
  answer: string | null;
  modelUsed: string | null;
  useEnriched: boolean;
  createdAt: string;
}

export interface ChatMessageRequest {
  question: string;
  useEnriched: boolean;
}
