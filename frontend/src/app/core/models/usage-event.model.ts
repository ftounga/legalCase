export interface UsageEvent {
  id: string;
  eventType: string;
  tokensInput: number;
  tokensOutput: number;
  estimatedCost: number;
  createdAt: string;
}
