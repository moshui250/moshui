package com.codeagent.common;

/**
 * 刷题段位。AC 题数只统计不同题目的首次 AC，阈值之间刻意拉开差距。
 */
public enum RankTier {

    UNINITIALIZED(0, "未初始化", 0, 0, "#8a94a6"),
    BIT_APPRENTICE(1, "比特学徒", 1, 9, "#98a2b3"),
    BYTE_WALKER(2, "字节行者", 10, 29, "#2f6f52"),
    REGISTER_MONK(3, "寄存器修士", 30, 69, "#0f6b63"),
    CACHE_WALKER(4, "缓存行者", 70, 129, "#335f8f"),
    KERNEL_DEACON(5, "内核执事", 130, 249, "#5b4b8a"),
    INSTRUCTION_MASTER(6, "指令宗师", 250, 449, "#4a3f6b"),
    COMPUTE_LORD(7, "算力尊者", 450, 799, "#9a5a35"),
    SILICON_DAO(8, "硅基道主", 800, 1399, "#8f3f3f"),
    ORIGIN_CORE(9, "源始智核", 1400, Long.MAX_VALUE, "#8a6a2f");

    private final int code;
    private final String label;
    private final long minAc;
    private final long maxAc;
    private final String color;

    RankTier(int code, String label, long minAc, long maxAc, String color) {
        this.code = code;
        this.label = label;
        this.minAc = minAc;
        this.maxAc = maxAc;
        this.color = color;
    }

    /**
     * 根据全站唯一 AC 题数找到对应段位。
     *
     * @param acCount 唯一 AC 题数
     * @return 段位，0 题返回未初始化
     */
    public static RankTier of(long acCount) {
        if (acCount <= 0) {
            return UNINITIALIZED;
        }
        for (RankTier tier : values()) {
            if (acCount <= tier.maxAc) {
                return tier;
            }
        }
        return ORIGIN_CORE;
    }

    /**
     * 返回下一段位，最高段位返回 null。
     *
     * @return 下一段位
     */
    public RankTier next() {
        int next = ordinal() + 1;
        return next < values().length ? values()[next] : null;
    }

    /**
     * 段位对应的 AC 区间文案，用于规则弹窗。
     *
     * @return 区间文案
     */
    public String rangeText() {
        if (maxAc == 0) {
            return "0 题";
        }
        if (maxAc == Long.MAX_VALUE) {
            return minAc + " 题及以上";
        }
        return minAc + " - " + maxAc + " 题";
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public long getMinAc() {
        return minAc;
    }

    public long getMaxAc() {
        return maxAc;
    }

    public String getColor() {
        return color;
    }
}
