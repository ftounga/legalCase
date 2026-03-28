import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { PublicShareComponent } from './public-share.component';
import { CaseFileShareService } from '../core/services/case-file-share.service';
import { PublicShareResponse } from '../core/models/share.model';

const MOCK_PUBLIC_SHARE: PublicShareResponse = {
  caseFileId: 'cf-1',
  caseFileTitle: 'Affaire Dupont',
  legalDomain: 'Droit du travail',
  expiresAt: '2026-04-28T00:00:00Z',
  synthesis: {
    id: 'syn-1',
    version: 1,
    analysisType: 'STANDARD',
    status: 'DONE',
    faits: ['Fait 1', 'Fait 2'],
    pointsJuridiques: ['Point 1'],
    risques: ['Risque 1'],
    questionsOuvertes: [],
    timeline: [{ date: '01/01/2026', evenement: 'Licenciement' }],
    modelUsed: null,
    updatedAt: '2026-03-28T00:00:00Z'
  }
};

describe('PublicShareComponent', () => {
  let fixture: ComponentFixture<PublicShareComponent>;
  let component: PublicShareComponent;
  let shareService: jasmine.SpyObj<CaseFileShareService>;

  function setup(response: any): void {
    shareService = jasmine.createSpyObj('CaseFileShareService', ['getPublicShare']);
    shareService.getPublicShare.and.returnValue(response);

    TestBed.configureTestingModule({
      imports: [PublicShareComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'token123' } } } },
        { provide: CaseFileShareService, useValue: shareService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PublicShareComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => TestBed.resetTestingModule());

  // T-06: displays synthesis when token is valid
  it('T-06: should display synthesis on valid token', fakeAsync(() => {
    setup(of(MOCK_PUBLIC_SHARE));
    tick();
    fixture.detectChanges();

    expect(component.share()).toEqual(MOCK_PUBLIC_SHARE);
    expect(component.loading()).toBeFalse();
    expect(component.error()).toBeNull();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.case-title')?.textContent).toContain('Affaire Dupont');
  }));

  // T-07: shows not_found error on 404
  it('T-07: should show not_found error on 404', fakeAsync(() => {
    setup(throwError(() => ({ status: 404 })));
    tick();
    fixture.detectChanges();

    expect(component.error()).toBe('not_found');
    expect(component.loading()).toBeFalse();
  }));

  // T-08: shows expired error on 410
  it('T-08: should show expired error on 410', fakeAsync(() => {
    setup(throwError(() => ({ status: 410 })));
    tick();
    fixture.detectChanges();

    expect(component.error()).toBe('expired');
  }));
});
