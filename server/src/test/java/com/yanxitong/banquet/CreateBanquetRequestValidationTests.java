package com.yanxitong.banquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanxitong.banquet.dto.CreateBanquetRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CreateBanquetRequestValidationTests {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void dateTimeAndLocationAreRequiredByBackend() {
        CreateBanquetRequest request = new CreateBanquetRequest();
        request.name = "婚宴";
        request.eventTypeCode = "WEDDING";

        var violations = validator.validate(request);

        assertEquals(2, violations.size());
        assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("banquetTime")));
        assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("location")));
    }

    @Test
    void completeCoreFieldsPassValidation() {
        CreateBanquetRequest request = new CreateBanquetRequest();
        request.name = "婚宴";
        request.eventTypeCode = "WEDDING";
        request.banquetTime = LocalDateTime.of(2026, 10, 1, 18, 0);
        request.location = "测试酒店宴会厅";

        assertTrue(validator.validate(request).isEmpty());
    }
}
