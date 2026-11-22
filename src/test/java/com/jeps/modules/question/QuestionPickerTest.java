package com.jeps.modules.question;

import com.jeps.modules.question.service.QuestionPicker;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionPickerTest {

    @RepeatedTest(20)
    void picksExactlyTargetCountWithoutDuplicates() {
        List<List<Long>> units = List.of(List.of(1L), List.of(2L), List.of(3L), List.of(4L), List.of(5L));

        List<Long> picked = QuestionPicker.pick(units, 3, new Random()).orElseThrow();

        assertEquals(3, picked.size());
        assertEquals(3, new HashSet<>(picked).size());
    }

    @RepeatedTest(20)
    void keepsGroupedQuestionsTogetherAndInOrder() {
        List<List<Long>> units = List.of(List.of(10L, 11L, 12L), List.of(20L), List.of(21L));

        List<Long> picked = QuestionPicker.pick(units, 5, new Random()).orElseThrow();

        int start = picked.indexOf(10L);
        assertEquals(List.of(10L, 11L, 12L), picked.subList(start, start + 3));
    }

    @Test
    void returnsEmptyWhenGroupsCannotAddUpToTarget() {
        List<List<Long>> units = List.of(List.of(1L, 2L, 3L), List.of(4L, 5L, 6L));

        Optional<List<Long>> picked = QuestionPicker.pick(units, 4, new Random(1));

        assertTrue(picked.isEmpty());
    }

    @Test
    void zeroTargetPicksNothing() {
        assertEquals(List.of(), QuestionPicker.pick(List.of(List.of(1L)), 0, new Random()).orElseThrow());
    }

    @Test
    void pick_emptyUnitsList_targetOne_returnsEmpty() {
        Optional<List<Long>> result = QuestionPicker.pick(List.of(), 1, new Random());

        assertTrue(result.isEmpty());
    }

    @Test
    void pick_allSingletonUnits_picksExact() {
        List<List<Long>> units = List.of(List.of(1L), List.of(2L), List.of(3L));

        List<Long> picked = QuestionPicker.pick(units, 3, new Random()).orElseThrow();

        assertEquals(3, picked.size());
        assertEquals(3, new HashSet<>(picked).size());
        assertTrue(picked.containsAll(List.of(1L, 2L, 3L)));
    }

    @Test
    void pick_oneUnitExactlyMatchesTarget_returnsIt() {
        List<List<Long>> units = List.of(List.of(10L, 11L, 12L));

        List<Long> picked = QuestionPicker.pick(units, 3, new Random()).orElseThrow();

        assertEquals(List.of(10L, 11L, 12L), picked);
    }

    @Test
    void pick_unitLargerThanTarget_skipped() {
        List<List<Long>> units = List.of(List.of(1L, 2L, 3L, 4L, 5L));

        Optional<List<Long>> result = QuestionPicker.pick(units, 3, new Random());

        assertTrue(result.isEmpty());
    }

    @Test
    void pick_negativeTarget_returnsEmpty() {
        Optional<List<Long>> result = QuestionPicker.pick(List.of(List.of(1L)), -1, new Random());
        assertTrue(result.isEmpty());
    }

    @Test
    void pick_targetExceedsTotalAvailable_returnsEmpty() {
        List<List<Long>> units = List.of(List.of(1L), List.of(2L));
        Optional<List<Long>> result = QuestionPicker.pick(units, 5, new Random());
        assertTrue(result.isEmpty());
    }

    @Test
    void pick_mixedGroupSizes_selectsCorrectTotal() {
        List<List<Long>> units = List.of(
                List.of(1L, 2L), List.of(3L), List.of(4L, 5L, 6L), List.of(7L));

        Optional<List<Long>> result = QuestionPicker.pick(units, 6, new Random(42));
        assertTrue(result.isPresent());
        assertEquals(6, result.get().size());
    }

    @Test
    void pick_deterministicWithSameSeed_sameResult() {
        List<List<Long>> units = List.of(
                List.of(1L), List.of(2L), List.of(3L), List.of(4L), List.of(5L));

        List<Long> pick1 = QuestionPicker.pick(units, 3, new Random(42)).orElseThrow();
        List<Long> pick2 = QuestionPicker.pick(units, 3, new Random(42)).orElseThrow();

        assertEquals(pick1, pick2);
    }

    @RepeatedTest(20)
    void pick_findsRareCombinationAmongManyLargeGroups() {
        List<List<Long>> units = new ArrayList<>();
        for (long g = 0; g < 40; g++) {
            units.add(List.of(g * 10, g * 10 + 1, g * 10 + 2, g * 10 + 3, g * 10 + 4, g * 10 + 5));
        }
        units.add(List.of(1001L, 1002L, 1003L, 1004L));
        units.add(List.of(2001L, 2002L, 2003L, 2004L));

        List<Long> picked = QuestionPicker.pick(units, 8, new Random()).orElseThrow();

        assertEquals(Set.of(1001L, 1002L, 1003L, 1004L, 2001L, 2002L, 2003L, 2004L), new HashSet<>(picked));
    }

    @Test
    void pick_varies_acrossSeeds() {
        List<List<Long>> units = new ArrayList<>();
        for (long i = 1; i <= 30; i++) units.add(List.of(i));

        Set<List<Long>> results = new HashSet<>();
        for (int seed = 0; seed < 10; seed++) {
            results.add(QuestionPicker.pick(units, 5, new Random(seed)).orElseThrow());
        }

        assertTrue(results.size() > 1);
    }

    @Test
    void pickBalanced_splitsEvenlyAcrossGroups() {
        Map<String, List<List<Long>>> groups = new LinkedHashMap<>();
        groups.put("VOCABULARY", singletons(1, 10));
        groups.put("GRAMMAR", singletons(101, 110));

        List<Long> picked = QuestionPicker.pickBalanced(groups, 6, new Random()).orElseThrow();

        assertEquals(6, picked.size());
        assertEquals(3, picked.stream().filter(id -> id < 100).count());
        assertEquals(3, picked.stream().filter(id -> id > 100).count());
    }

    @Test
    void pickBalanced_smallGroupGivesRemainderToOthers() {
        Map<String, List<List<Long>>> groups = new LinkedHashMap<>();
        groups.put("KANJI", singletons(1, 1));
        groups.put("VOCABULARY", singletons(11, 20));
        groups.put("GRAMMAR", singletons(101, 110));

        List<Long> picked = QuestionPicker.pickBalanced(groups, 9, new Random()).orElseThrow();

        assertEquals(9, picked.size());
        assertTrue(picked.contains(1L));
        assertEquals(4, picked.stream().filter(id -> id > 10 && id < 100).count());
        assertEquals(4, picked.stream().filter(id -> id > 100).count());
    }

    @Test
    void pickBalanced_keepsGroupOrder() {
        Map<String, List<List<Long>>> groups = new LinkedHashMap<>();
        groups.put("VOCABULARY", singletons(1, 5));
        groups.put("GRAMMAR", singletons(101, 105));

        List<Long> picked = QuestionPicker.pickBalanced(groups, 4, new Random()).orElseThrow();

        assertTrue(picked.get(0) < 100 && picked.get(1) < 100);
        assertTrue(picked.get(2) > 100 && picked.get(3) > 100);
    }

    @Test
    void pickBalanced_fallsBackWhenEvenSplitImpossible() {
        Map<String, List<List<Long>>> groups = new LinkedHashMap<>();
        groups.put("GRAMMAR", List.of(List.of(1L, 2L, 3L)));
        groups.put("VOCABULARY", singletons(101, 103));

        List<Long> picked = QuestionPicker.pickBalanced(groups, 4, new Random()).orElseThrow();

        assertEquals(4, picked.size());
        assertTrue(picked.containsAll(List.of(1L, 2L, 3L)));
    }

    @Test
    void pickBalanced_notEnoughQuestions_returnsEmpty() {
        Map<String, List<List<Long>>> groups = new LinkedHashMap<>();
        groups.put("GRAMMAR", singletons(1, 2));

        assertTrue(QuestionPicker.pickBalanced(groups, 3, new Random()).isEmpty());
    }

    private static List<List<Long>> singletons(long from, long to) {
        List<List<Long>> units = new ArrayList<>();
        for (long i = from; i <= to; i++) units.add(List.of(i));
        return units;
    }
}
