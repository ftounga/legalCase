import { TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceMembersComponent } from './workspace-members.component';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceInvitationService } from '../../core/services/workspace-invitation.service';
import { BillingService } from '../../core/services/billing.service';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMember } from '../../core/models/workspace-member.model';
import { WorkspaceInvitation } from '../../core/models/workspace-invitation.model';
import { SeatsSummary } from '../../core/models/seats-summary.model';

const mockMember: WorkspaceMember = {
  userId: 'u1', email: 'alice@test.com', firstName: 'Alice',
  lastName: 'Martin', memberRole: 'LAWYER', createdAt: '2026-03-01T10:00:00Z'
};

const mockInvitation: WorkspaceInvitation = {
  id: 'inv1', email: 'bob@test.com', role: 'LAWYER',
  status: 'PENDING', expiresAt: '2026-03-25T10:00:00Z', createdAt: '2026-03-18T10:00:00Z'
};

const mockSeatsTeam4: SeatsSummary = {
  planCode: 'TEAM', seatCount: 4, includedSeats: 3, maxSeats: 6,
  extraSeatPriceCents: 5900, baseMonthlyCostCents: 21900, totalMonthlyCostCents: 27800
};

describe('WorkspaceMembersComponent', () => {
  let component: WorkspaceMembersComponent;
  let memberService: jest.Mocked<WorkspaceMemberService>;
  let invitationService: jest.Mocked<WorkspaceInvitationService>;
  let billingService: jest.Mocked<BillingService>;
  let snackBar: jest.Mocked<MatSnackBar>;
  let dialog: jest.Mocked<MatDialog>;

  const setupWith = async (seats: SeatsSummary | null) => {
    TestBed.resetTestingModule();
    memberService = jasmine.createSpyObj('WorkspaceMemberService', ['getMembers', 'removeMember']);
    invitationService = jasmine.createSpyObj('WorkspaceInvitationService', ['getInvitations', 'createInvitation', 'revokeInvitation']);
    billingService = jasmine.createSpyObj('BillingService', ['getSeatsSummary']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj('MatDialog', ['open']);

    memberService.getMembers.mockReturnValue(of([mockMember]));
    invitationService.getInvitations.mockReturnValue(of([mockInvitation]));
    billingService.getSeatsSummary.mockReturnValue(seats ? of(seats) : throwError(() => new Error('403')));

    await TestBed.configureTestingModule({
      imports: [WorkspaceMembersComponent, NoopAnimationsModule],
      providers: [
        { provide: WorkspaceMemberService, useValue: memberService },
        { provide: WorkspaceInvitationService, useValue: invitationService },
        { provide: BillingService, useValue: billingService },
        { provide: AuthService, useValue: { currentUser: () => ({ email: 'alice@test.com' }) } },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(WorkspaceMembersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => { await setupWith(mockSeatsTeam4); });

  it('should be created', () => expect(component).toBeTruthy());

  it('loadAll — charge membres et invitations', () => {
    expect(component.members.length).toBe(1);
    expect(component.invitations.length).toBe(1);
  });

  it('SF-123-03 — charge le résumé seats au init', () => {
    expect(component.seatsSummary()).toEqual(mockSeatsTeam4);
  });

  it('SF-123-03 — seatsSummary null si endpoint 403', async () => {
    await setupWith(null);
    expect(component.seatsSummary()).toBeNull();
  });

  it('SF-123-03 — openInviteDialog ouvre MatDialog avec summary en data', () => {
    const dialogRefStub = { afterClosed: () => of(undefined) } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);
    component.openInviteDialog();
    expect(dialog.open).toHaveBeenCalled();
    const [, config] = dialog.open.mock.calls[0];
    expect((config as { data: { summary: SeatsSummary } }).data.summary).toEqual(mockSeatsTeam4);
  });

  it('SF-123-03 — openInviteDialog confirmé → appelle createInvitation + recharge seats', () => {
    const result = { email: 'bob@test.com', role: 'LAWYER' };
    const dialogRefStub = { afterClosed: () => of(result) } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);
    invitationService.createInvitation.mockReturnValue(of(mockInvitation));

    component.openInviteDialog();

    expect(invitationService.createInvitation).toHaveBeenCalledWith('bob@test.com', 'LAWYER');
    // loadSeatsSummary appelé à l'init + après acceptation = 2
    expect(billingService.getSeatsSummary).toHaveBeenCalledTimes(2);
  });

  it('SF-123-03 — createInvitation 402 → snackbar avec message backend', () => {
    const result = { email: 'bob@test.com', role: 'LAWYER' };
    const dialogRefStub = { afterClosed: () => of(result) } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);
    invitationService.createInvitation.mockReturnValue(throwError(() => ({
      status: 402, error: { message: 'Passez à PRO pour en ajouter davantage.' }
    })));

    component.openInviteDialog();

    expect(snackBar.open).toHaveBeenCalledWith(
      'Passez à PRO pour en ajouter davantage.', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('SF-123-03 — removeMember ouvre confirm dialog, confirme → appelle removeMember', () => {
    const dialogRefStub = { afterClosed: () => of(true) } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);
    memberService.removeMember.mockReturnValue(of(undefined as unknown as void));

    component.removeMember(mockMember);

    expect(dialog.open).toHaveBeenCalled();
    expect(memberService.removeMember).toHaveBeenCalledWith('u1');
    expect(component.members.length).toBe(0);
  });

  it('SF-123-03 — removeMember confirm dialog annulé → n\'appelle pas removeMember', () => {
    const dialogRefStub = { afterClosed: () => of(false) } as unknown as MatDialogRef<unknown>;
    dialog.open.mockReturnValue(dialogRefStub);

    component.removeMember(mockMember);

    expect(memberService.removeMember).not.toHaveBeenCalled();
    expect(component.members.length).toBe(1);
  });

  it('revokeInvitation — retire l\'invitation de la liste', () => {
    invitationService.revokeInvitation.mockReturnValue(of(undefined as unknown as void));
    component.revokeInvitation(mockInvitation);
    expect(component.invitations.length).toBe(0);
  });

  it('isCurrentUser — renvoie true pour l\'utilisateur connecté', () => {
    expect(component.isCurrentUser(mockMember)).toBe(true);
  });

  it('roleLabel — renvoie le libellé français', () => {
    expect(component.roleLabel('OWNER')).toBe('Propriétaire');
    expect(component.roleLabel('LAWYER')).toBe('Avocat');
  });

  it('SF-123-03 — seatsLabel format pour plan cappé', () => {
    expect(component.seatsLabel(mockSeatsTeam4)).toBe('4 / 6 utilisateurs');
  });

  it('SF-123-03 — seatsLabel format pour plan illimité', () => {
    const pro: SeatsSummary = {
      ...mockSeatsTeam4, planCode: 'PRO', maxSeats: Number.MAX_SAFE_INTEGER, seatCount: 8
    };
    expect(component.seatsLabel(pro)).toContain('illimité');
  });

  it('SF-123-03 — totalEuros centimes → euros arrondis', () => {
    expect(component.totalEuros(mockSeatsTeam4)).toBe(278);
  });
});
