import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { UnsubscribeComponent } from './unsubscribe.component';
import { EmailSubscriptionService } from '../../core/services/email-subscription.service';

function makeRoute(token: string | null) {
  return {
    snapshot: { queryParamMap: { get: (key: string) => (key === 'token' ? token : null) } }
  };
}

describe('UnsubscribeComponent', () => {
  let fixture: ComponentFixture<UnsubscribeComponent>;
  let component: UnsubscribeComponent;
  let emailSpy: jest.Mocked<EmailSubscriptionService>;

  function setup(token: string | null) {
    emailSpy = {
      getStatus: jest.fn(),
      unsubscribe: jest.fn(),
      resubscribe: jest.fn()
    } as unknown as jest.Mocked<EmailSubscriptionService>;

    TestBed.configureTestingModule({
      imports: [UnsubscribeComponent, NoopAnimationsModule],
      providers: [
        { provide: EmailSubscriptionService, useValue: emailSpy },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: makeRoute(token) }
      ]
    });

    fixture = TestBed.createComponent(UnsubscribeComponent);
    component = fixture.componentInstance;
  }

  // T-1 : token valide non désinscrit → appelle unsubscribe, affiche la confirmation
  it('token valide non désinscrit → appelle unsubscribe et affiche la confirmation', fakeAsync(() => {
    setup('valid-token');
    emailSpy.getStatus.mockReturnValue(of({ optedOut: false }));
    emailSpy.unsubscribe.mockReturnValue(of({ optedOut: true }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(emailSpy.getStatus).toHaveBeenCalledWith('valid-token');
    expect(emailSpy.unsubscribe).toHaveBeenCalledWith('valid-token');
    expect(component.state()).toBe('unsubscribed');
    expect(fixture.nativeElement.textContent).toContain('Vous êtes désinscrit');
    expect(fixture.nativeElement.textContent).toContain('liés à vos dossiers');
  }));

  // T-2 : token déjà désinscrit → affiche l'état + bouton réabonnement, pas d'appel unsubscribe
  it('token déjà désinscrit → affiche l\'état et propose le réabonnement', fakeAsync(() => {
    setup('already-token');
    emailSpy.getStatus.mockReturnValue(of({ optedOut: true }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(emailSpy.getStatus).toHaveBeenCalledWith('already-token');
    expect(emailSpy.unsubscribe).not.toHaveBeenCalled();
    expect(component.state()).toBe('unsubscribed');
    expect(fixture.nativeElement.textContent).toContain('Me réabonner');
  }));

  // T-3 : clic « Me réabonner » → appelle resubscribe, met à jour le message
  it('clic « Me réabonner » → appelle resubscribe et met à jour le message', fakeAsync(() => {
    setup('already-token');
    emailSpy.getStatus.mockReturnValue(of({ optedOut: true }));
    emailSpy.resubscribe.mockReturnValue(of({ optedOut: false }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    component.resubscribe();
    tick();
    fixture.detectChanges();

    expect(emailSpy.resubscribe).toHaveBeenCalledWith('already-token');
    expect(component.state()).toBe('resubscribed');
    expect(fixture.nativeElement.textContent).toContain('Vous êtes réabonné');
  }));

  // T-4 : token absent de l'URL → message d'erreur, aucun appel service
  it('token absent de l\'URL → message d\'erreur, aucun appel service', fakeAsync(() => {
    setup(null);

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.state()).toBe('no-token');
    expect(emailSpy.getStatus).not.toHaveBeenCalled();
    expect(emailSpy.unsubscribe).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('invalide ou incomplet');
  }));

  // T-5 : 404 backend → message d'erreur dédié
  it('404 backend → message d\'erreur dédié', fakeAsync(() => {
    setup('unknown-token');
    emailSpy.getStatus.mockReturnValue(throwError(() => ({ status: 404 })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.state()).toBe('not-found');
    expect(fixture.nativeElement.textContent).toContain('n\'est pas valide');
  }));

  // T-6 : échec réseau → message + bouton « Réessayer »
  it('échec réseau → message d\'erreur et bouton « Réessayer »', fakeAsync(() => {
    setup('valid-token');
    emailSpy.getStatus.mockReturnValue(throwError(() => ({ status: 0 })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.state()).toBe('error');
    expect(fixture.nativeElement.textContent).toContain('Réessayer');

    // Le retry repart de la lecture du statut.
    emailSpy.getStatus.mockReturnValue(of({ optedOut: true }));
    component.retry();
    tick();
    fixture.detectChanges();

    expect(component.state()).toBe('unsubscribed');
  }));
});
