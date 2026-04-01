import { TestBed } from '@angular/core/testing';
import { LoadingService } from './loading.service';

describe('LoadingService', () => {
  let service: LoadingService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LoadingService);
  });

  it('isLoading false par défaut', () => {
    expect(service.isLoading()).toBe(false);
  });

  it('increment → isLoading true', () => {
    service.increment();
    expect(service.isLoading()).toBe(true);
  });

  it('increment + decrement → isLoading false', () => {
    service.increment();
    service.decrement();
    expect(service.isLoading()).toBe(false);
  });

  it('decrement sous zéro → reste à 0 (isLoading false)', () => {
    service.decrement();
    expect(service.isLoading()).toBe(false);
  });
});
