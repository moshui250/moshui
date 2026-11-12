package com.codeagent.config;

import com.codeagent.common.ErrorCode;
import com.codeagent.exception.BizException;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * go-judge 判题服务的配置。go-judge 不提供语言版本协商，语言工具链由镜像和 PATH 决定。
 */
@Data
@Component
@ConfigurationProperties(prefix = "gojudge")
public class GoJudgeProperties {

    private static final Pattern JAVA_XMX = Pattern.compile("\\d+[mMgG]");

    /** go-judge 根地址，默认本机 5050。 */
    private String baseUrl = "http://127.0.0.1:5050";

    /** 建立连接超时，单位毫秒。 */
    private int connectTimeoutMs = 5000;

    /** 读取响应超时，单位毫秒；需要覆盖编译和全部用例。 */
    private int readTimeoutMs = 35000;

    /** 单个用例运行 CPU 上限，单位毫秒。 */
    private int runTimeoutMs = 3000;

    /** 编译 CPU 上限，单位毫秒。 */
    private int compileTimeoutMs = 10000;

    /** 单次执行内存上限，单位字节。 */
    private long memoryLimit = 1073741824L;

    /** 进程数上限，防止 fork 炸弹。 */
    private int procLimit = 64;

    /** stdout/stderr 收集上限，单位字节。 */
    private int outputLimit = 1048576;

    /** 沙箱内工具链查找路径，需同时覆盖 g++ 和 javac。 */
    private String path = "/usr/local/java15/bin:/usr/local/bin:/usr/bin:/bin";

    /** 沙箱内 JAVA_HOME；官方镜像可覆盖为 openjdk-17 路径。 */
    private String javaHome = "/usr/local/java15";

    /** Java 堆上限，只能使用 m/g 后缀。 */
    private String javaXmx = "512m";

    /**
     * 按业务语言返回源码文件、编译命令和运行命令。
     *
     * @param code 业务语言，支持 CPP/C++/JAVA
     * @return 语言执行配置
     */
    public Language requireLanguage(String code) {
        String key = code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
        if ("c++".equals(key)) {
            key = "cpp";
        }
        String xmx = JAVA_XMX.matcher(javaXmx == null ? "" : javaXmx.trim()).matches()
                ? javaXmx.trim() : "512m";
        return switch (key) {
            case "cpp" -> new Language(
                    "Main.cpp",
                    "g++ -O2 -std=c++17 -pipe Main.cpp -o Main 2>compile.err",
                    "./Main");
            case "java" -> new Language(
                    "Main.java",
                    "javac -encoding UTF-8 Main.java 2>compile.err",
                    "java -Xmx" + xmx + " -Xss64m -Dfile.encoding=UTF-8 Main");
            default -> throw new BizException(ErrorCode.JUDGE_UNAVAILABLE, "暂不支持该判题语言");
        };
    }

    /**
     * 一个语言的源码文件、编译命令和运行命令。
     */
    public record Language(String sourceFile, String compileCommand, String runCommand) {
    }
}
