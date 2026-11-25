package com.jeps.modules.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SepayWebhookRequestSizeFilter extends OncePerRequestFilter {

    public static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
    private static final byte[] TOO_LARGE_RESPONSE =
            "{\"success\":false,\"code\":\"PAYLOAD_TOO_LARGE\"}".getBytes(StandardCharsets.UTF_8);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }
        return !"POST".equals(request.getMethod())
                || !"/api/webhooks/sepay".equals(requestPath);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        byte[] rawBody = request.getInputStream().readNBytes(MAX_REQUEST_BODY_BYTES + 1);
        if (rawBody.length > MAX_REQUEST_BODY_BYTES) {
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getOutputStream().write(TOO_LARGE_RESPONSE);
            return;
        }

        filterChain.doFilter(new CachedBodyRequest(request, rawBody), response);
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] rawBody;

        private CachedBodyRequest(HttpServletRequest request, byte[] rawBody) {
            super(request);
            this.rawBody = rawBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(rawBody);
            return new ServletInputStream() {
                private ReadListener readListener;

                @Override
                public int read() throws IOException {
                    return input.read();
                }

                @Override
                public int read(byte[] buffer, int offset, int length) {
                    return input.read(buffer, offset, length);
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    if (readListener != null) {
                        throw new IllegalStateException("ReadListener has already been set.");
                    }
                    readListener = Objects.requireNonNull(listener, "listener");
                    try {
                        if (!isFinished()) {
                            readListener.onDataAvailable();
                        }
                        if (isFinished()) {
                            readListener.onAllDataRead();
                        }
                    } catch (IOException exception) {
                        readListener.onError(exception);
                    }
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.ISO_8859_1 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        @Override
        public int getContentLength() {
            return rawBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return rawBody.length;
        }
    }
}
