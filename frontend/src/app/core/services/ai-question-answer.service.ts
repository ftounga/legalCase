import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AiQuestionAnswerService {
  constructor(private http: HttpClient) {}

  submitAnswer(questionId: string, answerText: string): Observable<void> {
    return this.http.post<void>(`/api/v1/ai-questions/${questionId}/answer`, { answerText });
  }
}
