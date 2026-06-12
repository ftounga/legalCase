import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CaseIntakeSectionComponent } from './case-intake-section.component';
import { CaseIntake } from '../../core/models/case-intake.model';

describe('CaseIntakeSectionComponent', () => {
  let fixture: ComponentFixture<CaseIntakeSectionComponent>;
  let component: CaseIntakeSectionComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/intake`;

  function empty(): CaseIntake {
    return {
      disputeType: null, admissibility: null, prescriptionStatus: null,
      prescriptionDeadline: null, jurisdiction: null, estimatedValueCents: null,
      notes: null, qualifiedAt: null, qualified: false,
    };
  }

  function qualified(): CaseIntake {
    return {
      disputeType: 'Contestation de licenciement',
      admissibility: 'RECEVABLE',
      prescriptionStatus: 'DANS_LES_DELAIS',
      prescriptionDeadline: '2026-12-31',
      jurisdiction: 'Conseil de prud’hommes',
      estimatedValueCents: 2500000,
      notes: 'Dossier solide',
      qualifiedAt: '2026-06-12T10:00:00Z',
      qualified: true,
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CaseIntakeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(CaseIntakeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('dossier non qualifié : état vide avec invite + CTA', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(empty());
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="intake-empty"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="intake-qualify-btn"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="intake-verdict"]')).toBeNull();
  });

  it('dossier qualifié : verdict avec badge + 4 pastilles + valeur formatée', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(qualified());
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="intake-verdict"]')).toBeTruthy();
    expect(el.querySelector('.intake-badge--ok')).toBeTruthy();
    expect(el.querySelector('[data-testid="intake-pill-type"]')!.textContent)
      .toContain('Contestation de licenciement');
    expect(el.querySelector('[data-testid="intake-pill-prescription"]')!.textContent)
      .toContain('Dans les délais');
    expect(el.querySelector('[data-testid="intake-pill-jurisdiction"]')).toBeTruthy();
    // valeur formatée en euros (25 000 €)
    const valueTxt = el.querySelector('[data-testid="intake-pill-value"]')!.textContent ?? '';
    expect(valueTxt.replace(/ | /g, ' ')).toContain('25 000');
  });

  it('qualifie via le formulaire (PUT) puis bascule en verdict', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(empty());
    fixture.detectChanges();

    component.startEdit();
    component.form.patchValue({
      disputeType: 'Rappel de salaire',
      admissibility: 'RECEVABLE',
      estimatedValueEuros: 12000,
    });
    component.save();

    const put = httpMock.expectOne((r) => r.method === 'PUT' && r.url === URL);
    expect(put.request.body.disputeType).toBe('Rappel de salaire');
    // EUR → centimes
    expect(put.request.body.estimatedValueCents).toBe(1200000);
    put.flush({ ...empty(), disputeType: 'Rappel de salaire', admissibility: 'RECEVABLE',
      estimatedValueCents: 1200000, qualified: true, qualifiedAt: '2026-06-12T10:00:00Z' });
    fixture.detectChanges();

    expect(component.editing()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="intake-verdict"]'))
      .toBeTruthy();
  });

  it('valeur négative : formulaire invalide, pas de requête PUT', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush(empty());

    component.startEdit();
    component.form.patchValue({ estimatedValueEuros: -5 });
    component.save();

    expect(component.form.invalid).toBe(true);
    httpMock.expectNone((r) => r.method === 'PUT');
  });
});
