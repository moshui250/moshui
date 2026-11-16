package com.codeagent.common;

import com.codeagent.dto.QuestionDetailRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionAccessTest {

    private static QuestionDetailRow question(String type, String visibility, Long ownerId) {
        QuestionDetailRow row = new QuestionDetailRow();
        row.setType(type);
        row.setVisibility(visibility);
        row.setUserId(ownerId);
        return row;
    }

    @Test
    void sharedQuestionIsReadableByOtherUsers() {
        assertTrue(QuestionAccess.canUseAi(question("PERSONAL", "SHARED", 1L), 2L));
    }

    @Test
    void publicQuestionIsReadableByOtherUsers() {
        assertTrue(QuestionAccess.canUseAi(question("PERSONAL", "PUBLIC", 1L), 2L));
        assertTrue(QuestionAccess.canUseAi(question("PUBLIC", "PUBLIC", 1L), 2L));
    }

    @Test
    void privateQuestionIsOnlyReadableByOwner() {
        assertTrue(QuestionAccess.canUseAi(question("PERSONAL", "PRIVATE", 1L), 1L));
        assertFalse(QuestionAccess.canUseAi(question("PERSONAL", "PRIVATE", 1L), 2L));
    }

    @Test
    void missingQuestionIsNotReadable() {
        assertFalse(QuestionAccess.canUseAi(null, 1L));
    }
}
