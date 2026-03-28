import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ShareDialogComponent, ShareDialogData } from './share-dialog.component';
import { CaseFileShareService } from '../../core/services/case-file-share.service';
import { ShareResponse } from '../../core/models/share.model';

const MOCK_SHARE: ShareResponse = {
  id: 'share-1',
  shareUrl: 'https://app.example.com/share/token123',
  expiresAt: '2026-04-28T00:00:00Z',
  createdAt: '2026-03-28T00:00:00Z'
};

const DIALOG_DATA: ShareDialogData = { caseFileId: 'cf-1', caseFileTitle: 'Dossier Test' };

describe('ShareDialogComponent', () => {
  let fixture: ComponentFixture<ShareDialogComponent>;
  let component: ShareDialogComponent;
  let shareService: jasmine.SpyObj<CaseFileShareService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    shareService = jasmine.createSpyObj('CaseFileShareService', ['listShares', 'createShare', 'revokeShare']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    shareService.listShares.and.returnValue(of([MOCK_SHARE]));

    await TestBed.configureTestingModule({
      imports: [ShareDialogComponent],
      providers: [
        provideAnimationsAsync(),
        { provide: MAT_DIALOG_DATA, useValue: DIALOG_DATA },
        { provide: CaseFileShareService, useValue: shareService },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ShareDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // T-01: active shares loaded on init
  it('T-01: should load active shares on init', () => {
    expect(shareService.listShares).toHaveBeenCalledWith('cf-1');
    expect(component.activeShares().length).toBe(1);
    expect(component.loadingShares()).toBeFalse();
  });

  // T-02: generateLink sets generatedLink and prepends to activeShares
  it('T-02: should generate link and prepend to active shares', fakeAsync(() => {
    const newShare: ShareResponse = { ...MOCK_SHARE, id: 'share-2', shareUrl: 'https://app.example.com/share/new' };
    shareService.createShare.and.returnValue(of(newShare));

    component.generateLink();
    tick();

    expect(shareService.createShare).toHaveBeenCalledWith('cf-1', 7);
    expect(component.generatedLink()?.id).toBe('share-2');
    expect(component.activeShares()[0].id).toBe('share-2');
    expect(component.generating()).toBeFalse();
  }));

  // T-03: generateLink error shows snackbar
  it('T-03: should show error snackbar when generateLink fails', fakeAsync(() => {
    shareService.createShare.and.returnValue(throwError(() => new Error('server error')));

    component.generateLink();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du lien.', 'Fermer', jasmine.objectContaining({ duration: 4000 })
    );
    expect(component.generating()).toBeFalse();
  }));

  // T-04: revokeShare removes share from list
  it('T-04: should remove revoked share from activeShares', fakeAsync(() => {
    shareService.revokeShare.and.returnValue(of(void 0));

    component.revokeShare(MOCK_SHARE);
    tick();

    expect(shareService.revokeShare).toHaveBeenCalledWith('cf-1', 'share-1');
    expect(component.activeShares().find(s => s.id === 'share-1')).toBeUndefined();
    expect(component.revokingId()).toBeNull();
  }));

  // T-05: revokeShare also clears generatedLink if it's the same share
  it('T-05: should clear generatedLink when the generated share is revoked', fakeAsync(() => {
    shareService.createShare.and.returnValue(of(MOCK_SHARE));
    shareService.revokeShare.and.returnValue(of(void 0));

    component.generateLink();
    tick();
    expect(component.generatedLink()?.id).toBe('share-1');

    component.revokeShare(MOCK_SHARE);
    tick();
    expect(component.generatedLink()).toBeNull();
  }));
});
