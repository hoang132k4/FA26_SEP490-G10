package com.jeps.modules.question.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** Chọn ngẫu nhiên câu hỏi từ ngân hàng; mỗi unit (câu đơn hoặc cả cụm bài đọc/nghe) được lấy nguyên. */
public final class QuestionPicker {

    private QuestionPicker() {}

    /** Subset-sum trên các unit đã xáo trộn: luôn tìm được tổ hợp đúng {@code target} câu nếu tồn tại. */
    public static Optional<List<Long>> pick(List<List<Long>> units, int target, Random random) {
        if (target < 0) return Optional.empty();
        List<List<Long>> shuffled = new ArrayList<>(units);
        Collections.shuffle(shuffled, random);

        int n = shuffled.size();
        boolean[][] reach = new boolean[n + 1][target + 1];
        reach[0][0] = true;
        for (int i = 1; i <= n; i++) {
            int size = shuffled.get(i - 1).size();
            for (int s = 0; s <= target; s++) {
                reach[i][s] = reach[i - 1][s] || (s >= size && reach[i - 1][s - size]);
            }
        }
        if (!reach[n][target]) return Optional.empty();

        List<List<Long>> chosen = new ArrayList<>();
        for (int i = n, s = target; s > 0; i--) {
            if (!reach[i - 1][s]) {
                List<Long> unit = shuffled.get(i - 1);
                chosen.add(unit);
                s -= unit.size();
            }
        }
        Collections.reverse(chosen);
        return Optional.of(chosen.stream().flatMap(List::stream).toList());
    }

    /**
     * Chia {@code target} đều cho các nhóm (vd. KANJI/VOCABULARY/GRAMMAR); nhóm thiếu câu nhường phần dư cho nhóm khác.
     * Kết quả giữ thứ tự nhóm của map. Nếu cụm bài đọc khiến không chia đều được thì chọn chung cả ngân hàng.
     */
    public static Optional<List<Long>> pickBalanced(Map<String, List<List<Long>>> groups, int target, Random random) {
        Map<String, Integer> quotas = quotas(groups, target);
        List<Long> picked = new ArrayList<>();
        for (Map.Entry<String, List<List<Long>>> group : groups.entrySet()) {
            Optional<List<Long>> part = pick(group.getValue(), quotas.get(group.getKey()), random);
            if (part.isEmpty()) {
                return pick(groups.values().stream().flatMap(List::stream).toList(), target, random);
            }
            picked.addAll(part.get());
        }
        return picked.size() == target ? Optional.of(picked) : Optional.empty();
    }

    private static Map<String, Integer> quotas(Map<String, List<List<Long>>> groups, int target) {
        Map<String, Integer> capacity = new HashMap<>();
        groups.forEach((key, units) -> capacity.put(key, units.stream().mapToInt(List::size).sum()));
        List<String> bySize = groups.keySet().stream().sorted(Comparator.comparing(capacity::get)).toList();

        Map<String, Integer> quotas = new HashMap<>();
        int remaining = target;
        for (int i = 0; i < bySize.size(); i++) {
            String key = bySize.get(i);
            int quota = Math.min(capacity.get(key), remaining / (bySize.size() - i));
            quotas.put(key, quota);
            remaining -= quota;
        }
        return quotas;
    }
}
