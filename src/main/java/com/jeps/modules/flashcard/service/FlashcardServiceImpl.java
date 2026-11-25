package com.jeps.modules.flashcard.service;

import com.jeps.common.exception.ResourceNotFoundException;
import com.jeps.modules.course.entity.Lesson;
import com.jeps.modules.course.repository.LessonRepository;
import com.jeps.modules.flashcard.dto.FlashcardDeckDto;
import com.jeps.modules.flashcard.dto.FlashcardRequest;
import com.jeps.modules.flashcard.entity.Flashcard;
import com.jeps.modules.flashcard.repository.FlashcardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public Flashcard getFlashcardById(Long id) {
        return flashcardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thẻ Flashcard với ID: " + id));
    }

    @Override
    @Transactional
    public Flashcard createFlashcard(FlashcardRequest request) {
        return createFlashcard(request, null);
    }

    @Override
    @Transactional
    public Flashcard createFlashcard(FlashcardRequest request, Long creatorId) {
        boolean custom = (creatorId != null) || (request.getCustomDeckName() != null && !request.getCustomDeckName().isBlank());
        String customDeck = (request.getCustomDeckName() != null && !request.getCustomDeckName().isBlank())
                ? request.getCustomDeckName().trim()
                : null;

        String vocab = request.getVocabulary() != null ? request.getVocabulary().trim() : "";
        String meaning = request.getMeaning() != null ? request.getMeaning().trim() : "";
        String front = (request.getFrontContent() != null && !request.getFrontContent().isBlank())
                ? request.getFrontContent().trim()
                : vocab;
        String back = (request.getBackContent() != null && !request.getBackContent().isBlank())
                ? request.getBackContent().trim()
                : meaning;

        Flashcard flashcard = Flashcard.builder()
                .lessonId(request.getLessonId())
                .creatorId(creatorId)
                .customDeckName(customDeck)
                .isCustom(custom)
                .vocabulary(vocab)
                .reading(request.getReading() != null ? request.getReading().trim() : null)
                .meaning(meaning)
                .frontContent(front)
                .backContent(back)
                .exampleSentence(request.getExampleSentence() != null ? request.getExampleSentence().trim() : null)
                .jlptLevel(request.getJlptLevel() != null && !request.getJlptLevel().isBlank() ? request.getJlptLevel().trim() : "N4")
                .reviewStatus(request.getReviewStatus() != null && !request.getReviewStatus().isBlank() ? request.getReviewStatus().trim() : "NEW")
                .build();

        return flashcardRepository.save(flashcard);
    }

    @Override
    @Transactional
    public Flashcard updateFlashcard(Long id, FlashcardRequest request) {
        Flashcard flashcard = getFlashcardById(id);

        flashcard.setLessonId(request.getLessonId());
        flashcard.setVocabulary(request.getVocabulary().trim());
        flashcard.setReading(request.getReading() != null ? request.getReading().trim() : null);
        flashcard.setMeaning(request.getMeaning().trim());
        flashcard.setFrontContent(request.getFrontContent() != null ? request.getFrontContent().trim() : null);
        flashcard.setBackContent(request.getBackContent() != null ? request.getBackContent().trim() : null);
        flashcard.setExampleSentence(request.getExampleSentence() != null ? request.getExampleSentence().trim() : null);

        if (request.getCustomDeckName() != null) {
            flashcard.setCustomDeckName(request.getCustomDeckName().trim());
        }

        if (request.getJlptLevel() != null && !request.getJlptLevel().isBlank()) {
            flashcard.setJlptLevel(request.getJlptLevel().trim());
        }
        if (request.getReviewStatus() != null && !request.getReviewStatus().isBlank()) {
            flashcard.setReviewStatus(request.getReviewStatus().trim());
        }

        return flashcardRepository.save(flashcard);
    }

    @Override
    @Transactional
    public void deleteFlashcard(Long id) {
        Flashcard flashcard = getFlashcardById(id);
        flashcardRepository.delete(flashcard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flashcard> getPracticeFlashcards(String level, Long lessonId, String deckName, Integer limit) {
        List<Flashcard> list;
        if (deckName != null && !deckName.trim().isEmpty()) {
            String trimmed = deckName.trim();
            list = flashcardRepository.findByCustomDeckName(trimmed);
            if (list.isEmpty()) {
                list = flashcardRepository.findByCustomDeckNameIgnoreCase(trimmed);
            }
        } else {
            String cleanLevel = (level != null && !level.trim().isEmpty() && !level.equalsIgnoreCase("ALL")) ? level.trim() : null;
            list = flashcardRepository.searchFlashcards(null, cleanLevel, lessonId, null);
        }

        // Sort: prioritize NEW, then REVIEWING, then MASTERED, and shuffle within groups
        List<Flashcard> newCards = new ArrayList<>();
        List<Flashcard> reviewingCards = new ArrayList<>();
        List<Flashcard> masteredCards = new ArrayList<>();

        for (Flashcard f : list) {
            String status = f.getReviewStatus() != null ? f.getReviewStatus().toUpperCase() : "NEW";
            switch (status) {
                case "MASTERED" -> masteredCards.add(f);
                case "REVIEWING" -> reviewingCards.add(f);
                default -> newCards.add(f);
            }
        }

        Collections.shuffle(newCards);
        Collections.shuffle(reviewingCards);
        Collections.shuffle(masteredCards);

        List<Flashcard> combined = new ArrayList<>();
        combined.addAll(newCards);
        combined.addAll(reviewingCards);
        combined.addAll(masteredCards);

        int max = (limit != null && limit > 0) ? Math.min(limit, combined.size()) : combined.size();
        return combined.subList(0, max);
    }

    @Override
    @Transactional
    public Flashcard updateReviewStatus(Long id, String status) {
        Flashcard flashcard = getFlashcardById(id);
        String upperStatus = status != null ? status.toUpperCase() : "REVIEWING";
        if (!List.of("NEW", "REVIEWING", "MASTERED").contains(upperStatus)) {
            upperStatus = "REVIEWING";
        }
        flashcard.setReviewStatus(upperStatus);
        return flashcardRepository.save(flashcard);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        long newCount = 0;
        long reviewingCount = 0;
        long masteredCount = 0;
        long total = 0;

        List<Object[]> grouped = flashcardRepository.countGroupedByReviewStatus();
        for (Object[] row : grouped) {
            String st = (String) row[0];
            Long cnt = (Long) row[1];
            if (cnt == null) cnt = 0L;
            total += cnt;
            if ("NEW".equalsIgnoreCase(st)) {
                newCount += cnt;
            } else if ("REVIEWING".equalsIgnoreCase(st)) {
                reviewingCount += cnt;
            } else if ("MASTERED".equalsIgnoreCase(st)) {
                masteredCount += cnt;
            }
        }

        stats.put("total", total);
        stats.put("new", newCount);
        stats.put("reviewing", reviewingCount);
        stats.put("mastered", masteredCount);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardDeckDto> getSystemFlashcardDecks(String keyword, String level) {
        List<Flashcard> allCards = flashcardRepository.findAll();
        List<Flashcard> systemCards = allCards.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsCustom()) && c.getCreatorId() == null)
                .collect(Collectors.toList());

        return groupAndFilterDecks(systemCards, keyword, level, "SYSTEM", false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlashcardDeckDto> getUserFlashcardDecks(String keyword, String level, Long currentUserId) {
        List<Flashcard> allCards = flashcardRepository.findAll();
        List<Flashcard> userCards = allCards.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsCustom()) || c.getCreatorId() != null)
                .filter(c -> currentUserId == null || c.getCreatorId() == null || c.getCreatorId().equals(currentUserId))
                .collect(Collectors.toList());

        // Batch fetch all lessons for user cards at once instead of N queries
        Set<Long> userLessonIds = userCards.stream()
                .map(Flashcard::getLessonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Lesson> userLessonMap = userLessonIds.isEmpty() ? Collections.emptyMap() :
                lessonRepository.findAllById(userLessonIds).stream()
                        .collect(Collectors.toMap(Lesson::getId, l -> l, (a, b) -> a));

        // Group user cards by customDeckName first, fallback to lesson or level
        Map<String, List<Flashcard>> byDeckName = new LinkedHashMap<>();
        for (Flashcard c : userCards) {
            String deckName;
            if (c.getCustomDeckName() != null && !c.getCustomDeckName().isBlank()) {
                deckName = c.getCustomDeckName().trim();
            } else if (c.getLessonId() != null) {
                Lesson l = userLessonMap.get(c.getLessonId());
                deckName = (l != null) ? "Gói bài học: " + l.getTitle() : "Gói bài học #" + c.getLessonId();
            } else {
                deckName = "Sổ tay từ vựng tự do " + (c.getJlptLevel() != null ? c.getJlptLevel() : "N4");
            }
            byDeckName.computeIfAbsent(deckName, k -> new ArrayList<>()).add(c);
        }

        List<FlashcardDeckDto> decks = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, List<Flashcard>> entry : byDeckName.entrySet()) {
            String deckTitle = entry.getKey();
            List<Flashcard> cards = entry.getValue();

            DeckStatusCounts counts = countDeckStatuses(cards);
            String lvl = (!cards.isEmpty() && cards.get(0).getJlptLevel() != null) ? cards.get(0).getJlptLevel() : "N4";
            Long lId = (!cards.isEmpty()) ? cards.get(0).getLessonId() : null;

            FlashcardDeckDto deck = FlashcardDeckDto.builder()
                    .deckId("user-deck-" + (index++))
                    .title(deckTitle)
                    .description("Bộ từ vựng cá nhân do bạn tự biên tập và bổ sung để ôn tập theo tiến trình riêng.")
                    .jlptLevel(lvl)
                    .lessonId(lId)
                    .lessonTitle("Gói cá nhân")
                    .topicName("Không gian học viên")
                    .iconClass("bi-journal-bookmark-fill")
                    .cards(cards)
                    .totalCards(counts.total())
                    .newCount(counts.newCount())
                    .reviewingCount(counts.reviewingCount())
                    .masteredCount(counts.masteredCount())
                    .masteryRate(counts.masteryRate())
                    .deckType("USER")
                    .custom(true)
                    .creatorId(currentUserId)
                    .build();

            decks.add(deck);
        }

        return filterAndSortDecks(decks, keyword, level);
    }

    private List<FlashcardDeckDto> groupAndFilterDecks(List<Flashcard> cardsList, String keyword, String level, String deckType, boolean custom) {
        // Group flashcards by lessonId
        Map<Long, List<Flashcard>> byLesson = cardsList.stream()
                .filter(c -> c.getLessonId() != null)
                .collect(Collectors.groupingBy(Flashcard::getLessonId, LinkedHashMap::new, Collectors.toList()));

        // Group cards without lessonId by level
        Map<String, List<Flashcard>> withoutLesson = cardsList.stream()
                .filter(c -> c.getLessonId() == null)
                .collect(Collectors.groupingBy(c -> (c.getJlptLevel() != null ? c.getJlptLevel() : "N4"), LinkedHashMap::new, Collectors.toList()));

        List<FlashcardDeckDto> decks = new ArrayList<>();

        // Batch fetch all lessons for system decks at once instead of N queries
        Set<Long> systemLessonIds = byLesson.keySet();
        Map<Long, Lesson> systemLessonMap = systemLessonIds.isEmpty() ? Collections.emptyMap() :
                lessonRepository.findAllById(systemLessonIds).stream()
                        .collect(Collectors.toMap(Lesson::getId, l -> l, (a, b) -> a));

        for (Map.Entry<Long, List<Flashcard>> entry : byLesson.entrySet()) {
            Long lessonId = entry.getKey();
            List<Flashcard> cards = entry.getValue();
            Lesson lesson = systemLessonMap.get(lessonId);

            String jlpt = (lesson != null && lesson.getCourse() != null && lesson.getCourse().getLevel() != null)
                    ? lesson.getCourse().getLevel()
                    : (!cards.isEmpty() && cards.get(0).getJlptLevel() != null ? cards.get(0).getJlptLevel() : "N4");

            String title = (lesson != null)
                    ? "Gói từ vựng: " + lesson.getTitle()
                    : "Gói từ vựng chuyên đề #" + lessonId;

            String desc = (lesson != null && lesson.getDescription() != null && !lesson.getDescription().isBlank())
                    ? lesson.getDescription()
                    : "Tổng hợp các thẻ ghi nhớ từ vựng, Hán tự và ví dụ mẫu chuẩn giáo trình JLPT.";

            DeckStatusCounts counts = countDeckStatuses(cards);

            String icon = switch (jlpt.toUpperCase()) {
                case "N5" -> "bi-flower1";
                case "N4" -> "bi-brightness-high-fill";
                case "N3" -> "bi-briefcase-fill";
                case "N2" -> "bi-mortarboard-fill";
                case "N1" -> "bi-trophy-fill";
                default -> "bi-card-text";
            };

            FlashcardDeckDto deck = FlashcardDeckDto.builder()
                    .deckId("lesson-" + lessonId)
                    .title(title)
                    .description(desc)
                    .jlptLevel(jlpt)
                    .lessonId(lessonId)
                    .lessonTitle(lesson != null ? lesson.getTitle() : "Bài học #" + lessonId)
                    .topicName(lesson != null && lesson.getSectionName() != null ? lesson.getSectionName() : "Chủ đề bài học")
                    .iconClass(icon)
                    .cards(cards)
                    .totalCards(counts.total())
                    .newCount(counts.newCount())
                    .reviewingCount(counts.reviewingCount())
                    .masteredCount(counts.masteredCount())
                    .masteryRate(counts.masteryRate())
                    .deckType(deckType)
                    .custom(custom)
                    .build();

            decks.add(deck);
        }

        for (Map.Entry<String, List<Flashcard>> entry : withoutLesson.entrySet()) {
            String lvl = entry.getKey();
            List<Flashcard> cards = entry.getValue();
            DeckStatusCounts counts = countDeckStatuses(cards);

            FlashcardDeckDto deck = FlashcardDeckDto.builder()
                    .deckId("general-" + lvl.toLowerCase())
                    .title("Gói từ vựng tổng hợp " + lvl)
                    .description("Bộ thẻ flashcard ôn luyện từ vựng trọng tâm cấp độ " + lvl)
                    .jlptLevel(lvl)
                    .lessonId(null)
                    .lessonTitle("Tổng hợp")
                    .topicName("Từ vựng mở rộng")
                    .iconClass("bi-collection-fill")
                    .cards(cards)
                    .totalCards(counts.total())
                    .newCount(counts.newCount())
                    .reviewingCount(counts.reviewingCount())
                    .masteredCount(counts.masteredCount())
                    .masteryRate(counts.masteryRate())
                    .deckType(deckType)
                    .custom(custom)
                    .build();
            decks.add(deck);
        }

        return filterAndSortDecks(decks, keyword, level);
    }

    private List<FlashcardDeckDto> filterAndSortDecks(List<FlashcardDeckDto> decks, String keyword, String level) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;
        String cleanLevel = (level != null && !level.trim().isEmpty() && !level.equalsIgnoreCase("ALL")) ? level.trim().toUpperCase() : null;

        if (cleanLevel != null) {
            decks = decks.stream()
                    .filter(d -> cleanLevel.equalsIgnoreCase(d.getJlptLevel()))
                    .collect(Collectors.toList());
        }

        if (cleanKeyword != null) {
            decks = decks.stream()
                    .filter(d -> d.getTitle().toLowerCase().contains(cleanKeyword)
                            || d.getDescription().toLowerCase().contains(cleanKeyword)
                            || d.getCards().stream().anyMatch(c ->
                                (c.getVocabulary() != null && c.getVocabulary().toLowerCase().contains(cleanKeyword))
                                || (c.getMeaning() != null && c.getMeaning().toLowerCase().contains(cleanKeyword))
                                || (c.getReading() != null && c.getReading().toLowerCase().contains(cleanKeyword))))
                    .collect(Collectors.toList());
        }

        List<String> order = List.of("N5", "N4", "N3", "N2", "N1");
        decks.sort(Comparator.comparingInt(d -> {
            int idx = order.indexOf(d.getJlptLevel() != null ? d.getJlptLevel().toUpperCase() : "");
            return idx >= 0 ? idx : 99;
        }));

        return decks;
    }

    @Override
    @Transactional
    public int updateDeckInfo(String oldDeckName, String newDeckName, String newJlptLevel, Long creatorId) {
        List<Flashcard> deckCards = findCustomDeckCards(oldDeckName, creatorId, false, false);

        for (Flashcard c : deckCards) {
            if (newDeckName != null && !newDeckName.isBlank()) {
                c.setCustomDeckName(newDeckName.trim());
            }
            if (newJlptLevel != null && !newJlptLevel.isBlank()) {
                c.setJlptLevel(newJlptLevel.trim());
            }
        }
        flashcardRepository.saveAll(deckCards);
        return deckCards.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Flashcard> getDeckCards(String deckName, Long creatorId) {
        return findCustomDeckCards(deckName, creatorId, true, true).stream()
                .sorted(Comparator.comparing(Flashcard::getFlashcardId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int saveFullDeck(String oldDeckName, String newDeckName, String newJlptLevel, Long lessonId, Long creatorId, List<FlashcardRequest> cards, List<Long> deletedCardIds) {
        String finalDeckName = (newDeckName != null && !newDeckName.isBlank()) ? newDeckName.trim() : (oldDeckName != null ? oldDeckName.trim() : "Gói từ vựng");
        String finalLevel = (newJlptLevel != null && !newJlptLevel.isBlank()) ? newJlptLevel.trim() : "N4";

        // 1. Process deletions
        if (deletedCardIds != null && !deletedCardIds.isEmpty()) {
            for (Long delId : deletedCardIds) {
                if (delId != null) {
                    flashcardRepository.findById(delId).ifPresent(c -> {
                        if (creatorId == null || c.getCreatorId() == null || creatorId.equals(c.getCreatorId())) {
                            flashcardRepository.delete(c);
                        }
                    });
                }
            }
        }

        // 2. Process card updates and additions
        int savedCount = 0;
        if (cards != null) {
            for (FlashcardRequest req : cards) {
                String vocab = req.getVocabulary() != null ? req.getVocabulary().trim() : "";
                String meaning = req.getMeaning() != null ? req.getMeaning().trim() : "";
                if (vocab.isEmpty() || meaning.isEmpty()) {
                    continue;
                }
                String reading = req.getReading() != null ? req.getReading().trim() : "";
                String example = req.getExampleSentence() != null ? req.getExampleSentence().trim() : "";
                String backContent = meaning + (!reading.isBlank() ? " (" + reading + ")" : "");

                if (req.getFlashcardId() != null) {
                    // Update existing card
                    Flashcard existing = flashcardRepository.findById(req.getFlashcardId()).orElse(null);
                    if (existing != null && (creatorId == null || existing.getCreatorId() == null || creatorId.equals(existing.getCreatorId()))) {
                        existing.setVocabulary(vocab);
                        existing.setReading(reading);
                        existing.setMeaning(meaning);
                        existing.setExampleSentence(example);
                        existing.setFrontContent(vocab);
                        existing.setBackContent(backContent);
                        existing.setCustomDeckName(finalDeckName);
                        existing.setJlptLevel(finalLevel);
                        existing.setLessonId(lessonId);
                        existing.setIsCustom(true);
                        flashcardRepository.save(existing);
                        savedCount++;
                    }
                } else {
                    // Insert new card
                    Flashcard newCard = Flashcard.builder()
                            .vocabulary(vocab)
                            .reading(reading)
                            .meaning(meaning)
                            .exampleSentence(example)
                            .frontContent(vocab)
                            .backContent(backContent)
                            .customDeckName(finalDeckName)
                            .jlptLevel(finalLevel)
                            .lessonId(lessonId)
                            .creatorId(creatorId)
                            .isCustom(true)
                            .reviewStatus("NEW")
                            .build();
                    flashcardRepository.save(newCard);
                    savedCount++;
                }
            }
        }

        // 3. If deck name changed, update any remaining cards under oldDeckName
        if (oldDeckName != null && !oldDeckName.equalsIgnoreCase(finalDeckName)) {
            List<Flashcard> remaining = findCustomDeckCards(oldDeckName, creatorId, true, true);
            for (Flashcard c : remaining) {
                c.setCustomDeckName(finalDeckName);
                c.setJlptLevel(finalLevel);
                c.setLessonId(lessonId);
            }
            flashcardRepository.saveAll(remaining);
        }

        return savedCount;
    }

    @Override
    @Transactional
    public int deleteDeck(String deckName, Long creatorId) {
        List<Flashcard> deckCards = findCustomDeckCards(deckName, creatorId, true, true);
        if (!deckCards.isEmpty()) {
            flashcardRepository.deleteAll(deckCards);
        }
        return deckCards.size();
    }

    private List<Flashcard> findCustomDeckCards(
            String deckName, Long creatorId, boolean allowNullCreator, boolean ignoreCase) {
        if (deckName == null || deckName.isBlank()) {
            return Collections.emptyList();
        }
        List<Flashcard> matches = ignoreCase
                ? flashcardRepository.findByCustomDeckNameIgnoreCase(deckName.trim())
                : flashcardRepository.findByCustomDeckName(deckName.trim());
        return matches.stream()
                .filter(c -> creatorId == null
                        || (allowNullCreator && c.getCreatorId() == null)
                        || creatorId.equals(c.getCreatorId()))
                .collect(Collectors.toList());
    }

    private DeckStatusCounts countDeckStatuses(List<Flashcard> cards) {
        int newCount = 0;
        int reviewingCount = 0;
        int masteredCount = 0;
        for (Flashcard card : cards) {
            if ("MASTERED".equalsIgnoreCase(card.getReviewStatus())) {
                masteredCount++;
            } else if ("REVIEWING".equalsIgnoreCase(card.getReviewStatus())) {
                reviewingCount++;
            } else if ("NEW".equalsIgnoreCase(card.getReviewStatus())) {
                newCount++;
            }
        }
        int total = cards.size();
        int masteryRate = total > 0 ? (int) Math.round((masteredCount * 100.0) / total) : 0;
        return new DeckStatusCounts(newCount, reviewingCount, masteredCount, total, masteryRate);
    }

    private record DeckStatusCounts(int newCount, int reviewingCount, int masteredCount, int total, int masteryRate) {
    }
}
