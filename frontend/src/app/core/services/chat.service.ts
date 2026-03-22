import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ChatMessageRequest } from '../models/chat-message.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  constructor(private http: HttpClient) {}

  getHistory(caseFileId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`/api/v1/case-files/${caseFileId}/chat`);
  }

  sendMessage(caseFileId: string, request: ChatMessageRequest): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`/api/v1/case-files/${caseFileId}/chat`, request);
  }
}
