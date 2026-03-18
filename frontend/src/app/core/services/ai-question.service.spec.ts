import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AiQuestionService } from './ai-question.service';
import { AiQuestion } from '../models/ai-question.model';

const mockQuestions: AiQuestion[] = [
  { orderIndex: 0, questionText: 'Question A ?' },
  { orderIndex: 1, questionText: 'Question B ?' }
];

describe('AiQuestionService', () => {
  let service: AiQuestionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AiQuestionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('getQuestions — GET /api/v1/case-files/cf1/ai-questions', () => {
    service.getQuestions('cf1').subscribe(qs => {
      expect(qs.length).toBe(2);
      expect(qs[0].questionText).toBe('Question A ?');
      expect(qs[0].orderIndex).toBe(0);
    });
    http.expectOne('/api/v1/case-files/cf1/ai-questions').flush(mockQuestions);
  });

  it('getQuestions — liste vide → retourne []', () => {
    service.getQuestions('cf1').subscribe(qs => expect(qs).toEqual([]));
    http.expectOne('/api/v1/case-files/cf1/ai-questions').flush([]);
  });
});
