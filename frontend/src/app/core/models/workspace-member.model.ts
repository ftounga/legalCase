export interface WorkspaceMember {
  userId: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  memberRole: string;
  createdAt: string;
}
