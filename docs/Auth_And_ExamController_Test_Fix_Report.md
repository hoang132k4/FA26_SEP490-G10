# Authentication & ExamControllerTest Failure Investigation and Fix Report

**Project**: JEPS (`jlpp-platform`)  
**Date**: 2026-11-12  
**Scope**: Part 1 (Authentication & CSRF Regression Fixes) and Part 2 (`ExamControllerTest` ApplicationContext Initialization)

---

## 1. Root Cause of Failing Authentication Tests (Part 1)

### 1.1 Post-Login and Complete-Profile Redirect Changes (Commit `362e51a`)
Commit `362e51a0e2655ea01fb019c35a077a9385d53c19` (`feat(ui): refine learner header navigation, notification bell dropdown and login redirect to home`) updated the learner post-authentication redirect destination from `/learning/dashboard` to `/` (Home Page):

1. [`CustomAuthenticationSuccessHandler.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/auth/security/CustomAuthenticationSuccessHandler.java#L57-L72) (`determineTargetUrl`):
   - `ROLE_ADMIN` $\rightarrow$ `/admin/dashboard`
   - `ROLE_MANAGER` $\rightarrow$ `/manager/dashboard`
   - `ROLE_LEARNER` $\rightarrow$ `/`
   - Google OAuth2 user missing `phone` (`onAuthenticationSuccess`, lines 37–44) $\rightarrow$ `/auth/complete-profile`
2. [`AuthController.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/auth/controller/AuthController.java#L267-L321):
   - `GET /auth/complete-profile` when phone is already populated (lines 268–270) $\rightarrow$ `redirect:/`
   - `POST /auth/complete-profile` upon saving phone number (line 321) $\rightarrow$ `redirect:/`

Four existing tests still asserted the pre-`362e51a` redirect URL `/learning/dashboard`:
- [`LoginAndSecurityTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/LoginAndSecurityTest.java#L87-L94) — `testLG01_ValidLearnerLogin`
- [`GoogleOAuth2IntegrationTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/GoogleOAuth2IntegrationTest.java#L109-L116) — `testExistingFormLogin_Success`
- [`GoogleOAuth2IntegrationTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/GoogleOAuth2IntegrationTest.java#L146-L173) — `testCompleteProfile_SubmitPhone_Success`
- [`ChangePasswordTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/ChangePasswordTest.java#L209-L233) — `testCP11_and_CP12_LoginVerificationAfterChange`

### 1.2 Verified Intended Redirect Destinations by Scenario

| Scenario | Source of Truth | Verified Redirect Destination |
|---|---|---|
| 1. Successful Form Login (`ROLE_LEARNER`) | `CustomAuthenticationSuccessHandler.determineTargetUrl` (line 67) | `/` |
| 2. Successful Form Login (`ROLE_MANAGER`) | `CustomAuthenticationSuccessHandler.determineTargetUrl` (line 65) | `/manager/dashboard` |
| 3. Successful Form Login (`ROLE_ADMIN`) | `CustomAuthenticationSuccessHandler.determineTargetUrl` (line 63) | `/admin/dashboard` |
| 4. Successful Google OAuth2 Login (missing phone) | `CustomAuthenticationSuccessHandler.onAuthenticationSuccess` (line 41) | `/auth/complete-profile` |
| 5. Successful Google OAuth2 Login (phone present, `ROLE_LEARNER`) | `CustomAuthenticationSuccessHandler.determineTargetUrl` (line 67) | `/` |
| 6. Successful Login after Password Change (`ROLE_LEARNER`) | `CustomAuthenticationSuccessHandler.determineTargetUrl` (line 67) | `/` |
| 7. Successful Completion of User Profile (`POST /auth/complete-profile`) | `AuthController.handleCompleteProfile` (line 321) | `/` |

---

## 2. Root Cause and Fix for CSRF Protection Test (`testCP14_CsrfProtectionEnforced`)

### 2.1 Root Cause
In [`ChangePasswordTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/ChangePasswordTest.java#L249-L258), `testCP14_CsrfProtectionEnforced` sends an authenticated `POST /profile/change-password` request (`@WithMockUser(username = "learner_cp@jeps.edu.vn", roles = {"LEARNER"})`) without a CSRF token and expects HTTP `403 Forbidden`, but received HTTP `302 Found` (`Location: /auth/login?sessionExpired=true`).

Inspection of [`SecurityConfig.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/common/config/SecurityConfig.java#L164-L175) revealed that commit `2cbde875b3493bd9341860cf60c4b20a9c8efd46` configured a custom `accessDeniedHandler`:

```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
boolean isAnonymous = (auth == null || auth instanceof AnonymousAuthenticationToken);
boolean isCsrf = accessDeniedException instanceof CsrfException;
if (isAnonymous || isCsrf) {
    response.sendRedirect(request.getContextPath() + "/auth/login?sessionExpired=true");
} else {
    response.sendError(403, "Access Denied: You do not have permission to access this resource.");
}
```

Because `isCsrf` was combined with `||`, even when an **authenticated** user (`isAnonymous == false`) received a `CsrfException` (`InvalidCsrfTokenException` or `MissingCsrfTokenException`), the handler redirected with `302 Found` to `/auth/login?sessionExpired=true` instead of rejecting the request with `403 Forbidden`.

### 2.2 Security Fix Applied
In [`SecurityConfig.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/common/config/SecurityConfig.java#L164-L174), the `accessDeniedHandler` condition was corrected to check `if (isAnonymous)`:

```java
.exceptionHandling(ex -> ex
        .accessDeniedHandler((request, response, accessDeniedException) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAnonymous = (auth == null || auth instanceof AnonymousAuthenticationToken);
            if (isAnonymous) {
                response.sendRedirect(request.getContextPath() + "/auth/login?sessionExpired=true");
            } else {
                response.sendError(403, "Access Denied: You do not have permission to access this resource.");
            }
        })
)
```

### 2.3 Resulting Security Behavior
- **Authenticated user + protected `POST` + missing or invalid CSRF token**: Rejected by `CsrfFilter` and handled by `accessDeniedHandler` (`isAnonymous == false`) $\rightarrow$ **HTTP `403 Forbidden`**.
- **Authenticated user + valid CSRF token**: Passes `CsrfFilter` and proceeds to controller validation/business logic.
- **Unauthenticated / expired-session user + protected `POST`**: `isAnonymous == true` $\rightarrow$ **HTTP `302 Found`** redirecting to `/auth/login?sessionExpired=true` (or `/auth/login` when a valid test CSRF token is supplied and `AuthorizationFilter` triggers `AuthenticationEntryPoint`).

---

## 3. Root Cause and Fix for `ExamControllerTest` ApplicationContext Failure (Part 2)

### 3.1 Root Cause
In commit `362e51a`, [`GlobalUserAdvice.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/common/config/GlobalUserAdvice.java#L16-L22) (`@ControllerAdvice`) added a required constructor dependency on [`NotificationService`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/modules/notification/service/NotificationService.java):

```java
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalUserAdvice {
    private final UserRepository userRepository;
    private final NotificationService notificationService;
```

Because [`ExamControllerTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/exam/ExamControllerTest.java#L36-L55) is a `@WebMvcTest(ExamController.class)` slice test, Spring loads `@ControllerAdvice` beans (`GlobalUserAdvice`) into the MVC test context, but does not scan `@Service` implementations (`NotificationServiceImpl`). Without a mocked `NotificationService` bean in the test context, `GlobalUserAdvice` failed with `UnsatisfiedDependencyException` / `NoSuchBeanDefinitionException: No qualifying bean of type 'com.jlpp.modules.notification.service.NotificationService' available`.

### 3.2 Fix Applied
Added `@MockBean private NotificationService notificationService;` in [`ExamControllerTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/exam/ExamControllerTest.java#L52-L54).

---

## 4. Exact Source Files Modified

1. [`src/main/java/com/jlpp/common/config/SecurityConfig.java`](file:///c:/Users/hoang/Downloads/JEPS/src/main/java/com/jlpp/common/config/SecurityConfig.java#L164-L174) — Fixed `accessDeniedHandler` so authenticated requests failing CSRF validation return HTTP `403 Forbidden`.
2. [`src/test/java/com/jlpp/modules/auth/ChangePasswordTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/ChangePasswordTest.java#L226-L265) — Updated learner post-login redirect expectation in `testCP11_and_CP12_LoginVerificationAfterChange` to `/` and verified both missing and invalid CSRF tokens return `403 Forbidden` in `testCP14_CsrfProtectionEnforced`.
3. [`src/test/java/com/jlpp/modules/auth/GoogleOAuth2IntegrationTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/GoogleOAuth2IntegrationTest.java#L107-L173) — Updated `testExistingFormLogin_Success` and `testCompleteProfile_SubmitPhone_Success` redirect assertions to `/`.
4. [`src/test/java/com/jlpp/modules/auth/LoginAndSecurityTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/auth/LoginAndSecurityTest.java#L85-L94) — Updated `testLG01_ValidLearnerLogin` redirect assertion to `/`.
5. [`src/test/java/com/jlpp/modules/exam/ExamControllerTest.java`](file:///c:/Users/hoang/Downloads/JEPS/src/test/java/com/jlpp/modules/exam/ExamControllerTest.java#L52-L54) — Provided `@MockBean private NotificationService notificationService;` for `GlobalUserAdvice`.

---

## 5. Before / After Test Results

| Test Suite | Before Fix | After Fix |
|---|---|---|
| `ChangePasswordTest` | 15 run, **2 Failures** (`testCP11_and_CP12`, `testCP14`) | **15 PASS, 0 FAIL, 0 ERROR, 0 SKIP** |
| `GoogleOAuth2IntegrationTest` | 7 run, **2 Failures** (`testExistingFormLogin_Success`, `testCompleteProfile_SubmitPhone_Success`) | **7 PASS, 0 FAIL, 0 ERROR, 0 SKIP** |
| `LoginAndSecurityTest` | 22 run, **1 Failure** (`testLG01_ValidLearnerLogin`) | **22 PASS, 0 FAIL, 0 ERROR, 0 SKIP** |
| `ExamControllerTest` | 14 affected by `ApplicationContext` failure (`NotificationService` missing) | **14 PASS, 0 FAIL, 0 ERROR, 0 SKIP** |
| **Combined Total** | **44 run (5 failures) + 14 context errors** | **58 PASS, 0 FAIL, 0 ERROR, 0 SKIP** |

---

## 6. Security & Assessment Regression Verification

- **Authentication & Role-Based Redirects**:
  - Learner login (`LG-01`, `Auth-1`, `CP-12`) $\rightarrow$ `/`
  - Manager login (`LG-02`) $\rightarrow$ `/manager/dashboard`
  - Admin login (`LG-03`) $\rightarrow$ `/admin/dashboard`
  - Invalid credentials (`LG-04`..`LG-08`, `CP-11`) $\rightarrow$ `/auth/login?error=true`
  - Disabled account (`LG-09`) $\rightarrow$ `/auth/login?disabled=true`
  - Logout & session invalidation (`LG-11`, `LG-12`) $\rightarrow$ `/auth/login?logout=true` and old session rejected
- **CSRF Enforcement**:
  - Authenticated `POST /profile/change-password` without CSRF token or with invalid CSRF token (`CP-14`) $\rightarrow$ `403 Forbidden`
  - Authenticated `POST /profile/change-password` with valid CSRF token (`CP-10`, `CP-15`) $\rightarrow$ succeeds (`302` to `/profile/change-password`)
- **Role-Based Access Control (`RB-01`..`RB-10`)**:
  - Cross-role access to `/manager/dashboard`, `/admin/dashboard`, `/admin/exams` returns `403 Forbidden`.
- **Learner Exam Workflows (`ExamControllerTest`)**:
  - All 14 controller tests pass (Placement, Milestone, JLPT Mock, exam submission, result viewing, IDOR history protection, and unauthenticated redirects).

---

## 7. Remaining Blockers or Risks

- None for Part 1 or Part 2. All 58 tests across `ChangePasswordTest`, `GoogleOAuth2IntegrationTest`, `LoginAndSecurityTest`, and `ExamControllerTest` pass with zero failures or errors.

