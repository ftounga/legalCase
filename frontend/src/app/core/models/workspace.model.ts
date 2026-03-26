export interface Workspace {
  id: string;
  name: string;
  slug: string;
  planCode: string;
  status: string;
  expiresAt?: string | null;
  primary?: boolean;
  legalDomain?: string;
  country?: string;
}
