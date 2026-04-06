import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InAppNotification {
  id: string;
  type: string;
  title: string;
  message: string | null;
  link: string | null;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationPage {
  content: InAppNotification[];
  totalElements: number;
  totalPages: number;
}

export interface UnreadCountResponse {
  count: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  readonly unreadCount = signal(0);

  constructor(private http: HttpClient) {}

  getNotifications(page = 0, size = 20): Observable<NotificationPage> {
    return this.http.get<NotificationPage>(`/api/v1/notifications?page=${page}&size=${size}`);
  }

  getUnreadCount(): Observable<UnreadCountResponse> {
    return this.http.get<UnreadCountResponse>('/api/v1/notifications/unread-count');
  }

  markAsRead(id: string): Observable<void> {
    return this.http.patch<void>(`/api/v1/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>('/api/v1/notifications/read-all', {});
  }

  refreshUnreadCount(): void {
    this.getUnreadCount().subscribe({
      next: res => this.unreadCount.set(res.count),
      error: () => {}
    });
  }
}
