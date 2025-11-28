package br.jeanjacintho.tideflow.user_service.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import br.jeanjacintho.tideflow.user_service.model.User;
import br.jeanjacintho.tideflow.user_service.repository.CompanyAdminRepository;

public class UserResponseDTO {
    private UUID id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private String trustedEmail;
    private String city;
    private String state;
    private Boolean mustChangePassword;
    private UUID companyId;
    private UUID departmentId;
    private String systemRole;
    private String companyRole;
    private String privacyConsentStatus;
    private Boolean dataSharingEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserResponseDTO() {}

    public UserResponseDTO(UUID id, String name, String username, String email, String phone, String avatarUrl, String trustedEmail, String city, String state, Boolean mustChangePassword, UUID companyId, UUID departmentId, String systemRole, String companyRole, String privacyConsentStatus, Boolean dataSharingEnabled, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.trustedEmail = trustedEmail;
        this.city = city;
        this.state = state;
        this.mustChangePassword = mustChangePassword;
        this.companyId = companyId;
        this.departmentId = departmentId;
        this.systemRole = systemRole;
        this.companyRole = companyRole;
        this.privacyConsentStatus = privacyConsentStatus;
        this.dataSharingEnabled = dataSharingEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponseDTO fromEntity(User user) {
        return fromEntity(user, null);
    }

    public static UserResponseDTO fromEntity(User user, CompanyAdminRepository companyAdminRepository) {
        final String companyRole;
        if (user.getCompany() != null && companyAdminRepository != null) {
            companyRole = companyAdminRepository.findByUserIdAndCompanyId(user.getId(), user.getCompany().getId())
                .map(admin -> admin.getRole().name())
                .orElse(null);
        } else {
            companyRole = null;
        }

        return new UserResponseDTO(
            user.getId(),
            user.getName(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl(),
            user.getTrustedEmail(),
            user.getCity(),
            user.getState(),
            user.getMustChangePassword(),
            user.getCompany() != null ? user.getCompany().getId() : null,
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getSystemRole() != null ? user.getSystemRole().name() : null,
            companyRole,
            user.getPrivacyConsentStatus() != null ? user.getPrivacyConsentStatus().name() : null,
            user.getDataSharingEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTrustedEmail() {
        return trustedEmail;
    }

    public void setTrustedEmail(String trustedEmail) {
        this.trustedEmail = trustedEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Boolean getMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(Boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public String getCompanyRole() {
        return companyRole;
    }

    public void setCompanyRole(String companyRole) {
        this.companyRole = companyRole;
    }

    public String getPrivacyConsentStatus() {
        return privacyConsentStatus;
    }

    public void setPrivacyConsentStatus(String privacyConsentStatus) {
        this.privacyConsentStatus = privacyConsentStatus;
    }

    public Boolean getDataSharingEnabled() {
        return dataSharingEnabled;
    }

    public void setDataSharingEnabled(Boolean dataSharingEnabled) {
        this.dataSharingEnabled = dataSharingEnabled;
    }
}
