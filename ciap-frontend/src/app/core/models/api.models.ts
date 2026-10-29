// Mirrors the backend DTOs in tz.go.tirdo.ciap. Keep these in sync with the API.

/** Uniform envelope returned by every endpoint (common/dto/ApiResponse). */
export interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
  timestamp: string;
}

/** Paged envelope (common/dto/PageResponse). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ---- Auth ----
export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  email: string;
  fullName: string;
  roles: string[];
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// ---- Client ----
export type ClientType =
  | 'INDUSTRY' | 'SME' | 'INVESTOR' | 'RESEARCHER'
  | 'DEVELOPMENT_PARTNER' | 'GOVERNMENT_INSTITUTION';

export type IndustryCategory =
  | 'MANUFACTURING' | 'AGRO_PROCESSING' | 'MINING' | 'ENERGY'
  | 'CONSTRUCTION' | 'ICT' | 'ENVIRONMENTAL' | 'PHARMACEUTICAL'
  | 'FOOD_AND_BEVERAGE' | 'TEXTILE' | 'GOVERNMENT' | 'OTHER';

export interface ClientRegistrationRequest {
  companyName: string;
  tin?: string;
  clientType: ClientType;
  industryCategory: IndustryCategory;
  contactPerson: string;
  phone: string;
  email: string;
  password: string;
  region?: string;
  physicalAddress?: string;
  country?: string;
}

export interface ClientResponse {
  id: number;
  companyName: string;
  tin: string | null;
  clientType: ClientType;
  industryCategory: IndustryCategory;
  contactPerson: string;
  phone: string;
  email: string;
  region: string | null;
  physicalAddress: string | null;
  country: string | null;
}

// ---- Consultancy request ----
export type ConsultancyType =
  | 'INDUSTRIAL' | 'ENGINEERING' | 'ICT' | 'ERP' | 'ENVIRONMENTAL';

export type RequestStatus =
  | 'SUBMITTED' | 'UNDER_REVIEW' | 'QUOTED' | 'AWAITING_PAYMENT'
  | 'PAID' | 'ASSIGNED' | 'IN_EXECUTION' | 'REPORT_SUBMITTED'
  | 'CLOSED' | 'REJECTED' | 'CANCELLED';

export interface CreateRequestDto {
  clientId: number;
  consultancyType: ConsultancyType;
  title: string;
  description?: string;
  expectedStartDate?: string; // ISO yyyy-MM-dd
  serviceId?: number;
  seenPriceVersionId?: number;
  quantity?: number;
}

export interface RequestResponse {
  id: number;
  referenceNo: string;
  clientId: number;
  clientName: string;
  consultancyType: ConsultancyType;
  title: string;
  description: string | null;
  expectedStartDate: string | null;
  status: RequestStatus;
  assignedConsultantId: number | null;
  assignedConsultantName: string | null;
  reviewNotes: string | null;
  departmentId: number | null;
  departmentName: string | null;
  routingStatus: 'ROUTED' | 'UNMAPPED' | 'NO_HOD' | null;
  assignmentDeadline: string | null;
  createdAt: string | null;
  serviceId?: number | null;
  serviceName?: string | null;
  pricingMethod?: string | null;
  snapshotAmount?: number | null;
  snapshotCurrency?: string | null;
  priceLabel?: string | null;
}

// Human-readable labels for the select inputs.
export const CLIENT_TYPES: { value: ClientType; label: string }[] = [
  { value: 'INDUSTRY', label: 'Industry' },
  { value: 'SME', label: 'SME' },
  { value: 'INVESTOR', label: 'Investor' },
  { value: 'RESEARCHER', label: 'Researcher' },
  { value: 'DEVELOPMENT_PARTNER', label: 'Development Partner' },
  { value: 'GOVERNMENT_INSTITUTION', label: 'Government Institution' }
];

export const INDUSTRY_CATEGORIES: { value: IndustryCategory; label: string }[] = [
  { value: 'MANUFACTURING', label: 'Manufacturing' },
  { value: 'AGRO_PROCESSING', label: 'Agro-processing' },
  { value: 'MINING', label: 'Mining' },
  { value: 'ENERGY', label: 'Energy' },
  { value: 'CONSTRUCTION', label: 'Construction' },
  { value: 'ICT', label: 'ICT' },
  { value: 'ENVIRONMENTAL', label: 'Environmental' },
  { value: 'PHARMACEUTICAL', label: 'Pharmaceutical' },
  { value: 'FOOD_AND_BEVERAGE', label: 'Food & Beverage' },
  { value: 'TEXTILE', label: 'Textile' },
  { value: 'GOVERNMENT', label: 'Government' },
  { value: 'OTHER', label: 'Other' }
];

export const CONSULTANCY_TYPES: { value: ConsultancyType; label: string }[] = [
  { value: 'INDUSTRIAL', label: 'Industrial' },
  { value: 'ENGINEERING', label: 'Engineering' },
  { value: 'ICT', label: 'ICT' },
  { value: 'ERP', label: 'ERP' },
  { value: 'ENVIRONMENTAL', label: 'Environmental' }
];

// ============================================================
// Staff-side models (workflow, quotation, billing, consultant, project)
// ============================================================

// ---- Workflow transition bodies ----
export interface ReviewDto {
  approved: boolean;
  notes?: string;
}

export interface AssignDto {
  consultantId: number;
  note?: string;
  deadline?: string;
}

export interface ReassignDto {
  consultantId: number;
  reason: string;
  note?: string;
  deadline?: string;
}

export interface EligibleAssignee {
  consultantId: number;
  fullName: string;
  role: string;
  category: string;
  skills: string | null;
  activeAssignments: number;
  availabilityStatus: string;
}

export interface AssignmentRecord {
  id: number;
  consultantId: number;
  consultantName: string;
  previousConsultantId: number | null;
  previousConsultantName: string | null;
  assignedBy: string;
  note: string | null;
  reason: string | null;
  deadline: string | null;
  createdAt: string;
}

export interface DepartmentSummary {
  id: number;
  name: string;
  code: string;
  active: boolean;
  hodUserId: number | null;
  hodName: string | null;
  hodEmail: string | null;
}

export interface DepartmentQueueSummary {
  submitted: number;
  underReview: number;
  awaitingAssignment: number;
  assigned: number;
  inProgress: number;
  overdue: number;
  awaitingClose: number;
  rejected: number;
  unrouted: number;
}

export interface DepartmentMember {
  userId: number;
  fullName: string;
  email: string;
  role: string;
  canReceiveWork: boolean;
  active: boolean;
}

// ---- Consultant ----
export type ExpertCategory =
  | 'ENGINEER' | 'SCIENTIST' | 'ICT_SPECIALIST'
  | 'ENVIRONMENTAL_EXPERT' | 'INDUSTRIAL_TECHNOLOGIST';

export type AvailabilityStatus =
  | 'AVAILABLE' | 'PARTIALLY_AVAILABLE' | 'FULLY_BOOKED' | 'ON_LEAVE';

export interface ConsultantResponse {
  id: number;
  fullName: string;
  category: ExpertCategory;
  skills: string | null;
  yearsExperience: number | null;
  availabilityStatus: AvailabilityStatus;
  activeAssignments: number;
  performanceRating: number | null;
  billingRatePerHour: number | null;
}

// ---- Quotation ----
export type QuotationStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED';

export interface QuotationItemDto {
  description: string;
  quantity: number;
  unitPrice: number;
}

export interface CreateQuotationRequest {
  requestId: number;
  vatRate?: number;          // fraction, e.g. 0.18
  validUntil?: string;       // ISO yyyy-MM-dd
  notes?: string;
  items: QuotationItemDto[];
}

export interface QuotationResponse {
  id: number;
  quotationNo: string;
  requestId: number;
  requestReference: string;
  status: QuotationStatus;
  currency: string;
  subtotal: number;
  vatRate: number;
  vatAmount: number;
  total: number;
  validUntil: string | null;
  notes: string | null;
  versionNo: number;
}

export interface ContractResponse {
  id: number;
  contractNo: string;
  requestId: number;
  quotationId: number;
  status: string;
  contractValue: number;
  currency: string;
  startDate: string | null;
  endDate: string | null;
  signedByClient: boolean;
}

// ---- Billing ----
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';

export interface PaymentResponse {
  id: number;
  controlNumber: string;
  requestId: number;
  quotationId: number | null;
  amount: number;
  currency: string;
  status: PaymentStatus;
  gepgReference: string | null;
  paidAt: string | null;
  confirmedBy: string | null;
  confirmationNote: string | null;
}

export interface GepgCallbackRequest {
  controlNumber: string;
  gepgReference: string;
  confirmationNote?: string;
}

export interface FinancePayment {
  id: number;
  controlNumber: string;
  requestId: number;
  requestReference: string;
  clientName: string;
  quotationNo: string | null;
  amount: number;
  currency: string;
  status: PaymentStatus;
  gepgReference: string | null;
  paidAt: string | null;
  createdAt: string | null;
  confirmedBy: string | null;
  confirmationNote: string | null;
  requestStatus: string;
  mismatch: boolean;
}

export interface FinanceSummary {
  quoted: number;
  pendingVerification: number;
  verified: number;
  failed: number;
  mismatches: number;
}

export interface RequestEvent {
  id: number;
  requestId: number;
  fromStatus: string | null;
  toStatus: string;
  actor: string;
  reason: string | null;
  createdAt: string;
}

export interface AppNotification {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
}

// ---- Project ----
export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'CANCELLED';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE';

export interface CreateProjectRequest {
  name: string;
  description?: string;
  plannedStartDate?: string;
  plannedEndDate?: string;
  leadConsultantId?: number;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  assigneeConsultantId?: number;
  dueDate?: string;
}

export interface CreateMilestoneRequest {
  title: string;
  dueDate?: string;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  assigneeId: number | null;
  assigneeName: string | null;
  dueDate: string | null;
  completedAt: string | null;
}

export interface MilestoneResponse {
  id: number;
  title: string;
  dueDate: string | null;
  achieved: boolean;
  achievedDate: string | null;
}

export interface ProjectResponse {
  id: number;
  projectCode: string;
  requestId: number;
  requestReference: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  progressPercent: number;
  leadConsultantId: number | null;
  leadConsultantName: string | null;
  plannedStartDate: string | null;
  plannedEndDate: string | null;
  actualStartDate: string | null;
  actualEndDate: string | null;
  tasks: TaskResponse[];
  milestones: MilestoneResponse[];
}

// Labels
export const EXPERT_CATEGORIES: { value: ExpertCategory; label: string }[] = [
  { value: 'ENGINEER', label: 'Engineer' },
  { value: 'SCIENTIST', label: 'Scientist' },
  { value: 'ICT_SPECIALIST', label: 'ICT Specialist' },
  { value: 'ENVIRONMENTAL_EXPERT', label: 'Environmental Expert' },
  { value: 'INDUSTRIAL_TECHNOLOGIST', label: 'Industrial Technologist' }
];

// ---- Document Management ----
export type DocumentCategory =
  | 'REQUIREMENT' | 'QUOTATION' | 'CONTRACT' | 'REPORT'
  | 'INVOICE' | 'CORRESPONDENCE' | 'OTHER';

export interface DocumentResponse {
  id: number;
  requestId: number;
  requestReference: string;
  category: DocumentCategory;
  originalFilename: string;
  contentType: string | null;
  sizeBytes: number;
  description: string | null;
  uploadedBy: string | null;
  uploadedAt: string;
}

export const DOCUMENT_CATEGORIES: { value: DocumentCategory; label: string }[] = [
  { value: 'REQUIREMENT', label: 'Requirement' },
  { value: 'QUOTATION', label: 'Quotation' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'REPORT', label: 'Report' },
  { value: 'INVOICE', label: 'Invoice' },
  { value: 'CORRESPONDENCE', label: 'Correspondence' },
  { value: 'OTHER', label: 'Other' }
];

// ---- Knowledge Portal ----
export interface CreateArticleRequest {
  title: string;
  category?: string;
  summary?: string;
  body: string;
  published?: boolean;
}

export interface ArticleResponse {
  id: number;
  title: string;
  category: string | null;
  summary: string | null;
  body: string | null;
  published: boolean;
  author: string | null;
  createdAt: string;
  updatedAt: string | null;
}

// ---- Training / Webinars ----
export type TrainingMode = 'ONLINE' | 'IN_PERSON' | 'HYBRID';

export interface CreateEventRequest {
  title: string;
  description?: string;
  mode: TrainingMode;
  location?: string;
  startsAt: string;   // ISO instant
  endsAt?: string;
  capacity?: number;
  published?: boolean;
  feeKind?: 'FREE' | 'PAID';
  feeAmount?: number;
  feeCurrency?: string;
  registrationOpensAt?: string;
  registrationClosesAt?: string;
  objectives?: string;
  audience?: string;
  prerequisites?: string;
  contactEmail?: string;
}

export interface EventResponse {
  id: number;
  title: string;
  description: string | null;
  mode: TrainingMode;
  location: string | null;
  startsAt: string;
  endsAt: string | null;
  capacity: number;
  registeredCount: number;
  published: boolean;
  registered: boolean;
  feeKind?: 'FREE' | 'PAID';
  feeAmount?: number | null;
  feeCurrency?: string | null;
  objectives?: string | null;
  audience?: string | null;
  prerequisites?: string | null;
  contactEmail?: string | null;
  registrationStatus?: 'CONFIRMED' | 'AWAITING_PAYMENT' | 'CANCELLED' | null;
  controlNumber?: string | null;
}

export interface CategoryOffer {
  id: number;
  code: string;
  name: string;
  description: string | null;
  consultancyType: ConsultancyType;
  displayOrder: number;
  active: boolean;
}

export type PricingMethod = 'FIXED' | 'STARTING' | 'QUOTATION' | 'VARIABLE' | 'CUSTOM';

export interface ServiceOffer {
  id: number;
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  consultancyType: ConsultancyType;
  name: string;
  description: string | null;
  pricingMethod: PricingMethod;
  amount: number | null;
  currency: string;
  unitLabel: string | null;
  conditions: string | null;
  priceVersionId: number | null;
  customerLabel: string;
  published: boolean;
  active: boolean;
}

export interface PriceVersion {
  id: number;
  pricingMethod: PricingMethod;
  amount: number | null;
  currency: string;
  unitLabel: string | null;
  note: string | null;
  changedBy: string;
  changedAt: string;
  customerLabel: string;
}

export interface TrainingPaymentRow {
  id: number;
  controlNumber: string;
  registrationId: number;
  eventId: number;
  eventTitle: string;
  registrantEmail: string;
  amount: number;
  currency: string;
  status: string;
  gepgReference: string | null;
  paidAt: string | null;
}

export const TRAINING_MODES: { value: TrainingMode; label: string }[] = [
  { value: 'ONLINE', label: 'Online' },
  { value: 'IN_PERSON', label: 'In person' },
  { value: 'HYBRID', label: 'Hybrid' }
];
