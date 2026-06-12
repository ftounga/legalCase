import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PiecesWaveCardComponent } from './pieces-wave-card.component';
import { PiecesWave } from '../../core/models/pieces-wave.model';

describe('PiecesWaveCardComponent', () => {
  let fixture: ComponentFixture<PiecesWaveCardComponent>;
  let component: PiecesWaveCardComponent;
  let httpMock: HttpTestingController;

  const CASE_ID = 'case-1';
  const URL = `/api/v1/case-files/${CASE_ID}/pieces-wave`;

  function piece(id: string, name: string): PiecesWave['pendingPieces'][number] {
    return { documentId: id, filename: name, createdAt: '2026-06-10T10:00:00Z' };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PiecesWaveCardComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(PiecesWaveCardComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_ID;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('n’affiche aucune carte quand pendingCount = 0', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({ analyzedAt: null, pendingCount: 0, pendingPieces: [] } as PiecesWave);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="pieces-wave-card"]')).toBeNull();
  });

  it('affiche la carte avec compteur et liste quand pendingCount > 0', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({
      analyzedAt: '2026-06-01T08:00:00Z',
      pendingCount: 2,
      pendingPieces: [piece('d1', 'piece-a.pdf'), piece('d2', 'piece-b.pdf')],
    } as PiecesWave);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="pieces-wave-card"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="wave-count"]')!.textContent).toContain('2');
    expect(el.querySelectorAll('.wave-item').length).toBe(2);
  });

  it('limite l’affichage à 5 pièces et indique le surplus', () => {
    const pieces = Array.from({ length: 7 }, (_, i) => piece('d' + i, 'piece-' + i + '.pdf'));
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({
      analyzedAt: '2026-06-01T08:00:00Z', pendingCount: 7, pendingPieces: pieces,
    } as PiecesWave);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.wave-item').length).toBe(5);
    expect(el.querySelector('.wave-more')!.textContent).toContain('2 autre');
  });

  it('le CTA « Relancer l’analyse » émet analyzeRequested', () => {
    fixture.detectChanges();
    httpMock.expectOne(URL).flush({
      analyzedAt: '2026-06-01T08:00:00Z', pendingCount: 1, pendingPieces: [piece('d1', 'p.pdf')],
    } as PiecesWave);
    fixture.detectChanges();

    const spy = jest.fn();
    component.analyzeRequested.subscribe(spy);
    const btn = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-testid="wave-analyze-btn"]') as HTMLButtonElement;
    expect(btn).toBeTruthy();
    btn.click();
    expect(spy).toHaveBeenCalled();
  });
});
