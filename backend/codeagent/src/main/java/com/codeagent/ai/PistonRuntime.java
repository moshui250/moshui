package com.codeagent.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Piston /runtimes 返回的单个运行时信息。
 *
 * @param language Piston 语言名，例如 c++、java
 * @param version  运行时真实版本
 * @param aliases  语言别名，例如 cpp、g++
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PistonRuntime(String language, String version, List<String> aliases) {
}
