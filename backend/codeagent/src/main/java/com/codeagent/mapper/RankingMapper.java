package com.codeagent.mapper;

import com.codeagent.dto.RankingRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 总刷题排行榜查询。
 */
public interface RankingMapper {

    /**
     * 查询全站前 N 名。
     *
     * @param limit 返回条数
     * @return 按名次升序的榜单
     */
    List<RankingRow> selectTop(@Param("limit") int limit);

    /**
     * 查询指定用户的全站名次。
     *
     * @param userId 用户 ID
     * @return 用户排名行；用户不存在时返回 null
     */
    RankingRow selectUserRank(@Param("userId") Long userId);
}
