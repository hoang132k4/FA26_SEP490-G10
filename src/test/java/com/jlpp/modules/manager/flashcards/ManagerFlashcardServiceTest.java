package com.jlpp.modules.manager.flashcards;

import com.jlpp.modules.lesson.repository.LessonRepository;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetCardForm;
import com.jlpp.modules.manager.flashcards.dto.ManagerFlashcardSetForm;
import com.jlpp.modules.manager.flashcards.model.ManagerFlashcard;
import com.jlpp.modules.manager.flashcards.repository.ManagerFlashcardRepository;
import com.jlpp.modules.manager.flashcards.service.ManagerFlashcardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerFlashcardServiceTest {

    @Mock
    private ManagerFlashcardRepository flashcardRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private ManagerFlashcardService flashcardService;

    @Test
    void createsAllCardsInQuizletStyleSetWithSharedDeckInformation() {
        ManagerFlashcardSetForm form = validSet();
        form.getCards().add(card("学生", "がくせい", "học sinh", null));
        form.getCards().add(card("先生", "せんせい", "giáo viên", "先生です。"));
        when(lessonRepository.existsById(12L)).thenReturn(true);
        when(flashcardRepository.insert(any(ManagerFlashcard.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var savedCards = flashcardService.createFlashcardSet(form, 34L);

        assertEquals(2, savedCards.size());
        ArgumentCaptor<ManagerFlashcard> captor = ArgumentCaptor.forClass(ManagerFlashcard.class);
        verify(flashcardRepository, times(2)).insert(captor.capture());
        for (ManagerFlashcard saved : captor.getAllValues()) {
            assertEquals(12L, saved.getLessonId());
            assertEquals(34L, saved.getCreatorId());
            assertEquals("Từ vựng N5", saved.getCustomDeckName());
            assertEquals("N5", saved.getJlptLevel());
            assertEquals("NEW", saved.getReviewStatus());
            assertEquals(saved.getVocabulary(), saved.getFrontContent());
            assertEquals(saved.getMeaning(), saved.getBackContent());
            assertEquals(true, saved.isCustom());
        }
        assertEquals("がくせい", savedCards.get(0).getReading());
        assertEquals("先生です。", savedCards.get(1).getExampleSentence());
    }

    @Test
    void refusesToCreateSetForMissingLesson() {
        when(lessonRepository.existsById(12L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> flashcardService.createFlashcardSet(validSet(), 34L));
    }

    private ManagerFlashcardSetForm validSet() {
        ManagerFlashcardSetForm form = new ManagerFlashcardSetForm();
        form.setDeckName(" Từ vựng N5 ");
        form.setLessonId(12L);
        form.setJlptLevel("N5");
        form.setCards(new java.util.ArrayList<>());
        return form;
    }

    private ManagerFlashcardSetCardForm card(String vocabulary, String reading, String meaning, String example) {
        ManagerFlashcardSetCardForm card = new ManagerFlashcardSetCardForm();
        card.setVocabulary(vocabulary);
        card.setReading(reading);
        card.setMeaning(meaning);
        card.setExampleSentence(example);
        return card;
    }
}
