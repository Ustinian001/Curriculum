package com.example.curriculum.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 上课时间冲突检测工具
 *
 * schedule 约定格式：以「星期 + 开始节 - 结束节」表示一个时间段，多段用逗号分隔。
 * 例如："周一 1-2节,周三 5-6节" 表示周一第 1-2 节、周三第 5-6 节。
 * 同一学生在同一时间段不能有两门课，即两门课在同一星期且节次区间有交集即视为冲突。
 */
public class ScheduleUtil {

    /** 星期汉字 -> 数字（1~7） */
    private static final Map<Character, Integer> DAY_MAP = Map.of(
            '一', 1, '二', 2, '三', 3, '四', 4, '五', 5, '六', 6, '日', 7);

    /** 匹配 "周一 1-2节" 形式，提取星期与起止节次 */
    private static final Pattern SEG = Pattern.compile("周([一二三四五六日])\\s*([0-9]+)-([0-9]+)");

    /**
     * 判断两个 schedule 是否存在时间冲突
     *
     * @param a 课程 A 的上课时间
     * @param b 课程 B 的上课时间
     * @return true 表示冲突（同一天且节次区间重叠）
     */
    public static boolean isConflict(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return false;
        }
        List<int[]> x = parse(a);
        List<int[]> y = parse(b);
        for (int[] i : x) {
            for (int[] j : y) {
                // 同一天，且区间 [i1,i2] 与 [j1,j2] 有交集
                if (i[0] == j[0] && i[1] <= j[2] && j[1] <= i[2]) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 将 schedule 字符串解析为 {星期, 开始节, 结束节} 列表 */
    private static List<int[]> parse(String s) {
        List<int[]> list = new ArrayList<>();
        for (String seg : s.split("[,，]")) {
            Matcher m = SEG.matcher(seg.trim());
            if (m.find()) {
                int day = DAY_MAP.getOrDefault(m.group(1).charAt(0), 0);
                int start = Integer.parseInt(m.group(2));
                int end = Integer.parseInt(m.group(3));
                list.add(new int[]{day, start, end});
            }
        }
        return list;
    }
}
