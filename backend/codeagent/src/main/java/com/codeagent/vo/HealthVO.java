package com.codeagent.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthVO implements Serializable {

    private String status;
    private String db;
    private LocalDateTime time;
}
