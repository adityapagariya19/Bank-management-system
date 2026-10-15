package com.meridian.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    // ---------- generic ----------
    public record MessageDto(String message) {}
    public record PageDto<T>(List<T> items, int page, int totalPages, long total) {}

    // ---------- auth ----------
    public record RegisterRequest(
            @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Mobile number is required") @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a valid 10-digit Indian mobile number") String phone,
            @NotBlank(message = "Password is required") String password) {}
    public record LoginRequest(@NotBlank(message = "Email or mobile is required") String identifier,
                               @NotBlank(message = "Password is required") String password) {}
    public record IdentifierRequest(@NotBlank(message = "Email or mobile is required") String identifier) {}
    public record OtpVerifyRequest(@NotBlank(message = "Email or mobile is required") String identifier,
                                   @NotBlank(message = "Enter the 6-digit code") @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code) {}
    public record ResetPasswordRequest(@NotBlank String identifier,
                                       @NotBlank(message = "Enter the 6-digit code") @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code,
                                       @NotBlank(message = "New password is required") String newPassword) {}
    public record ChangePasswordRequest(@NotBlank(message = "Current password is required") String currentPassword,
                                        @NotBlank(message = "New password is required") String newPassword) {}
    public record UserDto(Long id, String fullName, String email, String phone, String role, String status,
                          String kycStatus, LocalDateTime createdAt, LocalDateTime lastLoginAt) {}
    public record AuthResponse(String token, UserDto user) {}

    // ---------- KYC ----------
    public record KycRequest(
            @NotNull(message = "Date of birth is required") LocalDate dateOfBirth,
            @NotBlank(message = "Address is required") @Size(max = 255) String address,
            @NotBlank(message = "City is required") String city,
            @NotBlank(message = "State is required") String state,
            @NotBlank(message = "PIN code is required") @Pattern(regexp = "\\d{6}", message = "PIN code must be 6 digits") String pincode,
            @NotBlank(message = "Select an ID type") String idType,
            @NotBlank(message = "ID number is required") String idNumber) {}
    public record KycReviewRequest(boolean approve, String note) {}
    public record KycDto(Long id, Long userId, String userName, String email, String phone, LocalDate dateOfBirth,
                         String address, String city, String state, String pincode, String idType, String idNumberMasked,
                         String status, String reviewNote, String reviewedBy, LocalDateTime submittedAt, LocalDateTime reviewedAt) {}

    // ---------- banks & accounts ----------
    public record BranchDto(Long id, String name, String ifsc, String city, String state) {}
    public record BankDto(Long id, String code, String name, String shortName, String color, String headquarters, List<BranchDto> branches) {}
    public record OpenAccountRequest(@NotNull(message = "Select a bank") Long bankId,
                                     @NotNull(message = "Select a branch") Long branchId,
                                     @NotBlank(message = "Select account type") String type,
                                     @NotBlank(message = "Set a PIN") String pin) {}
    public record AccountDto(Long id, String accountNumber, Long bankId, String bankName, String bankShortName, String bankColor,
                             String branchName, String ifsc, String type, BigDecimal balance, String status, boolean primary,
                             boolean pinLocked, LocalDateTime openedAt, String holderName) {}
    public record ChangePinRequest(@NotBlank(message = "Enter current PIN") String oldPin, @NotBlank(message = "Enter new PIN") String newPin) {}
    public record ResetPinRequest(@NotBlank(message = "Enter the 6-digit code") @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code,
                                  @NotBlank(message = "Enter new PIN") String newPin) {}

    // ---------- UPI ----------
    public record UpiDto(Long id, String vpa, Long accountId, String bankName, String accountMasked, boolean primary) {}
    public record CreateUpiRequest(@NotBlank(message = "Enter a UPI handle") String handle, @NotNull(message = "Select an account") Long accountId) {}
    public record LinkUpiRequest(@NotNull(message = "Select an account") Long accountId) {}
    public record PayeeDto(String name, String vpa, String bankName, String accountMasked, String ifsc) {}

    // ---------- payments ----------
    public record PayRequest(@NotNull(message = "Select a bank account to pay from") Long fromAccountId,
                             String vpa, String accountNumber, String ifsc,
                             @NotNull(message = "Enter an amount") @DecimalMin(value = "1.00", message = "Minimum amount is Rs 1") @Digits(integer = 9, fraction = 2, message = "Invalid amount") BigDecimal amount,
                             @Size(max = 100, message = "Remark is too long") String remark,
                             @NotBlank(message = "Enter your PIN") String pin,
                             String channel, Long requestId) {}
    public record TransactionDto(Long id, String reference, String type, String channel, String status, BigDecimal amount,
                                 String remark, String direction, String fromName, String fromBank, String fromAccount,
                                 String toName, String toBank, String toAccount, String fromVpa, String toVpa,
                                 BigDecimal balanceAfter, String performedBy, LocalDateTime createdAt) {}

    public record CreateRequestRequest(@NotBlank(message = "Enter a UPI ID or mobile number") String payee,
                                       @NotNull(message = "Enter an amount") @DecimalMin(value = "1.00", message = "Minimum amount is Rs 1") @Digits(integer = 9, fraction = 2) BigDecimal amount,
                                       @Size(max = 100) String note) {}
    public record PaymentRequestDto(Long id, String requesterName, String requesterVpa, String payerName, String payerVpa,
                                    BigDecimal amount, String note, String status, String direction,
                                    LocalDateTime createdAt, LocalDateTime resolvedAt) {}

    public record BeneficiaryRequest(@NotBlank(message = "UPI ID is required") String vpa, String nickname) {}
    public record BeneficiaryDto(Long id, String name, String vpa, String nickname) {}

    // ---------- dashboard / notifications ----------
    public record DailyPoint(String date, BigDecimal income, BigDecimal expense) {}
    public record BankSlice(String bank, String color, BigDecimal balance) {}
    public record DashboardDto(BigDecimal totalBalance, BigDecimal income30, BigDecimal expense30, long accountCount,
                               String kycStatus, long pendingRequests, List<DailyPoint> chart, List<BankSlice> bankSplit,
                               List<TransactionDto> recent) {}
    public record NotificationDto(Long id, String title, String message, String type, boolean seen, LocalDateTime createdAt) {}
    public record NotificationsDto(List<NotificationDto> items, long unread) {}
    public record ActivityDto(String action, boolean success, String ip, LocalDateTime createdAt) {}

    // ---------- staff ----------
    public record AccountLookupDto(Long id, String accountNumber, String holderName, String bankName, String branchName,
                                   String ifsc, String type, String status, BigDecimal balance) {}
    public record CounterRequest(@NotBlank(message = "Account number is required") String accountNumber,
                                 @NotNull(message = "Enter an amount") @DecimalMin(value = "1.00", message = "Minimum amount is Rs 1") @Digits(integer = 9, fraction = 2) BigDecimal amount,
                                 @Size(max = 100) String remark, String pin) {}
    public record BankSummary(String bank, String color, long accounts, BigDecimal balance) {}
    public record ManagerOverview(long pendingKyc, long customers, long accounts, long frozenAccounts, BigDecimal totalDeposits,
                                  long transactionsToday, List<BankSummary> banks) {}
    public record AdminAccountDto(Long id, String accountNumber, String holderName, String email, String bankName, String ifsc,
                                  String type, BigDecimal balance, String status, LocalDateTime openedAt) {}
    public record AuditDto(Long id, String actorEmail, String action, String entity, String entityId, String details,
                           String ip, boolean success, LocalDateTime createdAt) {}
    public record AdminStats(long users, long customers, long staff, long accounts, long banks, long transactions,
                             BigDecimal totalDeposits, long lockedUsers, long transactionsToday) {}
    public record AdminUserDto(Long id, String fullName, String email, String phone, String role, String status,
                               String kycStatus, int failedAttempts, boolean locked, LocalDateTime createdAt, LocalDateTime lastLoginAt) {}
    public record CreateStaffRequest(@NotBlank(message = "Name is required") String fullName,
                                     @NotBlank(message = "Email is required") @Email String email,
                                     @NotBlank(message = "Mobile is required") @Pattern(regexp = "[6-9]\\d{9}", message = "Enter a valid 10-digit mobile number") String phone,
                                     @NotBlank(message = "Role is required") String role,
                                     @NotBlank(message = "Password is required") String password) {}
    public record OutboxDto(Long id, String channel, String recipient, String subject, String body, LocalDateTime createdAt) {}

    // ---------- database explorer ----------
    public record TableInfo(String name, String type, long rows, int columns) {}
    public record ColumnInfo(String name, String type, String key, boolean nullable) {}
    public record TableData(String table, List<ColumnInfo> columns, List<List<Object>> rows, long total, int page, int size) {}
    public record Relation(String table, String column, String refTable, String refColumn) {}
    public record QueryRequest(@NotBlank(message = "Enter a SQL query") @Size(max = 2000) String sql) {}
    public record QueryResult(List<String> columns, List<List<Object>> rows, int count, long ms, boolean truncated) {}
}
