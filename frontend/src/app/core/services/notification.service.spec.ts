import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getNotifications appelle GET /api/v1/notifications', () => {
    const mockPage = { content: [], totalElements: 0, totalPages: 0 };
    service.getNotifications().subscribe(res => {
      expect(res.totalElements).toBe(0);
    });
    const req = httpMock.expectOne('/api/v1/notifications?page=0&size=20');
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('getUnreadCount appelle GET /api/v1/notifications/unread-count', () => {
    service.getUnreadCount().subscribe(res => {
      expect(res.count).toBe(3);
    });
    const req = httpMock.expectOne('/api/v1/notifications/unread-count');
    expect(req.request.method).toBe('GET');
    req.flush({ count: 3 });
  });

  it('markAsRead appelle PATCH /api/v1/notifications/{id}/read', () => {
    service.markAsRead('abc-123').subscribe();
    const req = httpMock.expectOne('/api/v1/notifications/abc-123/read');
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('markAllAsRead appelle PATCH /api/v1/notifications/read-all', () => {
    service.markAllAsRead().subscribe();
    const req = httpMock.expectOne('/api/v1/notifications/read-all');
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('refreshUnreadCount met à jour le signal unreadCount', () => {
    service.refreshUnreadCount();
    const req = httpMock.expectOne('/api/v1/notifications/unread-count');
    req.flush({ count: 5 });
    expect(service.unreadCount()).toBe(5);
  });
});
