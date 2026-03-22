import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ChatService } from './chat.service';
import { ChatMessage } from '../models/chat-message.model';

describe('ChatService', () => {
  let service: ChatService;
  let http: HttpTestingController;

  const CASE_FILE_ID = 'cf-123';

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(ChatService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getHistory — GET /api/v1/case-files/:id/chat', () => {
    const messages: ChatMessage[] = [
      { id: 'm1', question: 'Q?', answer: 'R.', modelUsed: 'haiku', useEnriched: false, createdAt: '2026-03-22T10:00:00Z' }
    ];
    service.getHistory(CASE_FILE_ID).subscribe(result => expect(result).toEqual(messages));
    const req = http.expectOne(`/api/v1/case-files/${CASE_FILE_ID}/chat`);
    expect(req.request.method).toBe('GET');
    req.flush(messages);
  });

  it('sendMessage — POST /api/v1/case-files/:id/chat', () => {
    const request = { question: 'Qui est le salarié ?', useEnriched: false };
    const response: ChatMessage = { id: 'm2', question: request.question, answer: 'M. Dupont.', modelUsed: 'haiku', useEnriched: false, createdAt: '2026-03-22T10:01:00Z' };
    service.sendMessage(CASE_FILE_ID, request).subscribe(result => expect(result).toEqual(response));
    const req = http.expectOne(`/api/v1/case-files/${CASE_FILE_ID}/chat`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(response);
  });
});
