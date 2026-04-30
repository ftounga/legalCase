import { TestBed } from '@angular/core/testing';
import { QuotaErrorStateService, QuotaError } from './quota-error-state.service';

describe('QuotaErrorStateService', () => {
  let service: QuotaErrorStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(QuotaErrorStateService);
  });

  function buildError(code: string | null, message = 'Limite atteinte'): QuotaError {
    return { code, message, sourceUrl: '/api/v1/test', receivedAt: Date.now() };
  }

  it('démarre avec un état null', () => {
    expect(service.error()).toBeNull();
  });

  it('set() pousse une erreur dans le signal', () => {
    const err = buildError('TOKEN_BUDGET_EXCEEDED', 'Budget tokens dépassé');
    service.set(err);
    expect(service.error()).toEqual(err);
  });

  it('clear() vide l\'état', () => {
    service.set(buildError('TOKEN_BUDGET_EXCEEDED'));
    service.clear();
    expect(service.error()).toBeNull();
  });

  it('set() écrase l\'erreur précédente (pas d\'empilement)', () => {
    service.set(buildError('TOKEN_BUDGET_EXCEEDED'));
    service.set(buildError('CHAT_MESSAGE_LIMIT_EXCEEDED'));
    expect(service.error()?.code).toBe('CHAT_MESSAGE_LIMIT_EXCEEDED');
  });

  it('matches() retourne true quand le code courant correspond', () => {
    const isToken = service.matches('TOKEN_BUDGET_EXCEEDED');
    expect(isToken()).toBe(false);

    service.set(buildError('TOKEN_BUDGET_EXCEEDED'));
    expect(isToken()).toBe(true);

    service.set(buildError('CHAT_MESSAGE_LIMIT_EXCEEDED'));
    expect(isToken()).toBe(false);
  });

  it('matches() retourne false si code=null (legacy)', () => {
    service.set(buildError(null, 'Limite atteinte'));
    expect(service.matches('TOKEN_BUDGET_EXCEEDED')()).toBe(false);
  });
});
