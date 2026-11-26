package com.jeps.modules.admin.audit;

import com.jeps.modules.admin.entity.AdminSystemLog;
import com.jeps.modules.admin.repository.AdminSystemLogRepository;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerActivityLoggingInterceptorTest {

    @Mock
    private AdminSystemLogRepository systemLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectProvider<AdminSystemLogRepository> systemLogRepositoryProvider;

    @Mock
    private ObjectProvider<UserRepository> userRepositoryProvider;

    private ManagerActivityLoggingInterceptor interceptor;

    @BeforeEach
    void provideRepositories() {
        lenient().when(systemLogRepositoryProvider.getIfAvailable()).thenReturn(systemLogRepository);
        lenient().when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        interceptor = new ManagerActivityLoggingInterceptor(
                systemLogRepositoryProvider, userRepositoryProvider);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsManagerCreateWithActorAndRequestDetails() {
        authenticateAs("manager@example.com", "ROLE_MANAGER");
        User manager = new User();
        manager.setId(92001L);
        when(userRepository.findByEmailIgnoreCase("manager@example.com")).thenReturn(Optional.of(manager));

        MockHttpServletRequest createRequest = request("POST", "/manager/courses");
        createRequest.setParameter("name", "Khóa học N5");
        createRequest.setParameter("code", "N5-01");
        createRequest.setParameter("level", "N5");
        createRequest.setParameter("password", "do-not-log");
        interceptor.afterCompletion(createRequest,
                response(HttpServletResponse.SC_MOVED_TEMPORARILY), new Object(), null);

        ArgumentCaptor<AdminSystemLog> logCaptor = ArgumentCaptor.forClass(AdminSystemLog.class);
        verify(systemLogRepository).save(logCaptor.capture());
        AdminSystemLog log = logCaptor.getValue();
        assertThat(log.getUserId()).isEqualTo(92001L);
        assertThat(log.getLogType()).isEqualTo("MANAGER");
        assertThat(log.getActionPerformed()).isEqualTo("MANAGER_CREATE");
        assertThat(log.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(log.getDetails())
                .contains("tạo mới khóa học", "Khóa học N5", "N5-01", "cấp độ=\"N5\"")
                .doesNotContain("do-not-log", "POST", "/manager/courses", "HTTP");
    }

    @Test
    void logsDeleteRoutesAndFailedManagerAttempts() {
        authenticateAs("manager@example.com", "ROLE_MANAGER");
        MockHttpServletRequest deleteRequest = request("POST", "/manager/questions/42/delete");
        interceptor.afterCompletion(deleteRequest,
                response(HttpServletResponse.SC_MOVED_TEMPORARILY), new Object(), null);

        interceptor.afterCompletion(request("POST", "/manager/questions/43"),
                response(HttpServletResponse.SC_BAD_REQUEST), new Object(), null);

        ArgumentCaptor<AdminSystemLog> logCaptor = ArgumentCaptor.forClass(AdminSystemLog.class);
        verify(systemLogRepository, times(2)).save(logCaptor.capture());
        assertThat(logCaptor.getAllValues())
                .extracting(AdminSystemLog::getActionPerformed)
                .containsExactly("MANAGER_DELETE", "MANAGER_UPDATE");
        assertThat(logCaptor.getAllValues().get(0).getDetails()).contains("xóa câu hỏi #42");
        assertThat(logCaptor.getAllValues().get(1).getDetails())
                .contains("cập nhật câu hỏi #43")
                .doesNotContain("HTTP 400", "POST");
    }

    @Test
    void logsManagerPostReturningOkFromAnyApplicationRoute() {
        authenticateAs("manager@example.com", "ROLE_MANAGER");

        interceptor.afterCompletion(request("POST", "/profile/edit"),
                response(HttpServletResponse.SC_OK), new Object(), null);

        ArgumentCaptor<AdminSystemLog> logCaptor = ArgumentCaptor.forClass(AdminSystemLog.class);
        verify(systemLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getActionPerformed()).isEqualTo("MANAGER_UPDATE");
        assertThat(logCaptor.getValue().getDetails())
                .isEqualTo("cập nhật hồ sơ")
                .doesNotContain("POST", "/profile/edit", "HTTP");
    }

    @Test
    void doesNotLogAdminOrReadOnlyRequests() {
        authenticateAs("admin@example.com", "ROLE_ADMIN");
        interceptor.afterCompletion(request("POST", "/manager/courses"),
                response(HttpServletResponse.SC_MOVED_TEMPORARILY), new Object(), null);

        authenticateAs("manager@example.com", "ROLE_MANAGER");
        interceptor.afterCompletion(request("GET", "/manager/courses"),
                response(HttpServletResponse.SC_OK), new Object(), null);

        verify(systemLogRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void authenticateAs(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "credentials",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        request.setServletPath(path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private HttpServletResponse response(int status) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        return response;
    }
}
