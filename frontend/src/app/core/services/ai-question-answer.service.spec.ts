import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AiQuestionAnswerService } from './ai-question-answer.service';

describe('AiQuestionAnswerService', () => {
  let service: AiQuestionAnswerService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AiQuestionAnswerService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('submitAnswer — POST /api/v1/ai-questions/{id}/answer avec le bon body', () => {
    service.submitAnswer('q1', 'Ma réponse').subscribe();
    const req = http.expectOne('/api/v1/ai-questions/q1/answer');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ answerText: 'Ma réponse' });
    req.flush(null);
  });
});
