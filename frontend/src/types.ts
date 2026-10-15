export type Role = 'CUSTOMER' | 'TELLER' | 'MANAGER' | 'AUDITOR' | 'ADMIN';
export interface User { id: number; fullName: string; email: string; phone: string; role: Role; status: string; kycStatus: string | null; createdAt: string; lastLoginAt: string | null }
export interface Kyc { id: number; userId: number; userName: string; email: string; phone: string; dateOfBirth: string | null; address: string | null; city: string | null; state: string | null; pincode: string | null; idType: string | null; idNumberMasked: string | null; status: 'NOT_SUBMITTED' | 'PENDING' | 'APPROVED' | 'REJECTED'; reviewNote: string | null; reviewedBy: string | null; submittedAt: string | null; reviewedAt: string | null }
export interface Branch { id: number; name: string; ifsc: string; city: string; state: string }
export interface Bank { id: number; code: string; name: string; shortName: string; color: string; headquarters: string; branches: Branch[] }
export interface Account { id: number; accountNumber: string; bankId: number; bankName: string; bankShortName: string; bankColor: string; branchName: string; ifsc: string; type: string; balance: number; status: 'ACTIVE' | 'FROZEN' | 'CLOSED'; primary: boolean; pinLocked: boolean; openedAt: string; holderName: string }
export interface Upi { id: number; vpa: string; accountId: number; bankName: string; accountMasked: string; primary: boolean }
export interface Payee { name: string; vpa: string | null; bankName: string; accountMasked: string; ifsc: string }
export interface Tx { id: number; reference: string; type: 'TRANSFER' | 'CASH_DEPOSIT' | 'CASH_WITHDRAWAL'; channel: string; status: string; amount: number; remark: string | null; direction: 'CREDIT' | 'DEBIT' | 'SELF' | 'NONE'; fromName: string | null; fromBank: string | null; fromAccount: string | null; toName: string | null; toBank: string | null; toAccount: string | null; fromVpa: string | null; toVpa: string | null; balanceAfter: number | null; performedBy: string | null; createdAt: string }
export interface Page<T> { items: T[]; page: number; totalPages: number; total: number }
export interface PayReq { id: number; requesterName: string; requesterVpa: string | null; payerName: string; payerVpa: string | null; amount: number; note: string | null; status: string; direction: 'INCOMING' | 'OUTGOING'; createdAt: string; resolvedAt: string | null }
export interface Contact { id: number; name: string; vpa: string; nickname: string }
export interface Dashboard { totalBalance: number; income30: number; expense30: number; accountCount: number; kycStatus: string; pendingRequests: number; chart: { date: string; income: number; expense: number }[]; bankSplit: { bank: string; color: string; balance: number }[]; recent: Tx[] }
export interface Notice { id: number; title: string; message: string; type: string; seen: boolean; createdAt: string }
export interface Activity { action: string; success: boolean; ip: string | null; createdAt: string }
export interface AccountLookup { id: number; accountNumber: string; holderName: string; bankName: string; branchName: string; ifsc: string; type: string; status: string; balance: number }
export interface ManagerOverview { pendingKyc: number; customers: number; accounts: number; frozenAccounts: number; totalDeposits: number; transactionsToday: number; banks: { bank: string; color: string; accounts: number; balance: number }[] }
export interface AdminAccount { id: number; accountNumber: string; holderName: string; email: string; bankName: string; ifsc: string; type: string; balance: number; status: string; openedAt: string }
export interface Audit { id: number; actorEmail: string | null; action: string; entity: string | null; entityId: string | null; details: string | null; ip: string | null; success: boolean; createdAt: string }
export interface AdminStats { users: number; customers: number; staff: number; accounts: number; banks: number; transactions: number; totalDeposits: number; lockedUsers: number; transactionsToday: number }
export interface AdminUser { id: number; fullName: string; email: string; phone: string; role: Role; status: string; kycStatus: string | null; failedAttempts: number; locked: boolean; createdAt: string; lastLoginAt: string | null }
export interface Outbox { id: number; channel: string; recipient: string; subject: string | null; body: string; createdAt: string }
export interface TableInfo { name: string; type: 'TABLE' | 'VIEW'; rows: number; columns: number }
export interface ColumnInfo { name: string; type: string; key: string; nullable: boolean }
export interface TableData { table: string; columns: ColumnInfo[]; rows: unknown[][]; total: number; page: number; size: number }
export interface Relation { table: string; column: string; refTable: string; refColumn: string }
export interface QueryResult { columns: string[]; rows: unknown[][]; count: number; ms: number; truncated: boolean }
