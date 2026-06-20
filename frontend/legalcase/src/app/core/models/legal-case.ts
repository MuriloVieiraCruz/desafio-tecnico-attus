export type CaseStatus = 'FILED' | 'IN_PROGRESS' | 'SUSPENDED' | 'ARCHIVED' | 'CLOSED';

export interface LegalCase {
  id: number;
  cnjNumber: string;
  plaintiff: string;
  defendant: string;
  court: string;
  judicialDistrict: string | null;
  claimValue: number | null;
  filingDate: string | null;
  status: CaseStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateLegalCaseRequest {
  cnjNumber: string;
  plaintiff: string;
  defendant: string;
  court: string;
  judicialDistrict?: string | null;
  claimValue?: number | null;
  filingDate?: string | null;
}

export interface UpdateLegalCaseRequest {
  plaintiff: string;
  defendant: string;
  court: string;
  judicialDistrict?: string | null;
  claimValue?: number | null;
  filingDate?: string | null;
}

export interface UpdateStatusRequest {
  newStatus: CaseStatus;
}

export interface LegalCaseFilter {
  status?: CaseStatus | null;
  party?: string | null;
  court?: string | null;
  filingDateFrom?: string | null;
  filingDateTo?: string | null;
}

export const CASE_STATUSES: CaseStatus[] = [
  'FILED', 'IN_PROGRESS', 'SUSPENDED', 'ARCHIVED', 'CLOSED',
];

export const CASE_STATUS_LABELS: Record<CaseStatus, string> = {
  FILED: 'Distribuído',
  IN_PROGRESS: 'Em andamento',
  SUSPENDED: 'Suspenso',
  ARCHIVED: 'Arquivado',
  CLOSED: 'Baixado',
};

export const ALLOWED_TRANSITIONS: Record<CaseStatus, CaseStatus[]> = {
  FILED: ['IN_PROGRESS'],
  IN_PROGRESS: ['SUSPENDED', 'ARCHIVED', 'CLOSED'],
  SUSPENDED: ['IN_PROGRESS'],
  ARCHIVED: [],
  CLOSED: [],
};