import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AceLandingComponent } from './ace-landing.component';
import { ContactService, ContactPayload } from '../contact/contact.service';

describe('AceLandingComponent', () => {
  let fixture: ComponentFixture<AceLandingComponent>;
  let component: AceLandingComponent;
  let contactSpy: jasmine.SpyObj<ContactService>;

  beforeEach(async () => {
    contactSpy = jasmine.createSpyObj<ContactService>('ContactService', ['send']);
    await TestBed.configureTestingModule({
      imports: [AceLandingComponent],
      providers: [{ provide: ContactService, useValue: contactSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AceLandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('se crée', () => {
    expect(component).toBeTruthy();
  });

  it('expose les 5 piliers produit', () => {
    expect(component.valueItems.length).toBe(5);
  });

  it('formulaire invalide → submit() n\'appelle pas le service', () => {
    component.submit();
    expect(contactSpy.send).not.toHaveBeenCalled();
  });

  it('formulaire valide → appelle send() avec le sujet ACE et le téléphone omis si vide', () => {
    contactSpy.send.and.returnValue(of({ status: 'sent' }));
    component.form.setValue({
      nom: 'Me Dupont',
      email: 'dupont@cabinet.fr',
      cabinet: 'Barreau de Paris',
      telephone: '',
      message: 'Bonjour, je souhaite activer mon offre.',
    });

    component.submit();

    expect(contactSpy.send).toHaveBeenCalledTimes(1);
    const payload = contactSpy.send.calls.mostRecent().args[0] as ContactPayload;
    expect(payload.sujet).toBe('Partenariat ACE 2026');
    expect(payload.telephone).toBeUndefined();
    expect(payload.message).toContain('Cabinet / Barreau : Barreau de Paris');
    expect(component.sent()).toBe(true);
    expect(component.sending()).toBe(false);
  });

  it('succès → bascule l\'état sent', () => {
    contactSpy.send.and.returnValue(of({ status: 'sent' }));
    component.form.patchValue({
      nom: 'Me Martin',
      email: 'martin@cabinet.fr',
      message: 'Demande de démo.',
    });
    component.submit();
    expect(component.sent()).toBe(true);
    expect(component.errorMsg()).toBeNull();
  });

  it('erreur réseau → affiche un message d\'erreur et ne reste pas en envoi', () => {
    contactSpy.send.and.returnValue(throwError(() => new Error('500')));
    component.form.patchValue({
      nom: 'Me Martin',
      email: 'martin@cabinet.fr',
      message: 'Demande de démo.',
    });
    component.submit();
    expect(component.sent()).toBe(false);
    expect(component.sending()).toBe(false);
    expect(component.errorMsg()).toContain('échoué');
  });
});
