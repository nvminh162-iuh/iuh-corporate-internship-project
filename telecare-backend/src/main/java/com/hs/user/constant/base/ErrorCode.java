package com.hs.user.constant.base;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public enum ErrorCode {
    // COMMON
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "You do not have permission", HttpStatus.FORBIDDEN),
    ROUTE_NOT_FOUND(1012, "Route not found", HttpStatus.NOT_FOUND),
    // USER
    USER_EXISTED(1004, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    USER_ALREADY_ONBOARDED(1006, "User already onboarded", HttpStatus.BAD_REQUEST),
    INVALID_OLD_PASSWORD(1007, "Invalid old password", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1008, "Password is required", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_REQUIRED(1009, "New password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_SHORT(1010, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_WEAK(1011, "Password must include at least 1 uppercase letter, 1 digit and 1 special character", HttpStatus.BAD_REQUEST),
    USERNAME_EXISTED(1013, "Username existed", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1014, "Email existed", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_VERIFIED(1015, "Email already verified", HttpStatus.BAD_REQUEST),
    USER_ALREADY_HAS_ROLE(1016, "User already has this role", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1017, "Phone number existed", HttpStatus.CONFLICT),
    USER_DISABLED(1018, "User is disabled", HttpStatus.BAD_REQUEST),
    PASSWORD_ALREADY_SET(1019, "Password has already been set", HttpStatus.CONFLICT),
    USER_ALREADY_ACTIVATED(1020, "User already completed invitation actions", HttpStatus.CONFLICT),
    USER_CANNOT_DISABLE_SELF(1021, "You cannot disable your own account", HttpStatus.CONFLICT),
    USER_CANNOT_UPDATE_OWN_ROLE(1022, "You cannot update your own role", HttpStatus.CONFLICT),
    // ROLE
    ROLE_EXISTED(1101, "Role existed", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXISTED(1102, "Role not existed", HttpStatus.NOT_FOUND),
    // PERMISSION
    PERMISSION_EXISTED(1201, "Permission existed", HttpStatus.BAD_REQUEST),
    PERMISSION_NOT_EXISTED(1202, "Permission not existed", HttpStatus.NOT_FOUND),
    // KEYCLOAK
    KEYCLOAK_USER_UPDATE_FAILED(1301, "Keycloak user update failed", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_PASSWORD_UPDATE_FAILED(1302, "Keycloak password update failed", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_CREDENTIAL_READ_FAILED(1303, "Could not read Keycloak credentials", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_USER_CREATE_FAILED(1304, "Keycloak user creation failed", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_INVITATION_SEND_FAILED(1305, "Failed to send invitation email", HttpStatus.BAD_GATEWAY),
    // STORAGE / FILE
    INVALID_FILE(1501, "Invalid file", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1502, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    int code;
    String message;
    HttpStatusCode statusCode;
}
