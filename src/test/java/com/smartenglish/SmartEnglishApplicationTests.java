package com.smartenglish;

import com.smartenglish.dto.response.PageResponse;
import com.smartenglish.dto.response.StudentSummaryResponse;
import com.smartenglish.service.AdminStudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class SmartEnglishApplicationTests {

    @Autowired
    private AdminStudentService adminStudentService;

    @Test
    void testGetStudentsPaged() {
        PageResponse<StudentSummaryResponse> result = adminStudentService.getStudentsPaged(
                null, null, null, 0, 10, "createdAt", "desc"
        );
        assertNotNull(result);
    }
}
