import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AiQuestion } from '../models/ai-question.model';

@Injectable({ providedIn: 'root' })
export class AiQuestionService {
  constructor(private http: HttpClient) {}

  getQuestions(caseFileId: string): Observable<AiQuestion[]> {
    return this.http.get<AiQuestion[]>(`/api/v1/case-files/${caseFileId}/ai-questions`);
  }

  getQuestionsByAnalysisId(caseFileId: string, analysisId: string): Observable<AiQuestion[]> {
    return this.http.get<AiQuestion[]>(`/api/v1/case-files/${caseFileId}/ai-questions`, {
      params: { analysisId }
    });
  }
}
