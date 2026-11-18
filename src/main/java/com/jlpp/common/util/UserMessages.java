package com.jlpp.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

public final class UserMessages {

    public static final String DATABASE_ERROR = "Không lưu được dữ liệu. Vui lòng thử lại hoặc báo quản trị viên.";

    private static final Logger log = LoggerFactory.getLogger(UserMessages.class);

    private UserMessages() {
    }

    /** Business errors carry a Vietnamese message for the user; database errors are logged and hidden. */
    public static String of(RuntimeException exception) {
        if (exception instanceof DataAccessException) {
            log.error("Database error", exception);
            return DATABASE_ERROR;
        }
        return exception.getMessage();
    }
}
