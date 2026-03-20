import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('loadCurrentUser — success → sets currentUser signal', () => {
    const user: User = { id: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B', provider: 'GOOGLE', isSuperAdmin: false };
    service.loadCurrentUser().subscribe(result => {
      expect(result).toEqual(user);
      expect(service.currentUser()).toEqual(user);
    });
    http.expectOne('/api/me').flush(user);
  });

  it('loadCurrentUser — 401 → sets currentUser to null', () => {
    service.loadCurrentUser().subscribe(result => {
      expect(result).toBeNull();
      expect(service.currentUser()).toBeNull();
    });
    http.expectOne('/api/me').flush(null, { status: 401, statusText: 'Unauthorized' });
  });
});
