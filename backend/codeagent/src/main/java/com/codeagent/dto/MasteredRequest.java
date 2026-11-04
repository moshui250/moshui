package com.codeagent.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标记错题掌握状态的请求体。
 */
@Data
public class MasteredRequest {

    /** true 表示已掌握，false 表示重新回到待复习。 */
    @NotNull(message = "mastered 不能为空")
    private Boolean mastered;
}
