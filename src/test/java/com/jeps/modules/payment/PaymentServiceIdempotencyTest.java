package com.jeps.modules.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeps.common.constants.RoleEnum;
import com.jeps.modules.course.entity.Course;
import com.jeps.modules.course.entity.CourseRegistration;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.dto.CourseRegistrationRequest;
import com.jeps.modules.course.repository.CourseRegistrationRepository;
import com.jeps.modules.course.repository.CourseRepository;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.course.service.CourseRegistrationService;
import com.jeps.modules.payment.dto.PaymentRequestDTO;
import com.jeps.modules.payment.dto.PaymentResponseDTO;
import com.jeps.modules.payment.dto.SePayWebhookResult;
import com.jeps.modules.payment.dto.InvoiceResponseDTO;
import com.jeps.modules.payment.entity.Payment;
import com.jeps.modules.payment.entity.PaymentTransaction;
import com.jeps.modules.payment.entity.PaymentWebhookLog;
import com.jeps.modules.payment.service.PaymentExpiryService;
import com.jeps.modules.payment.repository.PaymentRepository;
import com.jeps.modules.payment.repository.PaymentTransactionRepository;
import com.jeps.modules.payment.repository.PaymentWebhookLogRepository;
import com.jeps.modules.payment.security.SepayAccountNumberCryptoService;
import com.jeps.modules.payment.service.PaymentReconciliationService;
import com.jeps.modules.payment.service.PaymentOrderCodeGenerator;
import com.jeps.modules.payment.service.PaymentService;
import com.jeps.modules.user.entity.User;
import com.jeps.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "BANK_ACCOUNT_NUMBER=123456789",
        "SEPAY_WEBHOOK_SECRET=payment-test-root-secret",
        "payment.expiry-sweep-interval-ms=3600000"
})
class PaymentServiceIdempotencyTest {

    private static final AtomicLong COURSE_IDS = new AtomicLong(990000L);
    private static final AtomicLong SEPAY_TRANSACTION_IDS = new AtomicLong(System.currentTimeMillis() * 1000L);
    private static final String TEST_WEBHOOK_SECRET = "payment-test-root-secret";

    @Autowired
    private PaymentService paymentService;

    @SpyBean
    private CourseRegistrationService courseRegistrationService;

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private PaymentWebhookLogRepository paymentWebhookLogRepository;

    @SpyBean
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SepayAccountNumberCryptoService accountNumberCryptoService;

    @Autowired
    private PaymentReconciliationService paymentReconciliationService;

    @Autowired
    private PaymentExpiryService paymentExpiryService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @SpyBean
    private PaymentOrderCodeGenerator orderCodeGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private Course course;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        user = userRepository.saveAndFlush(User.builder()
                .email("payment-" + unique + "@example.test")
                .username("payment-" + unique)
                .fullName("Payment Test")
                .password("unused")
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        course = courseRepository.saveAndFlush(Course.builder()
                .id(COURSE_IDS.incrementAndGet())
                .title("Payment Test Course")
                .level("N4")
                .status("PUBLISHED")
                .price(250000.0)
                .build());
    }

    @Test
    void reusesUnexpiredPendingPayment() {
        PaymentResponseDTO first = createPayment();
        String legacyId = UUID.randomUUID().toString();
        Payment expiredPending = paymentRepository.saveAndFlush(Payment.builder()
                .orderCode("JLPP-" + legacyId)
                .user(user)
                .course(course)
                .amount(first.getAmount())
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("PENDING")
                .transferContent("legacy-" + legacyId)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build());

        PaymentResponseDTO second = createPayment();

        assertEquals(first.getId(), second.getId());
        assertEquals("EXPIRED", findPayment(expiredPending.getId()).getStatus());
    }

    @Test
    void createsNewPaymentAfterExpiredOrFailedPayment() {
        PaymentResponseDTO expired = createPayment();
        setPaymentState(expired.getId(), "PENDING", LocalDateTime.now().minusMinutes(1));
        PaymentResponseDTO afterExpired = createPayment();

        assertNotEquals(expired.getId(), afterExpired.getId());
        assertEquals("EXPIRED", findPayment(expired.getId()).getStatus());

        setPaymentState(afterExpired.getId(), "FAILED", LocalDateTime.now().plusMinutes(10));
        PaymentResponseDTO afterFailed = createPayment();

        assertNotEquals(afterExpired.getId(), afterFailed.getId());

        setPaymentState(afterFailed.getId(), "CANCELLED", LocalDateTime.now().plusMinutes(10));
        PaymentResponseDTO afterCancelled = createPayment();

        assertNotEquals(afterFailed.getId(), afterCancelled.getId());
    }

    @Test
    void pendingPaymentWithoutExpiryIsExpiredAndReplaced() {
        PaymentResponseDTO pending = createPayment();
        setPaymentState(pending.getId(), "PENDING", null);

        PaymentResponseDTO replacement = createPayment();

        assertNotEquals(pending.getId(), replacement.getId());
        assertEquals("EXPIRED", findPayment(pending.getId()).getStatus());
        assertNotNull(findPayment(pending.getId()));
        assertEquals("PENDING", findPayment(replacement.getId()).getStatus());
    }

    @Test
    void expirySweepOnlyExpiresPendingPaymentsAtOrBeforeTheCutoff() {
        LocalDateTime cutoff = LocalDateTime.now();
        Payment overduePending = savePayment("PENDING", cutoff.minusMinutes(1));
        Payment pendingWithoutExpiry = savePayment("PENDING", null);
        Payment futurePending = savePayment("PENDING", cutoff.plusMinutes(1));
        Payment paid = savePayment("PAID", cutoff.minusMinutes(1));
        Payment failed = savePayment("FAILED", cutoff.minusMinutes(1));

        int expiredCount = paymentExpiryService.expirePendingPaymentsBefore(cutoff);

        assertEquals(2, expiredCount);
        assertEquals("EXPIRED", findPayment(overduePending.getId()).getStatus());
        assertEquals("EXPIRED", findPayment(pendingWithoutExpiry.getId()).getStatus());
        assertEquals("PENDING", findPayment(futurePending.getId()).getStatus());
        assertEquals("PAID", findPayment(paid.getId()).getStatus());
        assertEquals("FAILED", findPayment(failed.getId()).getStatus());
    }

    @Test
    void retriesWhenAnOrderCodeUniqueConstraintCollides() {
        String collidedCode = "JLPP" + UUID.randomUUID().toString().replace("-", "");
        String retryCode = "JLPP" + UUID.randomUUID().toString().replace("-", "");
        paymentRepository.saveAndFlush(Payment.builder()
                .orderCode(collidedCode)
                .user(user)
                .course(course)
                .amount(new BigDecimal("250000"))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("FAILED")
                .transferContent(collidedCode)
                .build());
        doReturn(collidedCode, retryCode).when(orderCodeGenerator).generateOrderCode();

        PaymentResponseDTO payment = createPayment();

        verify(orderCodeGenerator, times(2)).generateOrderCode();
        assertNotEquals(collidedCode, payment.getOrderCode());
        assertEquals(retryCode, payment.getOrderCode());
        assertTrue(payment.getOrderCode().matches("JLPP[0-9a-f]{32}"));
        assertEquals(payment.getOrderCode(), payment.getTransferContent());
    }

    @Test
    void stopsAfterTheMaximumNumberOfOrderCodeCollisions() {
        String collidedCode = "JLPP" + UUID.randomUUID().toString().replace("-", "");
        paymentRepository.saveAndFlush(Payment.builder()
                .orderCode(collidedCode)
                .user(user)
                .course(course)
                .amount(new BigDecimal("250000"))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("FAILED")
                .transferContent(collidedCode)
                .build());
        doReturn(collidedCode).when(orderCodeGenerator).generateOrderCode();

        assertThrows(IllegalStateException.class, this::createPayment);

        verify(orderCodeGenerator, times(5)).generateOrderCode();
        assertEquals(1, paymentRepository.findAll().stream()
                .filter(payment -> payment.getUser().getId().equals(user.getId())
                        && payment.getCourse().getId().equals(course.getId()))
                .count());
    }

    @Test
    void paymentCreationUsesIndependentTransactionWhenCallerIsRollbackOnly() {
        String collidedCode = "JLPP" + UUID.randomUUID().toString().replace("-", "");
        String retryCode = "JLPP" + UUID.randomUUID().toString().replace("-", "");
        paymentRepository.saveAndFlush(Payment.builder()
                .orderCode(collidedCode)
                .user(user)
                .course(course)
                .amount(new BigDecimal("250000"))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status("FAILED")
                .transferContent(collidedCode)
                .build());
        doReturn(collidedCode, retryCode).when(orderCodeGenerator).generateOrderCode();

        CourseRegistration registration = courseRegistrationService.createPendingRegistration(user.getId(),
                CourseRegistrationRequest.builder().courseId(course.getId()).paymentMethod("SEPAY").build());
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .courseId(course.getId())
                .registrationId(registration.getId())
                .paymentMethod("SEPAY")
                .build();
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);
        PaymentResponseDTO payment = outerTransaction.execute(status -> {
            status.setRollbackOnly();
            return paymentService.createPayment(user.getId(), request);
        });

        assertNotNull(payment);
        assertEquals(retryCode, payment.getOrderCode());
        verify(orderCodeGenerator, times(2)).generateOrderCode();
        assertTrue(paymentRepository.existsByOrderCode(payment.getOrderCode()));
    }

    @Test
    void doesNotRetryForNonUniqueDatabaseIntegrityErrors() {
        doThrow(new DataIntegrityViolationException("Forced non-unique database failure"))
                .when(paymentRepository).save(any(Payment.class));

        assertThrows(DataIntegrityViolationException.class, this::createPayment);

        verify(orderCodeGenerator, times(1)).generateOrderCode();
    }

    @Test
    void onlyPubliclyAvailableCourseStatusesCanBePurchased() {
        course.setStatus("DRAFT");
        courseRepository.saveAndFlush(course);
        assertThrows(IllegalStateException.class, this::createPayment);

        course.setStatus("ARCHIVED");
        courseRepository.saveAndFlush(course);
        assertThrows(IllegalStateException.class, this::createPayment);

        course.setStatus("PUBLISHED");
        courseRepository.saveAndFlush(course);
        assertDoesNotThrow(() -> {
            createPayment();
        });
        assertThrows(RuntimeException.class, () -> createPayment(Long.MAX_VALUE));
    }

    @Test
    void refusesPaymentForPaidOrRegisteredCourse() {
        PaymentResponseDTO paid = createPayment();
        setPaymentState(paid.getId(), "PAID", LocalDateTime.now().plusMinutes(10));

        assertThrows(IllegalStateException.class, this::createPayment);

        paymentRepository.delete(findPayment(paid.getId()));
        CourseRegistration registration = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), course.getId()).orElseThrow();
        registration.setStatus(CourseRegistration.STATUS_ACTIVE);
        courseRegistrationRepository.saveAndFlush(registration);

        assertThrows(IllegalStateException.class, this::createPayment);
    }

    @Test
    void onlyActiveAndCompletedRegistrationsAreEnrollmentEligible() {
        CourseRegistration registration = courseRegistrationRepository.saveAndFlush(CourseRegistration.builder()
                .user(user)
                .course(course)
                .status(CourseRegistration.STATUS_ACTIVE)
                .build());

        assertTrue(courseRegistrationService.isUserEnrolled(user.getId(), course.getId()));
        for (String ineligibleStatus : new String[] {
                CourseRegistration.STATUS_PENDING_PAYMENT,
                CourseRegistration.STATUS_CANCELLED,
                CourseRegistration.STATUS_EXPIRED,
                "UNKNOWN"
        }) {
            registration.setStatus(ineligibleStatus);
            courseRegistrationRepository.saveAndFlush(registration);
            assertFalse(courseRegistrationService.isUserEnrolled(user.getId(), course.getId()), ineligibleStatus);
        }

        registration.setStatus(CourseRegistration.STATUS_COMPLETED);
        courseRegistrationRepository.saveAndFlush(registration);
        assertTrue(courseRegistrationService.isUserEnrolled(user.getId(), course.getId()));
    }

    @Test
    void concurrentFreeCourseAccessCreatesOnlyOneActiveRegistration() throws Exception {
        Course freeCourse = courseRepository.saveAndFlush(Course.builder()
                .id(COURSE_IDS.incrementAndGet())
                .title("Free Payment Test Course")
                .level("N5")
                .status("PUBLISHED")
                .price(0.0)
                .enrollmentCount(0)
                .build());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<CourseRegistration> first = executor.submit(() ->
                    ensureFreeRegistrationAtSameTime(freeCourse.getId(), ready, start));
            Future<CourseRegistration> second = executor.submit(() ->
                    ensureFreeRegistrationAtSameTime(freeCourse.getId(), ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertEquals(first.get(10, TimeUnit.SECONDS).getId(), second.get(10, TimeUnit.SECONDS).getId());
            assertEquals(1, courseRegistrationRepository.countByCourseId(freeCourse.getId()));
            assertEquals(CourseRegistration.STATUS_ACTIVE,
                    courseRegistrationRepository.findByUserIdAndCourseId(user.getId(), freeCourse.getId())
                            .orElseThrow().getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void pendingRegistrationCannotAccessLessonsOrCompleteProgress() throws Exception {
        CourseRegistration pending = courseRegistrationService.createPendingRegistration(user.getId(),
                CourseRegistrationRequest.builder().courseId(course.getId()).build());
        Lesson lesson = lessonRepository.saveAndFlush(Lesson.builder()
                .course(course)
                .title("Payment-gated lesson")
                .orderIndex(1)
                .build());

        mockMvc.perform(get("/learning/courses/{courseId}/lessons/{lessonId}", course.getId(), lesson.getId())
                        .with(user(user.getEmail()).roles("LEARNER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/courses/" + course.getId() + "/register"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/learning/lessons/{lessonId}/complete", lesson.getId())
                        .with(user(user.getEmail()).roles("LEARNER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learning/courses/" + course.getId() + "/register"));
        assertEquals(CourseRegistration.STATUS_PENDING_PAYMENT, pending.getStatus());
        assertEquals(0, courseRegistrationRepository.findByUserIdAndCourseId(user.getId(), course.getId())
                .orElseThrow().getProgressPercent());
    }

    @Test
    void secondTransactionIdForPaidOrderIsRecordedForReconciliationWithoutAnotherInvoice() {
        PaymentResponseDTO payment = createPayment();
        String firstId = "TX-PAID-" + UUID.randomUUID();
        String secondId = "TX-PAID-" + UUID.randomUUID();

        assertEquals(SePayWebhookResult.Outcome.PROCESSED,
                paymentService.processSePayWebhook(webhookPayload(payment, firstId)).outcome());
        SePayWebhookResult duplicate = paymentService.processSePayWebhook(webhookPayload(payment, secondId));

        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED, duplicate.outcome());
        assertEquals(2, paymentTransactionRepository.countByPayment_Id(payment.getId()));
        assertEquals(1, courseRegistrationRepository.countByCourseId(course.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
        assertEquals(firstId, paymentService.getInvoice(user.getId(), payment.getId()).getTransactionId());
        assertEquals(CourseRegistration.STATUS_ACTIVE,
                paymentService.getInvoice(user.getId(), payment.getId()).getRegistrationStatus());
    }

    @Test
    void lateSettlementIsReconciledWithoutSupersedingTheCurrentOrder() {
        PaymentResponseDTO first = createPayment();
        setPaymentState(first.getId(), "PENDING", LocalDateTime.now().minusMinutes(1));
        PaymentResponseDTO replacement = createPayment();
        assertNotEquals(first.getId(), replacement.getId());

        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED,
                paymentService.processSePayWebhook(webhookPayload(first, "TX-LATE-OLD-" + UUID.randomUUID()))
                        .outcome());
        assertEquals("RECONCILIATION_REQUIRED", findPayment(first.getId()).getStatus());
        assertEquals("PENDING", findPayment(replacement.getId()).getStatus());
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED,
                paymentService.processSePayWebhook(
                        webhookPayload(replacement, "TX-CURRENT-" + UUID.randomUUID())).outcome());

        String secondTransactionId = "TX-LATE-SECOND-" + UUID.randomUUID();
        SePayWebhookResult reconciliation = paymentService.processSePayWebhook(
                webhookPayload(first, secondTransactionId));

        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED, reconciliation.outcome());
        assertEquals("PAID", findPayment(replacement.getId()).getStatus());
        assertEquals("RECONCILIATION_REQUIRED", findPayment(first.getId()).getStatus());
        assertTrue(paymentTransactionRepository.existsByGatewayTransactionId(secondTransactionId));
        assertEquals(2, paymentTransactionRepository.countByPayment_Id(first.getId()));
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(replacement.getId()));
        assertEquals(1, courseRegistrationRepository.countByCourseId(course.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
        assertThrows(IllegalStateException.class,
                () -> paymentService.getInvoice(user.getId(), first.getId()));
        assertEquals(replacement.getId(),
                paymentService.getInvoice(user.getId(), replacement.getId()).getPaymentId());
    }

    @Test
    void checkoutRequiresLoginAndReturnsToTheSelectedCourse() throws Exception {
        mockMvc.perform(get("/checkout/{courseId}", course.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?continue=/courses"));
    }

    @Test
    void checkoutStopsWhenTheLearnerIsAlreadyEnrolled() throws Exception {
        courseRegistrationRepository.saveAndFlush(CourseRegistration.builder()
                .user(user)
                .course(course)
                .status("ACTIVE")
                .build());

        mockMvc.perform(get("/checkout/{courseId}", course.getId())
                        .with(user(user.getEmail()).roles("LEARNER")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + course.getId()));
    }

    @Test
    void checkoutCreatesPaymentOnlyAfterExplicitConfirmation() throws Exception {
        CourseRegistration registration = courseRegistrationService.createPendingRegistration(user.getId(),
                CourseRegistrationRequest.builder()
                        .courseId(course.getId())
                        .paymentMethod("SEPAY")
                        .build());

        mockMvc.perform(get("/checkout/{courseId}", course.getId())
                        .param("registrationId", registration.getId().toString())
                        .with(user(user.getEmail()).roles("LEARNER")))
                .andExpect(status().isOk());

        assertTrue(paymentRepository.findByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
                user.getId(), course.getId(), "PENDING").isEmpty());

        mockMvc.perform(post("/checkout/{courseId}", course.getId())
                        .with(user(user.getEmail()).roles("LEARNER"))
                        .with(csrf())
                        .param("registrationId", registration.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/payment/*"));

        assertEquals(1, paymentRepository.findByUser_IdAndCourse_IdAndStatusOrderByCreatedAtDesc(
                user.getId(), course.getId(), "PENDING").size());
    }

    @Test
    void concurrentPaymentRequestsReuseOnePendingPayment() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<PaymentResponseDTO> first = executor.submit(() -> createAtSameTime(ready, start));
            Future<PaymentResponseDTO> second = executor.submit(() -> createAtSameTime(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            PaymentResponseDTO firstResult = first.get(10, TimeUnit.SECONDS);
            PaymentResponseDTO secondResult = second.get(10, TimeUnit.SECONDS);

            assertEquals(firstResult.getId(), secondResult.getId());
            assertEquals(1, paymentRepository.findAll().stream()
                    .filter(payment -> payment.getUser().getId().equals(user.getId())
                            && payment.getCourse().getId().equals(course.getId()))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRequestsForDifferentCoursesOfSameUserBothComplete() throws Exception {
        Course secondCourse = courseRepository.saveAndFlush(Course.builder()
                .id(COURSE_IDS.incrementAndGet())
                .title("Second Payment Test Course")
                .level("N3")
                .status("PUBLISHED")
                .price(350000.0)
                .build());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<PaymentResponseDTO> first = executor.submit(() -> createAtSameTime(course.getId(), ready, start));
            Future<PaymentResponseDTO> second = executor.submit(() -> createAtSameTime(secondCourse.getId(), ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            assertNotEquals(first.get(10, TimeUnit.SECONDS).getId(), second.get(10, TimeUnit.SECONDS).getId());
            assertEquals(2, paymentRepository.findAll().stream()
                    .filter(payment -> payment.getUser().getId().equals(user.getId()))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentPaymentCreationAndWebhookRemainConsistent() throws Exception {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> payload = webhookPayload(pending, "TX-RACE-" + UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> webhook = executor.submit(() -> processWebhookAtSameTime(payload, ready, start));
            Future<PaymentResponseDTO> creation = executor.submit(() -> createAtSameTime(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            webhook.get(10, TimeUnit.SECONDS);
            try {
                creation.get(10, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException exception) {
                assertInstanceOf(IllegalStateException.class, exception.getCause());
            }

            assertEquals("PAID", findPayment(pending.getId()).getStatus());
            assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
            assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
            assertEquals(1, paymentRepository.findAll().stream()
                    .filter(payment -> payment.getUser().getId().equals(user.getId())
                            && payment.getCourse().getId().equals(course.getId()))
                    .count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void identicalConcurrentWebhooksCreateOneTransactionAndRegistration() throws Exception {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> payload = webhookPayload(pending, "TX-DUP-" + UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> processWebhookAtSameTime(payload, ready, start));
            Future<?> second = executor.submit(() -> processWebhookAtSameTime(payload, ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            assertEquals("PAID", findPayment(pending.getId()).getStatus());
            assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
            assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void latePaymentRacingWithRetryIsReconciledWithoutGrantingAccess() throws Exception {
        PaymentResponseDTO pending = createPayment();
        setPaymentState(pending.getId(), "PENDING", LocalDateTime.now().minusNanos(1_000_000));
        Map<String, Object> payload = webhookPayload(pending, "TX-EXP-" + UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> webhook = executor.submit(() -> processWebhookAtSameTime(payload, ready, start));
            Future<PaymentResponseDTO> replacement =
                    executor.submit(() -> createAtSameTime(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            webhook.get(10, TimeUnit.SECONDS);
            PaymentResponseDTO replacementPayment = replacement.get(10, TimeUnit.SECONDS);

            assertNotEquals(pending.getId(), replacementPayment.getId());
            assertEquals("RECONCILIATION_REQUIRED", findPayment(pending.getId()).getStatus());
            assertEquals("PENDING", findPayment(replacementPayment.getId()).getStatus());
            assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
            assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedPaymentWebhookIsRejectedWithoutTransactionOrRegistration() {
        assertRejectedTerminalPaymentWebhook("FAILED");
    }

    @Test
    void cancelledPaymentWebhookIsRejectedWithoutTransactionOrRegistration() {
        assertRejectedTerminalPaymentWebhook("CANCELLED");
    }

    @Test
    void validWebhookForExpiredPaymentIsRecordedForReconciliationWithoutGrantingAccess() {
        PaymentResponseDTO expired = createPayment();
        setPaymentState(expired.getId(), "PENDING", LocalDateTime.now().minusMinutes(1));
        paymentExpiryService.expirePendingPaymentsBefore(LocalDateTime.now());
        assertEquals("EXPIRED", findPayment(expired.getId()).getStatus());
        String transactionId = "TX-LATE-" + UUID.randomUUID();
        Map<String, Object> payload = webhookPayload(expired, transactionId);

        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED,
                paymentService.processSePayWebhook(payload).outcome());

        assertEquals("RECONCILIATION_REQUIRED", findPayment(expired.getId()).getStatus());
        assertTrue(paymentTransactionRepository.existsByGatewayTransactionId(transactionId));
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(expired.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
        assertTrue(paymentWebhookLogRepository.findAll().stream()
                .anyMatch(log -> transactionId.equals(log.getGatewayTransactionId())
                        && "RECONCILIATION_REQUIRED".equals(log.getProcessingStatus())));
    }

    @Test
    void webhookRacingWithExpirySweepReconcilesWithoutGrantingAccess() throws Exception {
        PaymentResponseDTO pending = createPayment();
        setPaymentState(pending.getId(), "PENDING", LocalDateTime.now().minusMinutes(1));
        Map<String, Object> payload = webhookPayload(pending, "TX-EXPIRY-RACE-" + UUID.randomUUID());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> webhook = executor.submit(() -> processWebhookAtSameTime(payload, ready, start));
            Future<Integer> expiry = executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start expiry sweep.");
                }
                return paymentExpiryService.expirePendingPaymentsBefore(LocalDateTime.now());
            });

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            webhook.get(10, TimeUnit.SECONDS);
            expiry.get(10, TimeUnit.SECONDS);

            assertEquals("RECONCILIATION_REQUIRED", findPayment(pending.getId()).getStatus());
            assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
            assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void mapsSePayPayloadFieldsAndRejectsTestIdZero() {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> payload = Map.of(
                "id", 0,
                "gateway", "Vietcombank",
                "transactionDate", "2026-11-15 02:00:00",
                "accountNumber", "123456789",
                "content", pending.getTransferContent(),
                "transferType", "in",
                "transferAmount", pending.getAmount(),
                "referenceCode", "REF-ZERO");

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.INVALID_TRANSACTION_ID, result.outcome());
        assertEquals("PENDING", findPayment(pending.getId()).getStatus());
        assertEquals(0, paymentTransactionRepository.countByPayment_Id(pending.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
    }

    @Test
    void mapsRealSePayPayloadNamesToPaymentTransaction() throws Exception {
        PaymentResponseDTO pending = createPayment();
        String gatewayTransactionId = Long.toString(SEPAY_TRANSACTION_IDS.incrementAndGet());
        Map<String, Object> payload = Map.of(
                "id", Long.parseLong(gatewayTransactionId),
                "gateway", "Vietcombank",
                "transactionDate", "2026-11-15 02:00:00",
                "accountNumber", "123456789",
                "content", pending.getTransferContent(),
                "transferType", "in",
                "transferAmount", pending.getAmount(),
                "referenceCode", "FT24012345678");

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED, result.outcome());
        var transaction = paymentTransactionRepository
                .findByGatewayTransactionId(gatewayTransactionId).orElseThrow();
        assertEquals("Vietcombank", transaction.getBankBrandName());
        assertEquals("****6789", transaction.getBankAccountNumber());
        assertEquals("123456789", accountNumberCryptoService.decrypt(
                readStoredJson(transaction.getWebhookPayload()).path("accountNumber").asText()));
        assertEquals("123456789", paymentReconciliationService.decryptReceiverAccountNumber(transaction));
        assertEquals(pending.getTransferContent(), transaction.getTransactionContent());
        assertEquals(0, pending.getAmount().compareTo(transaction.getAmountIn()));
        assertEquals("FT24012345678", transaction.getReferenceCode());
        assertEquals(LocalDateTime.of(2026, 11, 15, 2, 0), transaction.getTransactionDate());

        InvoiceResponseDTO invoice = paymentService.getInvoice(user.getId(), pending.getId());
        assertEquals(gatewayTransactionId, invoice.getTransactionId());
        assertEquals("FT24012345678", invoice.getReferenceCode());
        assertEquals(CourseRegistration.STATUS_ACTIVE, invoice.getRegistrationStatus());
    }

    @Test
    void extractsOrderCodeWithUppercasePrefixWithoutLowercasingStoredSuffix() {
        PaymentResponseDTO pending = createPayment();
        String transactionId = "TX-LOWERCASE-ORDER-" + UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(webhookPayload(pending, transactionId));
        payload.put("content", "jlpp" + pending.getOrderCode().substring(4));

        assertEquals(SePayWebhookResult.Outcome.PROCESSED,
                paymentService.processSePayWebhook(payload).outcome());
        assertEquals("PAID", findPayment(pending.getId()).getStatus());
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
    }

    @Test
    void matchesFullOrderCodeWhenBankAddsTransferText() {
        PaymentResponseDTO pending = createPayment();
        String transactionId = "TX-BANK-SUFFIX-" + UUID.randomUUID();
        String transferContent = pending.getTransferContent() + " hWU2ET9X/171157";
        Map<String, Object> payload = new HashMap<>(webhookPayload(pending, transactionId));
        payload.put("content", transferContent);

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED, result.outcome());
        assertEquals("PAID", findPayment(pending.getId()).getStatus());
        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewayTransactionId(transactionId).orElseThrow();
        assertEquals(transferContent, transaction.getTransactionContent());
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
        verify(courseRegistrationRepository, times(2)).save(any(CourseRegistration.class));
        assertEquals(transactionId, paymentService.getInvoice(user.getId(), pending.getId()).getTransactionId());
    }

    @Test
    void doesNotMatchOrderCodeWhenBankTextIsNotSeparatedFromTheFullCode() {
        PaymentResponseDTO pending = createPayment();
        String transactionId = "TX-UNSAFE-SUFFIX-" + UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(webhookPayload(pending, transactionId));
        payload.put("content", pending.getTransferContent() + "X");

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.PAYMENT_NOT_FOUND, result.outcome());
        assertEquals("PENDING", findPayment(pending.getId()).getStatus());
        assertFalse(paymentTransactionRepository.existsByGatewayTransactionId(transactionId));
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
    }

    @Test
    void invoiceIsUnavailableUntilPaidAndCannotBeReadByAnotherUser() {
        PaymentResponseDTO pending = createPayment();
        User otherUser = userRepository.saveAndFlush(User.builder()
                .email("payment-invoice-" + UUID.randomUUID() + "@example.test")
                .username("payment-invoice-" + UUID.randomUUID())
                .fullName("Other Learner")
                .password("unused")
                .role(RoleEnum.LEARNER)
                .status("ACTIVE")
                .build());

        assertThrows(IllegalStateException.class,
                () -> paymentService.getInvoice(user.getId(), pending.getId()));
        assertThrows(SecurityException.class,
                () -> paymentService.getInvoice(otherUser.getId(), pending.getId()));
        assertThrows(SecurityException.class,
                () -> paymentService.getInvoice(user.getId(), Long.MAX_VALUE));
    }

    @Test
    void successfulPaymentAndInvoicePagesShowActivatedAccessAndTransactionDetails() throws Exception {
        PaymentResponseDTO payment = createPayment();
        String transactionId = "TX-INVOICE-" + UUID.randomUUID();
        String referenceCode = "REF-" + UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(webhookPayload(payment, transactionId));
        payload.put("referenceCode", referenceCode);
        assertEquals(SePayWebhookResult.Outcome.PROCESSED,
                paymentService.processSePayWebhook(payload).outcome());

        mockMvc.perform(get("/payment/{paymentId}", payment.getId())
                        .with(user(user.getEmail()).roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Thanh toán thành công. Đăng ký khóa học thành công, quyền học đã được kích hoạt.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vào học ngay")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem hóa đơn")));

        mockMvc.perform(get("/invoice/{paymentId}", payment.getId())
                        .with(user(user.getEmail()).roles("LEARNER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(payment.getOrderCode())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(transactionId)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(referenceCode)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(course.getTitle())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(user.getEmail())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Vào học ngay")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xem đơn hàng")));
    }

    @Test
    void encryptsAccountNumberInPersistedWebhookPayloadsWithoutChangingBusinessPayload() throws Exception {
        PaymentResponseDTO pending = createPayment();
        String transactionId = "TX-ENCRYPTED-" + UUID.randomUUID();
        Map<String, Object> payload = webhookPayload(pending, transactionId);

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED, result.outcome());
        assertEquals("123456789", payload.get("bankAccountNumber"));

        PaymentTransaction transaction = paymentTransactionRepository
                .findByGatewayTransactionId(transactionId).orElseThrow();
        assertEquals("****6789", transaction.getBankAccountNumber());
        JsonNode transactionPayload = readStoredJson(transaction.getWebhookPayload());
        String encryptedTransactionAccount = transactionPayload.path("bankAccountNumber").asText();
        assertTrue(encryptedTransactionAccount.startsWith("v1."));
        assertFalse(objectMapper.writeValueAsString(transaction.getWebhookPayload()).contains("123456789"));
        assertEquals("123456789", accountNumberCryptoService.decrypt(encryptedTransactionAccount));

        PaymentWebhookLog log = paymentWebhookLogRepository.findAll().stream()
                .filter(item -> transactionId.equals(item.getGatewayTransactionId()))
                .findFirst()
                .orElseThrow();
        JsonNode requestPayload = readStoredJson(log.getRequestPayload());
        String encryptedLogAccount = requestPayload.path("bankAccountNumber").asText();
        assertTrue(encryptedLogAccount.startsWith("v1."));
        assertFalse(objectMapper.writeValueAsString(log.getRequestPayload()).contains("123456789"));
        assertEquals("123456789", accountNumberCryptoService.decrypt(encryptedLogAccount));
        assertNotEquals(encryptedTransactionAccount, encryptedLogAccount);
        assertTrue(paymentTransactionRepository.existsByGatewayTransactionId(transactionId));
        assertEquals("PAID", findPayment(pending.getId()).getStatus());
    }

    @Test
    void malformedAccountFieldIsRedactedAndCannotConfirmPayment() throws Exception {
        PaymentResponseDTO pending = createPayment();
        String transactionId = "TX-MALFORMED-ACCOUNT-" + UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>(webhookPayload(pending, transactionId));
        payload.put("accountNumber", Map.of("value", "123456789"));

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.ACCOUNT_MISMATCH, result.outcome());
        PaymentWebhookLog log = paymentWebhookLogRepository.findAll().stream()
                .filter(item -> transactionId.equals(item.getGatewayTransactionId()))
                .findFirst()
                .orElseThrow();
        JsonNode requestPayload = readStoredJson(log.getRequestPayload());
        assertEquals("[REDACTED_INVALID_ACCOUNT_NUMBER]",
                requestPayload.path("accountNumber").asText());
        assertEquals("123456789", accountNumberCryptoService.decrypt(
                requestPayload.path("bankAccountNumber").asText()));
        assertEquals("PENDING", findPayment(pending.getId()).getStatus());
        assertEquals(0, paymentTransactionRepository.countByPayment_Id(pending.getId()));
    }

    @Test
    void rejectsWebhookWithoutValidTransactionDate() {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> payload = new HashMap<>(webhookPayload(
                pending, "TX-MISSING-DATE-" + UUID.randomUUID()));
        payload.remove("transactionDate");

        SePayWebhookResult result = paymentService.processSePayWebhook(payload);

        assertEquals(SePayWebhookResult.Outcome.INVALID_TRANSACTION_DATE, result.outcome());
        assertEquals("PENDING", findPayment(pending.getId()).getStatus());
        assertEquals(0, paymentTransactionRepository.countByPayment_Id(pending.getId()));
    }

    @Test
    void paymentResponseStillUsesConfiguredAccountAndVietQrDetailsWithoutCryptoMaterial() throws Exception {
        PaymentResponseDTO payment = createPayment();

        assertEquals("123456789", payment.getBankAccountNumber());
        assertTrue(payment.getQrCodeUrl().contains("MBBank-123456789-compact2.png"));
        assertTrue(payment.getQrCodeUrl().contains("amount=" + payment.getAmount().toPlainString()));
        assertTrue(payment.getQrCodeUrl().contains(
                "addInfo=" + URLEncoder.encode(payment.getTransferContent(), StandardCharsets.UTF_8)));

        String responseJson = objectMapper.writeValueAsString(payment);
        assertFalse(responseJson.contains(TEST_WEBHOOK_SECRET));
        assertFalse(responseJson.contains("v1."));
    }

    @Test
    void usesDiscountPriceAndKeepsThePaymentAmountAsASnapshot() {
        course.setDiscountPrice(175000.0);
        courseRepository.saveAndFlush(course);

        PaymentResponseDTO payment = createPayment();

        assertEquals(0, new BigDecimal("175000").compareTo(payment.getAmount()));
        assertEquals(0, new BigDecimal("175000").compareTo(findPayment(payment.getId()).getAmount()));

        course.setDiscountPrice(100000.0);
        courseRepository.saveAndFlush(course);
        assertEquals(0, new BigDecimal("175000").compareTo(
                paymentService.getPaymentForUser(user.getId(), payment.getId()).getAmount()));
    }

    @Test
    void checkoutUsesCurrentPriceAndSupersedesAnUnexpiredPaymentWithAnOldAmount() {
        PaymentResponseDTO oldPayment = createPayment();
        course.setDiscountPrice(100000.0);
        courseRepository.saveAndFlush(course);

        PaymentResponseDTO currentPayment = createPayment();

        assertEquals(0, new BigDecimal("100000").compareTo(currentPayment.getAmount()));
        assertEquals("SUPERSEDED", findPayment(oldPayment.getId()).getStatus());
        assertEquals("PENDING", findPayment(currentPayment.getId()).getStatus());
        assertNotEquals(oldPayment.getId(), currentPayment.getId());

        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED,
                paymentService.processSePayWebhook(webhookPayload(
                        oldPayment, "TX-SUPERSEDED-" + UUID.randomUUID())).outcome());
        assertEquals("RECONCILIATION_REQUIRED", findPayment(oldPayment.getId()).getStatus());
        assertEquals("PENDING", findPayment(currentPayment.getId()).getStatus());
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
    }

    @Test
    void mismatchedWebhookDetailsDoNotConfirmPaymentOrGrantAccess() {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> validPayload = webhookPayload(pending, "TX-VALIDATION-" + UUID.randomUUID());
        Map<String, Object> wrongAmount = new HashMap<>(validPayload);
        wrongAmount.put("transactionId", "TX-WRONG-AMOUNT-" + UUID.randomUUID());
        wrongAmount.put("transferAmount", pending.getAmount().add(BigDecimal.ONE));
        Map<String, Object> wrongAccount = new HashMap<>(validPayload);
        wrongAccount.put("transactionId", "TX-WRONG-ACCOUNT-" + UUID.randomUUID());
        wrongAccount.put("bankAccountNumber", "000000000");
        Map<String, Object> outgoing = new HashMap<>(validPayload);
        outgoing.put("transactionId", "TX-OUT-" + UUID.randomUUID());
        outgoing.put("direction", "OUT");
        Map<String, Object> wrongContent = new HashMap<>(validPayload);
        wrongContent.put("transactionId", "TX-WRONG-CONTENT-" + UUID.randomUUID());
        wrongContent.put("content", "UNRELATED-CONTENT");
        Map<String, Object> missingTransactionId = new HashMap<>(validPayload);
        missingTransactionId.put("transactionId", null);

        paymentService.processSePayWebhook(wrongAccount);
        paymentService.processSePayWebhook(outgoing);
        paymentService.processSePayWebhook(wrongContent);
        paymentService.processSePayWebhook(missingTransactionId);
        assertEquals(SePayWebhookResult.Outcome.RECONCILIATION_REQUIRED,
                paymentService.processSePayWebhook(wrongAmount).outcome());

        assertEquals("RECONCILIATION_REQUIRED", findPayment(pending.getId()).getStatus());
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
    }

    @Test
    void repeatedWebhookDoesNotDuplicateTransactionOrRegistration() {
        PaymentResponseDTO payment = createPayment();
        String transactionId = "TX-" + UUID.randomUUID();
        Map<String, Object> payload = webhookPayload(payment, transactionId);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED,
                paymentService.processSePayWebhook(payload).outcome());
        assertEquals(SePayWebhookResult.Outcome.DUPLICATE,
                paymentService.processSePayWebhook(payload).outcome());

        assertTrue(paymentTransactionRepository.existsByGatewayTransactionId(transactionId));
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(payment.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
        assertEquals("PAID", findPayment(payment.getId()).getStatus());
        assertEquals(2, paymentWebhookLogRepository.findAll().stream()
                .filter(log -> transactionId.equals(log.getGatewayTransactionId()))
                .count());
        assertTrue(paymentWebhookLogRepository.findAll().stream()
                .anyMatch(log -> transactionId.equals(log.getGatewayTransactionId())
                        && "SUCCESS".equals(log.getProcessingStatus())));
        assertTrue(paymentWebhookLogRepository.findAll().stream()
                .anyMatch(log -> transactionId.equals(log.getGatewayTransactionId())
                        && "DUPLICATE".equals(log.getProcessingStatus())));
    }

    @Test
    void registrationWriteFailureRollsBackPaymentAndTransaction() {
        PaymentResponseDTO pending = createPayment();
        Map<String, Object> payload = webhookPayload(pending, "TX-ROLLBACK-" + UUID.randomUUID());
        doThrow(new DataIntegrityViolationException("Forced registration write failure"))
                .doCallRealMethod()
                .when(courseRegistrationService).completePaidRegistration(
                        eq(user.getId()),
                        eq(course.getId()),
                        eq(pending.getRegistrationId()),
                        eq(pending.getAmount()),
                        eq("SEPAY"));

        assertThrows(DataIntegrityViolationException.class,
                () -> paymentService.processSePayWebhook(payload));

        assertEquals("PENDING", findPayment(pending.getId()).getStatus());
        assertEquals(0, paymentTransactionRepository.countByPayment_Id(pending.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);

        assertEquals(SePayWebhookResult.Outcome.PROCESSED, paymentService.processSePayWebhook(payload).outcome());
        assertEquals("PAID", findPayment(pending.getId()).getStatus());
        assertEquals(1, paymentTransactionRepository.countByPayment_Id(pending.getId()));
        assertRegistrationStatus(CourseRegistration.STATUS_ACTIVE);
    }

    private PaymentResponseDTO createPayment() {
        return createPayment(course.getId());
    }

    private JsonNode readStoredJson(Map<String, Object> storedPayload) {
        return objectMapper.valueToTree(storedPayload);
    }

    private PaymentResponseDTO createPayment(Long targetCourseId) {
        CourseRegistration registration = courseRegistrationService.createPendingRegistration(user.getId(),
                CourseRegistrationRequest.builder()
                        .courseId(targetCourseId)
                        .paymentMethod("SEPAY")
                        .build());
        return paymentService.createPayment(user.getId(),
                PaymentRequestDTO.builder()
                        .courseId(targetCourseId)
                        .registrationId(registration.getId())
                        .paymentMethod("SEPAY")
                        .build());
    }

    private PaymentResponseDTO createAtSameTime(CountDownLatch ready, CountDownLatch start) throws Exception {
        return createAtSameTime(course.getId(), ready, start);
    }

    private PaymentResponseDTO createAtSameTime(Long targetCourseId, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent payment request.");
        }
        return createPayment(targetCourseId);
    }

    private CourseRegistration ensureFreeRegistrationAtSameTime(
            Long targetCourseId, CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting to start concurrent free enrollment.");
        }
        return courseRegistrationService.ensureFreeRegistrationForAccess(user.getId(), targetCourseId)
                .orElseThrow();
    }

    private void processWebhookAtSameTime(
            Map<String, Object> payload, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to start concurrent webhook.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start webhook.", exception);
        }
        paymentService.processSePayWebhook(payload);
    }

    private Map<String, Object> webhookPayload(PaymentResponseDTO payment, String transactionId) {
        return Map.of(
                "transactionId", transactionId,
                "content", payment.getTransferContent(),
                "amount", payment.getAmount(),
                "bankAccountNumber", "123456789",
                "transactionDate", "2026-11-15 02:00:00",
                "direction", "IN");
    }

    private void assertRejectedTerminalPaymentWebhook(String status) {
        PaymentResponseDTO payment = createPayment();
        setPaymentState(payment.getId(), status, LocalDateTime.now().plusMinutes(10));
        String transactionId = "TX-" + status + "-" + UUID.randomUUID();

        paymentService.processSePayWebhook(webhookPayload(payment, transactionId));

        assertEquals(status, findPayment(payment.getId()).getStatus());
        assertFalse(paymentTransactionRepository.existsByGatewayTransactionId(transactionId));
        assertRegistrationStatus(CourseRegistration.STATUS_PENDING_PAYMENT);
        assertTrue(paymentWebhookLogRepository.findAll().stream()
                .anyMatch(log -> transactionId.equals(log.getGatewayTransactionId())
                        && "REJECTED".equals(log.getProcessingStatus())));
    }

    private void assertRegistrationStatus(String expectedStatus) {
        CourseRegistration registration = courseRegistrationRepository
                .findByUserIdAndCourseId(user.getId(), course.getId()).orElseThrow();
        assertEquals(expectedStatus, registration.getStatus());
    }

    private void setPaymentState(Long paymentId, String status, LocalDateTime expiresAt) {
        Payment payment = findPayment(paymentId);
        payment.setStatus(status);
        payment.setExpiresAt(expiresAt);
        paymentRepository.saveAndFlush(payment);
    }

    private Payment findPayment(Long paymentId) {
        return paymentRepository.findByIdWithUserAndCourse(paymentId).orElseThrow();
    }

    private Payment savePayment(String status, LocalDateTime expiresAt) {
        String unique = UUID.randomUUID().toString();
        return paymentRepository.saveAndFlush(Payment.builder()
                .orderCode("TEST-" + unique)
                .user(user)
                .course(course)
                .amount(new BigDecimal("250000"))
                .currency("VND")
                .paymentMethod("SEPAY")
                .status(status)
                .transferContent("TEST-TRANSFER-" + unique)
                .expiresAt(expiresAt)
                .build());
    }
}
