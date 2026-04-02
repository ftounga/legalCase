import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { HelpChatWidgetComponent, SUGGESTED_QUESTIONS } from './help-chat-widget.component';
import { HelpService } from '../core/services/help.service';
import { AuthService } from '../core/services/auth.service';

describe('HelpChatWidgetComponent', () => {
  let component: HelpChatWidgetComponent;
  let fixture: ComponentFixture<HelpChatWidgetComponent>;
  let helpService: jest.Mocked<HelpService>;

  function setup(isAuthenticated: boolean) {
    helpService = jasmine.createSpyObj('HelpService', ['chat']);

    const currentUser = signal<any>(isAuthenticated ? { id: 'u-1', email: 'test@example.com' } : null);
    const authService = { currentUser };

    TestBed.configureTestingModule({
      imports: [HelpChatWidgetComponent, NoopAnimationsModule],
      providers: [
        { provide: HelpService, useValue: helpService },
        { provide: AuthService, useValue: authService },
      ],
    });

    fixture = TestBed.createComponent(HelpChatWidgetComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  // T-01 : bulle non visible quand currentUser est null
  it('T-01: hides widget when user is not authenticated', () => {
    setup(false);
    const widget = fixture.nativeElement.querySelector('.help-widget');
    expect(widget).toBeNull();
  });

  // T-02 : bulle visible quand currentUser est non null
  it('T-02: shows widget when user is authenticated', () => {
    setup(true);
    const bubble = fixture.nativeElement.querySelector('.help-bubble');
    expect(bubble).not.toBeNull();
  });

  // T-03 : clic sur bulle → panelOpen passe à true
  it('T-03: clicking bubble opens the panel', () => {
    setup(true);
    expect(component.panelOpen()).toBe(false);
    component.togglePanel();
    expect(component.panelOpen()).toBe(true);
  });

  // T-04 : clic sur fermer → panelOpen passe à false
  it('T-04: closePanel() closes the panel', () => {
    setup(true);
    component.panelOpen.set(true);
    component.closePanel();
    expect(component.panelOpen()).toBe(false);
  });

  // T-05 : bouton Envoyer désactivé si message vide
  it('T-05: canSend is false when message is empty', () => {
    setup(true);
    component.message = '';
    expect(component.canSend).toBe(false);
  });

  // T-06 : bouton Envoyer désactivé si message > 500 chars
  it('T-06: canSend is false when message exceeds 500 chars', () => {
    setup(true);
    component.message = 'a'.repeat(501);
    expect(component.canSend).toBe(false);
  });

  // T-07 : clic question suggérée → appel chat() avec la question
  it('T-07: sendSuggestion() calls helpService.chat with the question', fakeAsync(() => {
    setup(true);
    helpService.chat.mockReturnValue(of({ answer: 'Réponse IA' }));

    component.sendSuggestion(SUGGESTED_QUESTIONS[0]);
    tick();

    expect(helpService.chat).toHaveBeenCalledWith(SUGGESTED_QUESTIONS[0]);
  }));

  // T-08 : pendant chargement → isLoading true, canSend false
  it('T-08: isLoading is true while request is pending', () => {
    setup(true);
    helpService.chat.mockReturnValue(of({ answer: 'ok' }));
    component.message = 'Une question ?';

    let loadingDuringCall = false;
    helpService.chat.mockImplementation(() => {
      loadingDuringCall = component.isLoading();
      return of({ answer: 'ok' });
    });

    component.send();
    expect(loadingDuringCall).toBe(true);
  });

  // T-09 : succès → answer affiché dans le panneau
  it('T-09: successful response sets answer signal', fakeAsync(() => {
    setup(true);
    helpService.chat.mockReturnValue(of({ answer: 'Pour créer un dossier, cliquez sur Nouveau dossier.' }));

    component.message = 'Comment créer un dossier ?';
    component.send();
    tick();

    expect(component.answer()).toBe('Pour créer un dossier, cliquez sur Nouveau dossier.');
    expect(component.isLoading()).toBe(false);
  }));

  // T-10 : erreur service → message d'erreur inline affiché
  it('T-10: service error sets errorMessage signal', fakeAsync(() => {
    setup(true);
    helpService.chat.mockReturnValue(throwError(() => new Error('503')));

    component.message = 'Comment créer un dossier ?';
    component.send();
    tick();

    expect(component.errorMessage()).toBeTruthy();
    expect(component.answer()).toBeNull();
    expect(component.isLoading()).toBe(false);
  }));
});
