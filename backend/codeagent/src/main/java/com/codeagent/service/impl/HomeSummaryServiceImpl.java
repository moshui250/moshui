package com.codeagent.service.impl;

import com.codeagent.common.UserContext;
import com.codeagent.dto.HomeDailyRow;
import com.codeagent.dto.HomeTotalsRow;
import com.codeagent.mapper.HomeMapper;
import com.codeagent.service.HomeSummaryService;
import com.codeagent.vo.HomeDailyVO;
import com.codeagent.vo.HomeSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页公开统计实现。
 *
 * <p>总览用一条聚合 SQL，周统计用一条按日期 GROUP BY 的 SQL；
 * 缺失的日期在内存中补 0，保证前端始终收到周一到周日七个点。</p>
 */
@Service
@RequiredArgsConstructor
public class HomeSummaryServiceImpl implements HomeSummaryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd");
    private static final String[] WEEK_LABELS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final HomeMapper homeMapper;

    /**
     * 获取首页总览。
     *
     * @return 首页总览响应
     */
    @Override
    public HomeSummaryVO getSummary() {
        HomeTotalsRow totals = homeMapper.selectTotals();
        long total = value(totals == null ? null : totals.getTotalSubmits());
        long ac = value(totals == null ? null : totals.getAcSubmits());

        HomeSummaryVO vo = new HomeSummaryVO();
        vo.setTotalSubmits(total);
        vo.setAcSubmits(ac);
        vo.setPassRate(total == 0 ? 0.0 : round((double) ac / total, 3));
        vo.setPublicQuestions(value(totals == null ? null : totals.getPublicQuestions()));
        vo.setKnowledgePoints(value(totals == null ? null : totals.getKnowledgePoints()));
        vo.setHasData(total > 0);

        Long userId = UserContext.getUserId();
        LocalDate monday = LocalDate.now(ZONE).with(DayOfWeek.MONDAY);
        List<HomeDailyRow> rows = userId == null
                ? homeMapper.selectWeek(monday, monday.plusDays(7))
                : homeMapper.selectWeekByUser(userId, monday, monday.plusDays(7));
        Map<LocalDate, HomeDailyRow> byDay = new HashMap<>();
        if (rows != null) {
            for (HomeDailyRow row : rows) {
                if (row.getDay() != null) {
                    byDay.put(row.getDay(), row);
                }
            }
        }

        List<HomeDailyVO> weekly = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            HomeDailyRow row = byDay.get(day);
            weekly.add(new HomeDailyVO(
                    day.format(DATE_FORMAT),
                    WEEK_LABELS[i],
                    row == null ? 0L : value(row.getSubmitCount()),
                    row == null ? 0L : value(row.getAcCount())
            ));
        }
        vo.setWeekly(weekly);
        return vo;
    }

    /**
     * 把可能为空的 Long 转成 0。
     *
     * @param value 原始值
     * @return 非空 long
     */
    private long value(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * 四舍五入到指定小数位。
     *
     * @param value 原始值
     * @param scale 保留小数位
     * @return 四舍五入后的值
     */
    private double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
