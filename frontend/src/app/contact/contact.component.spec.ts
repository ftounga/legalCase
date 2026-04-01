import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ContactComponent } from './contact.component';
import { ContactService } from './contact.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

describe('ContactComponent', () => {
  let fixture: ComponentFixture<ContactComponent>;
  let component: ContactComponent;
  let contactService: jest.Mocked<ContactService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('ContactService', ['send']);

    await TestBed.configureTestingModule({
      imports: [ContactComponent, NoopAnimationsModule, RouterTestingModule],
      providers: [{ provide: ContactService, useValue: spy }]
    }).compileComponents();

    contactService = TestBed.inject(ContactService) as jest.Mocked<ContactService>;
    fixture = TestBed.createComponent(ContactComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('form is invalid when required fields are empty', () => {
    expect(component.form.invalid).toBe(true);
  });

  it('submit button is disabled when form is invalid', () => {
    fixture.detectChanges();
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(btn.disabled).toBe(true);
  });

  it('calls ContactService.send() on valid form submission', () => {
    contactService.send.mockReturnValue(of({ status: 'sent' }));
    component.form.setValue({
      nom: 'Alice', email: 'alice@example.com',
      telephone: '', sujet: 'Test', message: 'Bonjour'
    });
    component.submit();
    expect(contactService.send).toHaveBeenCalled();
  });

  it('shows success state after successful submission', () => {
    contactService.send.mockReturnValue(of({ status: 'sent' }));
    component.form.setValue({
      nom: 'Alice', email: 'alice@example.com',
      telephone: '', sujet: 'Test', message: 'Bonjour'
    });
    component.submit();
    expect(component.sent).toBe(true);
  });

  it('shows snackbar on API error', () => {
    contactService.send.mockReturnValue(throwError(() => new Error('500')));
    component.form.setValue({
      nom: 'Alice', email: 'alice@example.com',
      telephone: '', sujet: 'Test', message: 'Bonjour'
    });
    component.submit();
    expect(component.sent).toBe(false);
    expect(component.sending).toBe(false);
  });
});
