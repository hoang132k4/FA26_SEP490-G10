package com.jlpp.modules.flashcard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/flashcards")
public class FlashcardController {

    @GetMapping
    public String list() {
        return "pages/flashcard/list";
    }

    @GetMapping("/practice")
    public String practice() {
        return "pages/flashcard/practice";
    }
}
