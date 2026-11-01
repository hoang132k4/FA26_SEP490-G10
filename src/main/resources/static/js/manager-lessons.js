// Manager · Lesson workspace
// Progressive enhancement only: every action below also works without JavaScript.
(function () {
    'use strict';

    /* ---------------------------------------------------------------- ordering */
    var sortList = document.querySelector('[data-lesson-sort-list].is-sorting');
    var idsInput = document.getElementById('lessonReorderIds');
    var saveButton = document.getElementById('lessonReorderSave');

    function syncOrderFromDom() {
        if (!sortList || !idsInput) {
            return;
        }
        var rows = Array.prototype.slice.call(sortList.querySelectorAll('[data-lesson-id]'));
        idsInput.value = rows.map(function (row) {
            return row.getAttribute('data-lesson-id');
        }).join(',');
        rows.forEach(function (row, index) {
            var order = row.querySelector('.lesson-row-order');
            if (order) {
                order.textContent = String(index + 1);
            }
        });
        if (saveButton) {
            saveButton.hidden = false;
        }
    }

    if (sortList) {
        var draggedRow = null;

        Array.prototype.forEach.call(sortList.querySelectorAll('.lesson-row'), function (row) {
            row.draggable = true;

            row.addEventListener('dragstart', function () {
                draggedRow = row;
                row.classList.add('is-dragging');
            });

            row.addEventListener('dragend', function () {
                row.classList.remove('is-dragging');
                draggedRow = null;
            });

            row.addEventListener('dragover', function (event) {
                if (!draggedRow || draggedRow === row) {
                    return;
                }
                event.preventDefault();
                var box = row.getBoundingClientRect();
                var insertAfter = event.clientY > box.top + box.height / 2;
                row.parentNode.insertBefore(draggedRow, insertAfter ? row.nextSibling : row);
            });

            row.addEventListener('drop', function (event) {
                event.preventDefault();
                syncOrderFromDom();
            });
        });
    }

    /* ------------------------------------------------------------ lesson popup */
    var REPEAT_TARGETS = {
        exampleRowTemplate: 'examples',
        wordRowTemplate: 'words',
        compoundRowTemplate: 'compounds',
        questionRowTemplate: 'questions',
        listeningQuestionRowTemplate: 'questions'
    };

    function updateListeningQuestionCount(container) {
        if (!container) return;
        var cards = container.querySelectorAll('.listening-question-card');
        var count = document.querySelector('[data-listening-question-count]');
        if (count) count.textContent = String(cards.length);
        cards.forEach(function (card, index) {
            var heading = card.querySelector('.listening-question-heading strong');
            if (heading) heading.textContent = 'Câu hỏi #' + (index + 1);
        });
    }

    function setQuestionAudioMode(card, mode, clearOther) {
        if (!card) return;
        var fileMode = mode === 'file';
        var modeInput = card.querySelector('[name="questionAudioMode"]');
        var fileInput = card.querySelector('[name="questionAudioFile"]');
        var urlInput = card.querySelector('[name="questionAudioUrl"]');
        var filePanel = card.querySelector('[data-audio-panel="file"]');
        var urlPanel = card.querySelector('[data-audio-panel="url"]');
        if (modeInput) modeInput.value = fileMode ? 'file' : 'url';
        if (filePanel) filePanel.hidden = !fileMode;
        if (urlPanel) urlPanel.hidden = fileMode;
        if (fileInput) fileInput.disabled = !fileMode;
        if (urlInput) urlInput.disabled = false;
        if (clearOther) {
            if (fileMode && urlInput) urlInput.value = '';
            if (!fileMode && fileInput) fileInput.value = '';
        }
        card.querySelectorAll('.lesson-audio-mode-button').forEach(function (button) {
            var selected = button.dataset.audioMode === (fileMode ? 'file' : 'url');
            button.classList.toggle('active', selected);
            button.setAttribute('aria-pressed', String(selected));
        });
    }

    function sanitizeRichHtml(html) {
        html = String(html || '');
        if (!/<\/?(?:p|div|br|b|strong|i|em|u|span|ul|ol|li|blockquote|font)(?:\s|\/?>)/i.test(html)) {
            var plain = document.createElement('div');
            plain.textContent = html;
            return plain.innerHTML;
        }
        var parsed = new DOMParser().parseFromString(html, 'text/html');
        var allowed = new Set(['P', 'DIV', 'BR', 'B', 'STRONG', 'I', 'EM', 'U', 'SPAN', 'UL', 'OL', 'LI', 'BLOCKQUOTE']);
        function safeColor(value) {
            var color = String(value || '').trim();
            return /^(#[0-9a-f]{3,8}|rgba?\([\d\s.,%]+\)|[a-z]{3,20})$/i.test(color) ? color : '';
        }
        function copyNode(node) {
            if (node.nodeType === Node.TEXT_NODE) return document.createTextNode(node.nodeValue || '');
            if (node.nodeType !== Node.ELEMENT_NODE) return document.createDocumentFragment();
            if (/^(SCRIPT|STYLE|IFRAME|OBJECT|SVG|MATH)$/.test(node.tagName)) return document.createDocumentFragment();
            var tag = node.tagName === 'FONT' ? 'SPAN' : node.tagName;
            if (!allowed.has(tag)) {
                var unwrapped = document.createDocumentFragment();
                Array.from(node.childNodes).forEach(function (child) { unwrapped.appendChild(copyNode(child)); });
                return unwrapped;
            }
            var clean = document.createElement(tag.toLowerCase());
            var color = safeColor(node.getAttribute('color') || (node.style && node.style.color));
            var background = safeColor(node.style && (node.style.backgroundColor || node.style.background));
            if (color) clean.style.color = color;
            if (background) clean.style.backgroundColor = background;
            var align = node.getAttribute('align') || (node.style && node.style.textAlign) || '';
            if (/^(left|center|right|justify)$/i.test(align)) clean.style.textAlign = align.toLowerCase();
            var lineHeight = node.style && node.style.lineHeight;
            if (/^(1|1\.\d+|2|2\.\d+|3)$/.test(lineHeight || '')) clean.style.lineHeight = lineHeight;
            Array.from(node.childNodes).forEach(function (child) { clean.appendChild(copyNode(child)); });
            return clean;
        }
        var result = document.createElement('div');
        Array.from(parsed.body.childNodes).forEach(function (node) { result.appendChild(copyNode(node)); });
        return result.innerHTML;
    }

    function initializeReadingRichEditor() {
        var editor = document.querySelector('[data-rich-editor]');
        var valueField = document.querySelector('[data-rich-value]');
        if (editor && valueField) {
            editor.innerHTML = sanitizeRichHtml(valueField.value);
            var savedRange = null;
            document.addEventListener('selectionchange', function () {
                var selection = window.getSelection();
                if (selection && selection.rangeCount && editor.contains(selection.anchorNode)) {
                    savedRange = selection.getRangeAt(0).cloneRange();
                }
            });
            function restoreSelection() {
                editor.focus();
                if (savedRange) {
                    var selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(savedRange);
                }
            }
            editor.closest('form').addEventListener('submit', function () {
                valueField.value = sanitizeRichHtml(editor.innerHTML);
            });
            document.querySelectorAll('.reading-rich-toolbar [data-rich-command]').forEach(function (button) {
                button.addEventListener('mousedown', function (event) { event.preventDefault(); });
                button.addEventListener('click', function () {
                    restoreSelection();
                    document.execCommand(button.dataset.richCommand, false, button.dataset.richValue || null);
                    editor.dispatchEvent(new Event('input', { bubbles: true }));
                });
            });
            document.querySelectorAll('.reading-rich-toolbar [data-rich-color]').forEach(function (picker) {
                picker.addEventListener('input', function () {
                    restoreSelection();
                    document.execCommand(picker.dataset.richColor, false, picker.value);
                    editor.dispatchEvent(new Event('input', { bubbles: true }));
                });
            });
            document.querySelectorAll('.reading-rich-toolbar [data-rich-line-height]').forEach(function (select) {
                select.addEventListener('change', function () {
                    restoreSelection();
                    var selection = window.getSelection();
                    var parent = selection && selection.rangeCount ? selection.getRangeAt(0).commonAncestorContainer : editor;
                    var element = parent.nodeType === Node.ELEMENT_NODE ? parent : parent.parentElement;
                    var block = element && element.closest('p,div,li,blockquote');
                    if (!block || !editor.contains(block)) {
                        document.execCommand('formatBlock', false, 'p');
                        selection = window.getSelection();
                        parent = selection && selection.rangeCount ? selection.getRangeAt(0).commonAncestorContainer : editor;
                        element = parent.nodeType === Node.ELEMENT_NODE ? parent : parent.parentElement;
                        block = element && element.closest('p,div,li,blockquote');
                    }
                    if (block && editor.contains(block)) block.style.lineHeight = select.value;
                    editor.dispatchEvent(new Event('input', { bubbles: true }));
                });
            });
            document.querySelectorAll('.reading-rich-toolbar [data-rich-align]').forEach(function (select) {
                select.addEventListener('change', function () {
                    restoreSelection();
                    var command = select.value === 'full' ? 'justifyFull' : 'justify' + select.value.charAt(0).toUpperCase() + select.value.slice(1);
                    document.execCommand(command, false, null);
                    editor.dispatchEvent(new Event('input', { bubbles: true }));
                });
            });
        }
        document.querySelectorAll('.manager-reading-passage-source').forEach(function (source) {
            var target = source.parentElement.querySelector('[data-rich-rendered]');
            if (target) {
                target.innerHTML = sanitizeRichHtml(source.textContent);
                target.classList.add('markdown-body');
            }
        });
    }

    initializeReadingRichEditor();

    document.querySelectorAll('.listening-question-card').forEach(function (card) {
        var mode = card.querySelector('[name="questionAudioMode"]');
        setQuestionAudioMode(card, mode && mode.value === 'file' ? 'file' : 'url', false);
    });

    document.addEventListener('click', function (event) {
        var audioModeButton = event.target.closest('.lesson-audio-mode-button');
        if (audioModeButton) {
            setQuestionAudioMode(audioModeButton.closest('.listening-question-card'), audioModeButton.dataset.audioMode, true);
            return;
        }

        var libraryPick = event.target.closest('.lesson-library-pick');
        if (libraryPick) {
            var vocabRows = document.querySelector('[data-repeat="words"]');
            var vocabTemplate = document.getElementById('wordRowTemplate');
            if (!vocabRows || !vocabTemplate) return;
            var entryId = libraryPick.dataset.entryId;
            var existingRow = vocabRows.querySelector('[data-library-entry="' + entryId + '"]');
            if (existingRow) {
                existingRow.remove();
                libraryPick.classList.remove('is-selected');
                libraryPick.setAttribute('aria-pressed', 'false');
            } else {
                var fragment = vocabTemplate.content.cloneNode(true);
                var row = fragment.querySelector('.lesson-word-grid');
                row.dataset.libraryEntry = entryId;
                row.querySelector('[name="word"]').value = libraryPick.dataset.word || '';
                row.querySelector('[name="wordReading"]').value = libraryPick.dataset.reading || '';
                row.querySelector('[name="wordMeaning"]').value = libraryPick.dataset.meaning || '';
                row.querySelector('[name="wordExample"]').value = libraryPick.dataset.example || '';
                vocabRows.appendChild(fragment);
                libraryPick.classList.add('is-selected');
                libraryPick.setAttribute('aria-pressed', 'true');
            }
            return;
        }

        var addButton = event.target.closest('[data-add-row]');
        if (addButton) {
            var templateId = addButton.getAttribute('data-add-row');
            var template = document.getElementById(templateId);
            var repeatKey = REPEAT_TARGETS[templateId];
            var target = repeatKey ? document.querySelector('[data-repeat="' + repeatKey + '"]') : null;
            if (template && target) {
                var clone = template.content.cloneNode(true);
                target.appendChild(clone);
                updateListeningQuestionCount(target);
                var added = target.lastElementChild;
                if (added) {
                    var firstField = added.querySelector('input, select');
                    if (firstField) {
                        firstField.focus();
                    }
                }
            }
            return;
        }

        var removeButton = event.target.closest('.lesson-remove-row');
        if (removeButton) {
            var container = removeButton.closest('[data-repeat]');
            var row = removeButton.closest('.listening-question-card, .lesson-question, .lesson-example-row, .lesson-word-grid, .lesson-compound-row');
            if (row && row.classList.contains('listening-question-card')
                && container.querySelectorAll('.listening-question-card').length <= 1) return;
            if (!row) row = removeButton.parentElement;
            if (container && row) {
                container.removeChild(row);
                updateListeningQuestionCount(container);
            }
        }
    });

    document.querySelectorAll('.lesson-library-pick').forEach(function (item) {
        var matchingRow = Array.from(document.querySelectorAll('[data-repeat="words"] [name="word"]'))
            .find(function (input) { return input.value === (item.dataset.word || ''); });
        if (matchingRow) {
            matchingRow.closest('.lesson-word-grid').dataset.libraryEntry = item.dataset.entryId;
            item.classList.add('is-selected');
            item.setAttribute('aria-pressed', 'true');
        }
    });

    var lessonModal = document.getElementById('lessonModal');
    if (lessonModal) {
        document.body.classList.add('modal-open');
        var titleField = lessonModal.querySelector('#lessonTitle');
        if (titleField && !titleField.value) {
            titleField.focus();
        }
    }

    var deleteForm = document.querySelector('[data-confirm-delete]');
    if (deleteForm) {
        deleteForm.addEventListener('submit', function (event) {
            var confirmed = window.confirm('Xoá bài học này khỏi kỹ năng? Các bài còn lại sẽ được đánh số lại.');
            if (!confirmed) {
                event.preventDefault();
            }
        });
    }

    /* ------------------------------------------------------- unsaved-changes guard */
    var editorForm = document.querySelector('[data-lesson-editor]');
    if (editorForm) {
        var dirty = false;
        editorForm.addEventListener('input', function () {
            dirty = true;
        });
        editorForm.addEventListener('submit', function (event) {
            var listeningCards = editorForm.querySelectorAll('.listening-question-card');
            for (var index = 0; index < listeningCards.length; index++) {
                var card = listeningCards[index];
                var mode = card.querySelector('[name="questionAudioMode"]');
                var audioFile = card.querySelector('[name="questionAudioFile"]');
                var audioUrl = card.querySelector('[name="questionAudioUrl"]');
                var fileMode = mode && mode.value === 'file';
                var missingAudio = fileMode
                    ? (!audioFile || !audioFile.files || audioFile.files.length === 0)
                    : (!audioUrl || !audioUrl.value.trim());
                if (missingAudio) {
                    event.preventDefault();
                    window.alert('Câu hỏi nghe #' + (index + 1) + (fileMode ? ' cần chọn file nghe.' : ' cần nhập URL bài nghe.'));
                    if (fileMode && audioFile) audioFile.focus();
                    else if (audioUrl) audioUrl.focus();
                    return;
                }
            }
            dirty = false;
        });
        window.addEventListener('beforeunload', function (event) {
            if (!dirty) {
                return;
            }
            event.preventDefault();
            event.returnValue = '';
        });
    }
})();
