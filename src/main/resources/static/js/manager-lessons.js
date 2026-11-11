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

    /* ---------------------------------------------------- chapter column reordering */
    function getCsrfToken() {
        var meta = document.querySelector('meta[name="_csrf"]');
        if (meta && meta.content) return meta.content;
        var input = document.querySelector('input[name="_csrf"]');
        return input ? input.value : '';
    }

    function getCsrfHeader() {
        var meta = document.querySelector('meta[name="_csrf_header"]');
        return (meta && meta.content) ? meta.content : 'X-CSRF-TOKEN';
    }

    function showLessonToast(message, isError) {
        var toastEl = document.getElementById('lessonToast');
        if (!toastEl) return;
        var msgEl = document.getElementById('lessonToastMessage');
        if (msgEl) msgEl.textContent = message;
        if (isError) {
            toastEl.classList.remove('text-bg-success');
            toastEl.classList.add('text-bg-danger');
        } else {
            toastEl.classList.remove('text-bg-danger');
            toastEl.classList.add('text-bg-success');
        }
        if (window.bootstrap && window.bootstrap.Toast) {
            var toast = bootstrap.Toast.getInstance(toastEl) || new bootstrap.Toast(toastEl, { delay: 2500 });
            toast.show();
        } else {
            toastEl.classList.add('show');
            setTimeout(function () { toastEl.classList.remove('show'); }, 2500);
        }
    }

    function updateColumnDomState(container) {
        if (!container) return;
        var lessons = Array.prototype.slice.call(container.querySelectorAll('.manager-skill-lesson'));
        lessons.forEach(function (card, index) {
            card.setAttribute('data-order', String(index + 1));
            var badge = card.querySelector('.manager-lesson-order-badge');
            if (badge) badge.textContent = String(index + 1);
        });
    }

    function saveColumnOrder(container) {
        if (!container) return;
        var courseId = container.getAttribute('data-course-id');
        var chapter = container.getAttribute('data-chapter');
        var column = container.getAttribute('data-column');
        var lessons = Array.prototype.slice.call(container.querySelectorAll('.manager-skill-lesson'));
        var ids = lessons.map(function (card) {
            return card.getAttribute('data-lesson-id');
        }).filter(Boolean);

        if (!courseId || ids.length === 0) return;

        var formData = new URLSearchParams();
        formData.append('courseId', courseId);
        formData.append('chapter', chapter || '');
        formData.append('column', column || '');
        formData.append('ids', ids.join(','));

        var headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Content-Type': 'application/x-www-form-urlencoded'
        };
        var csrfToken = getCsrfToken();
        if (csrfToken) {
            headers[getCsrfHeader()] = csrfToken;
        }

        fetch('/manager/lessons/reorder', {
            method: 'POST',
            headers: headers,
            body: formData.toString()
        }).then(function (response) {
            return response.json();
        }).then(function (data) {
            if (data && data.success) {
                showLessonToast(data.message || 'Đã lưu thứ tự bài học thành công.', false);
            } else {
                showLessonToast((data && data.message) || 'Không thể lưu thứ tự bài học.', true);
            }
        }).catch(function (error) {
            console.error('Error saving lesson order:', error);
            showLessonToast('Lỗi khi lưu thứ tự bài học. Vui lòng thử lại.', true);
        });
    }

    function initChapterLessonOrdering() {
        var containers = document.querySelectorAll('.manager-skill-lessons');
        containers.forEach(function (container) {
            updateColumnDomState(container);

            var draggedCard = null;

            // Drag and drop events
            container.addEventListener('dragstart', function (event) {
                if (event.target.closest('button, a, input, form')) {
                    event.preventDefault();
                    return;
                }
                var card = event.target.closest('.manager-skill-lesson');
                if (!card) return;
                draggedCard = card;
                card.classList.add('is-dragging');
                if (event.dataTransfer) {
                    event.dataTransfer.effectAllowed = 'move';
                    event.dataTransfer.setData('text/plain', card.getAttribute('data-lesson-id') || '');
                }
            });

            container.addEventListener('dragend', function () {
                if (draggedCard) draggedCard.classList.remove('is-dragging');
                container.querySelectorAll('.manager-skill-lesson').forEach(function (c) {
                    c.classList.remove('drag-over-top', 'drag-over-bottom');
                });
                draggedCard = null;
            });

            container.addEventListener('dragover', function (event) {
                if (!draggedCard || draggedCard.parentNode !== container) return;
                event.preventDefault();
                if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';

                var targetCard = event.target.closest('.manager-skill-lesson');
                if (!targetCard || targetCard === draggedCard || targetCard.parentNode !== container) return;

                var rect = targetCard.getBoundingClientRect();
                var midY = rect.top + rect.height / 2;
                if (event.clientY < midY) {
                    targetCard.classList.add('drag-over-top');
                    targetCard.classList.remove('drag-over-bottom');
                } else {
                    targetCard.classList.add('drag-over-bottom');
                    targetCard.classList.remove('drag-over-top');
                }
            });

            container.addEventListener('dragleave', function (event) {
                var targetCard = event.target.closest('.manager-skill-lesson');
                if (targetCard) {
                    targetCard.classList.remove('drag-over-top', 'drag-over-bottom');
                }
            });

            container.addEventListener('drop', function (event) {
                if (!draggedCard || draggedCard.parentNode !== container) return;
                event.preventDefault();

                var targetCard = event.target.closest('.manager-skill-lesson');
                if (targetCard && targetCard !== draggedCard && targetCard.parentNode === container) {
                    var rect = targetCard.getBoundingClientRect();
                    var midY = rect.top + rect.height / 2;
                    if (event.clientY < midY) {
                        container.insertBefore(draggedCard, targetCard);
                    } else {
                        container.insertBefore(draggedCard, targetCard.nextSibling);
                    }
                }

                container.querySelectorAll('.manager-skill-lesson').forEach(function (c) {
                    c.classList.remove('drag-over-top', 'drag-over-bottom');
                });

                updateColumnDomState(container);
                saveColumnOrder(container);
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initChapterLessonOrdering);
    } else {
        initChapterLessonOrdering();
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
            if (!editor.innerHTML.trim() && valueField.value) {
                editor.innerHTML = sanitizeRichHtml(valueField.value);
            } else if (editor.innerHTML.trim() && !valueField.value) {
                valueField.value = sanitizeRichHtml(editor.innerHTML);
            }
            var syncFn = function () {
                valueField.value = sanitizeRichHtml(editor.innerHTML);
            };
            editor.addEventListener('input', syncFn);
            editor.addEventListener('blur', syncFn);
            var formEl = editor.closest('form');
            if (formEl) {
                formEl.addEventListener('submit', syncFn);
            }
            document.querySelectorAll('button[type="submit"][form="' + (formEl ? formEl.id : 'lessonForm') + '"]').forEach(function (btn) {
                btn.addEventListener('click', syncFn);
            });
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

    function renderLessonDetailContent(container) {
        if (!container) return;
        if (window.marked) {
            container.querySelectorAll('.manager-markdown-source').forEach(function (source) {
                var target = source.parentElement.querySelector('.manager-markdown-rendered');
                if (!target || !source.textContent.trim() || target.dataset.rendered === 'true') return;
                var markdown = source.textContent.replace(/\\(?:rightarrow|to)\b/g, '→').replace(/\\(?:leftarrow)\b/g, '←');
                target.innerHTML = window.marked.parse(markdown);
                if (window.renderMathInElement) {
                    window.renderMathInElement(target, {delimiters:[{left:'$$',right:'$$',display:true},{left:'$',right:'$',display:false}],throwOnError:false});
                }
                target.dataset.rendered = 'true';
            });
        }
        container.querySelectorAll('.manager-reading-passage-source').forEach(function (source) {
            var target = source.parentElement.querySelector('[data-rich-rendered]');
            if (target && target.dataset.rendered !== 'true' && typeof sanitizeRichHtml === 'function') {
                target.innerHTML = sanitizeRichHtml(source.textContent);
                target.classList.add('markdown-body');
                target.dataset.rendered = 'true';
            }
        });

        // Tô màu điểm ngữ pháp đỏ cho bảng cấu trúc kết hợp
        container.querySelectorAll('.grammar-f-col, .grammar-formation-single').forEach(function (col) {
            if (col.dataset.accented === 'true') return;
            var text = col.textContent;
            var regex = /(あるほど|であれば|ければ|ほど|なら|ば|ではない|ではないか|てはいけない|てもいい|なくてもいい|かもしれない|ばかり|たばかり|ところ|にする|になる)/g;
            if (regex.test(text)) {
                col.innerHTML = text.replace(regex, '<span class="grammar-accent">$1</span>');
                col.dataset.accented = 'true';
            }
        });
    }
    window.renderLessonDetailContent = renderLessonDetailContent;

    // Chi parse Markdown khi modal duoc mo ra qua su kien show.bs.modal
    document.addEventListener('show.bs.modal', function (event) {
        var modal = event.target;
        if (modal && modal.classList && modal.classList.contains('manager-lesson-detail-modal')) {
            renderLessonDetailContent(modal);
        }
    });

    document.querySelectorAll('.listening-question-card').forEach(function (card) {
        var mode = card.querySelector('[name="questionAudioMode"]');
        setQuestionAudioMode(card, mode && mode.value === 'file' ? 'file' : 'url', false);
    });

    document.addEventListener('click', function (event) {
        var detailBtn = event.target.closest('.js-lesson-detail-btn');
        if (detailBtn) {
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.which === 2) return;
            event.preventDefault();
            var lessonId = detailBtn.getAttribute('data-lesson-id');
            var courseId = detailBtn.getAttribute('data-course-id');
            if (!lessonId) return;

            var existingModal = document.getElementById('lessonDetail' + lessonId);
            if (existingModal && window.bootstrap && window.bootstrap.Modal) {
                renderLessonDetailContent(existingModal);
                bootstrap.Modal.getOrCreateInstance(existingModal).show();
                return;
            }

            var icon = detailBtn.querySelector('i');
            var originalClass = icon ? icon.className : '';
            if (icon) {
                icon.className = 'spinner-border spinner-border-sm';
            }

            var url = '/manager/lessons/' + encodeURIComponent(lessonId) + '/detail-modal?courseId=' + encodeURIComponent(courseId || '');
            fetch(url, {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(function (response) {
                if (!response.ok) throw new Error('HTTP ' + response.status);
                return response.text();
            })
            .then(function (html) {
                var container = document.getElementById('lessonDetailModalContainer');
                if (!container) {
                    container = document.createElement('div');
                    container.id = 'lessonDetailModalContainer';
                    document.body.appendChild(container);
                }
                container.innerHTML = html;
                renderLessonDetailContent(container);

                var modalEl = document.getElementById('lessonDetail' + lessonId);
                if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                    bootstrap.Modal.getOrCreateInstance(modalEl).show();
                }
            })
            .catch(function (error) {
                console.error('Failed to load lesson detail modal via AJAX, navigating directly:', error);
                window.location.href = detailBtn.getAttribute('href') || ('/manager/lessons?courseId=' + courseId + '&detailId=' + lessonId);
            })
            .finally(function () {
                if (icon) {
                    icon.className = originalClass;
                }
            });
            return;
        }
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
                var posInput = row.querySelector('[name="partOfSpeech"]');
                if (posInput && libraryPick.dataset.pos) {
                    posInput.value = libraryPick.dataset.pos;
                }
                row.querySelector('[name="wordMeaning"]').value = libraryPick.dataset.meaning || '';
                row.querySelector('[name="wordExample"]').value = libraryPick.dataset.example || '';
                var exViInput = row.querySelector('[name="wordExampleVi"]');
                if (exViInput && libraryPick.dataset.exampleVi) {
                    exViInput.value = libraryPick.dataset.exampleVi;
                }
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

    /* ------------------------------------------------------- unsaved-changes guard & validation */
    function clearFieldFeedback(field) {
        if (!field) return;
        field.classList.remove('is-invalid');
        var container = field.closest('.lesson-field-unit')
            || field.closest('.lesson-audio-row')
            || field;
        var parent = container.parentElement;
        if (parent) {
            var fb = parent.querySelector(':scope > .lesson-field-feedback');
            if (fb) fb.remove();
        }
    }

    function showFieldError(field, message) {
        if (!field) return;
        field.classList.add('is-invalid');

        var targetToInsertAfter = field;
        if (field.closest('.lesson-field-unit')) {
            targetToInsertAfter = field.closest('.lesson-field-unit');
        } else if (field.closest('.lesson-audio-row')) {
            targetToInsertAfter = field.closest('.lesson-audio-row');
        }

        var parent = targetToInsertAfter.parentElement;
        if (parent) {
            var existing = parent.querySelector(':scope > .lesson-field-feedback');
            if (existing) {
                var textEl = existing.querySelector('.feedback-text');
                if (textEl) textEl.textContent = message;
                else existing.textContent = message;
            } else {
                var div = document.createElement('div');
                div.className = 'lesson-field-feedback text-danger small mt-1';
                div.innerHTML = '<i class="bi bi-exclamation-circle-fill me-1"></i><span class="feedback-text">' + message + '</span>';
                targetToInsertAfter.insertAdjacentElement('afterend', div);
            }
        }

        var clearHandler = function () {
            clearFieldFeedback(field);
            field.removeEventListener('input', clearHandler);
            field.removeEventListener('change', clearHandler);
        };
        field.addEventListener('input', clearHandler);
        field.addEventListener('change', clearHandler);
    }

    function clearAllErrors(form) {
        form.querySelectorAll('.is-invalid').forEach(function (el) {
            el.classList.remove('is-invalid');
        });
        form.querySelectorAll('.lesson-field-feedback').forEach(function (el) {
            el.remove();
        });
        var clientAlert = form.querySelector('.lesson-client-alert');
        if (clientAlert) {
            clientAlert.style.display = 'none';
            clientAlert.textContent = '';
        }
    }

    var editorForms = document.querySelectorAll('[data-lesson-editor]');
    editorForms.forEach(function (editorForm) {
        // Tắt validation mặc định của trình duyệt để sử dụng thông báo trực tiếp tại field
        editorForm.setAttribute('novalidate', 'novalidate');

        var dirty = false;
        editorForm.addEventListener('input', function () {
            dirty = true;
        });

        // Nếu server trả về lỗi (ví dụ trùng tên bài học), highlight ngay trường tên bài học
        var serverAlert = editorForm.querySelector('.lesson-modal-alert');
        if (serverAlert && serverAlert.textContent.trim()) {
            var alertMsg = serverAlert.textContent.trim();
            if (alertMsg.toLowerCase().includes('tên bài học') || alertMsg.toLowerCase().includes('đã tồn tại')) {
                var titleInput = editorForm.querySelector('#lessonTitle, #otherLessonTitle, input[name="title"]');
                if (titleInput) {
                    showFieldError(titleInput, alertMsg);
                }
            }
        }

        editorForm.addEventListener('submit', function (event) {
            clearAllErrors(editorForm);
            var firstInvalid = null;

            function flagError(field, message) {
                if (!field) return;
                showFieldError(field, message);
                if (!firstInvalid) {
                    firstInvalid = field;
                }
            }

            // Đồng bộ nội dung rich editor trước khi kiểm tra
            editorForm.querySelectorAll('[data-rich-editor]').forEach(function (ed) {
                var vf = editorForm.querySelector('[data-rich-value]') || (ed.parentElement ? ed.parentElement.querySelector('[data-rich-value]') : null);
                if (vf) vf.value = ed.innerHTML;
            });

            // 1. Validate Tiêu đề bài học (bắt buộc, tối đa 150 ký tự)
            var titleInput = editorForm.querySelector('#lessonTitle, #otherLessonTitle, input[name="title"]');
            if (titleInput) {
                var titleVal = titleInput.value.trim();
                if (!titleVal) {
                    flagError(titleInput, 'Vui lòng nhập tên bài học.');
                } else if (titleVal.length > 150) {
                    flagError(titleInput, 'Tên bài học không được vượt quá 150 ký tự (hiện tại: ' + titleVal.length + ').');
                }
            }

            // 2. Validate Chương / Phần
            var sectionSelect = editorForm.querySelector('#lessonSectionName, #otherLessonSectionName, select[name="sectionName"]');
            if (sectionSelect && sectionSelect.offsetParent !== null) {
                var sectionVal = sectionSelect.value.trim();
                if (!sectionVal) {
                    flagError(sectionSelect, 'Vui lòng chọn chương / phần.');
                }
            }

            // 3. Validate Thời lượng ước tính (0 - 360 phút)
            var durationInput = editorForm.querySelector('#lessonDuration, input[name="durationMinutes"]');
            if (durationInput && durationInput.value.trim() !== '') {
                var durVal = Number(durationInput.value.trim());
                if (isNaN(durVal) || durVal < 0 || durVal > 360) {
                    flagError(durationInput, 'Thời lượng ước tính phải từ 0 đến 360 phút.');
                }
            }

            // 4. Validate Nội dung bài học khác (tối đa 50.000 ký tự)
            var otherContent = editorForm.querySelector('#otherLessonContent, textarea[name="content"], [data-rich-value][name="content"]');
            if (otherContent && otherContent.value && otherContent.value.length > 50000) {
                flagError(otherContent, 'Nội dung bài học không được vượt quá 50.000 ký tự.');
            }

            // 5. Validate Kỹ năng Từ vựng (VOCABULARY)
            var wordRepeat = editorForm.querySelector('[data-repeat="words"]');
            if (wordRepeat) {
                var wordRows = wordRepeat.querySelectorAll('.lesson-word-grid:not(.lesson-word-head)');
                if (wordRows.length === 0) {
                    var addWordBtn = editorForm.querySelector('[data-add-row="wordRowTemplate"]');
                    flagError(addWordBtn || wordRepeat, 'Vui lòng thêm ít nhất một từ vựng.');
                } else {
                    wordRows.forEach(function (row, idx) {
                        var wordInput = row.querySelector('input[name="word"]');
                        var readingInput = row.querySelector('input[name="wordReading"]');
                        var meaningInput = row.querySelector('input[name="wordMeaning"]');
                        var exJpInput = row.querySelector('input[name="wordExample"]');
                        var exViInput = row.querySelector('input[name="wordExampleVi"]');

                        if (wordInput) {
                            var wVal = wordInput.value.trim();
                            if (!wVal) {
                                flagError(wordInput, 'Vui lòng nhập từ vựng (dòng #' + (idx + 1) + ').');
                            } else if (wVal.length > 50) {
                                flagError(wordInput, 'Từ vựng không vượt quá 50 ký tự.');
                            }
                        }
                        if (readingInput) {
                            var rVal = readingInput.value.trim();
                            if (!rVal) {
                                flagError(readingInput, 'Vui lòng nhập cách đọc (dòng #' + (idx + 1) + ').');
                            } else if (rVal.length > 50) {
                                flagError(readingInput, 'Cách đọc không vượt quá 50 ký tự.');
                            }
                        }
                        if (meaningInput) {
                            var mVal = meaningInput.value.trim();
                            if (!mVal) {
                                flagError(meaningInput, 'Vui lòng nhập ý nghĩa (dòng #' + (idx + 1) + ').');
                            } else if (mVal.length > 150) {
                                flagError(meaningInput, 'Ý nghĩa không vượt quá 150 ký tự.');
                            }
                        }
                        if (exJpInput && exViInput) {
                            var jpVal = exJpInput.value.trim();
                            var viVal = exViInput.value.trim();
                            if (jpVal && !viVal) {
                                flagError(exViInput, 'Vui lòng nhập bản dịch tiếng Việt cho ví dụ (dòng #' + (idx + 1) + ').');
                            } else if (viVal && !jpVal) {
                                flagError(exJpInput, 'Vui lòng nhập câu ví dụ tiếng Nhật (dòng #' + (idx + 1) + ').');
                            }
                        }
                    });
                }
            }

            // 6. Validate Kỹ năng Chữ Hán (KANJI)
            var kanjiChar = editorForm.querySelector('#kanjiCharacter');
            if (kanjiChar) {
                var charVal = kanjiChar.value.trim();
                if (!charVal) {
                    flagError(kanjiChar, 'Vui lòng nhập chữ Hán (Kanji).');
                } else if (charVal.length !== 1) {
                    flagError(kanjiChar, 'Chữ Hán (Kanji) phải đúng 1 ký tự.');
                }

                var hanviet = editorForm.querySelector('#kanjiHanviet');
                if (hanviet) {
                    var hvVal = hanviet.value.trim();
                    if (!hvVal) {
                        flagError(hanviet, 'Vui lòng nhập âm Hán Việt.');
                    } else if (hvVal.length > 50) {
                        flagError(hanviet, 'Âm Hán Việt tối đa 50 ký tự.');
                    }
                }

                var kanjiMeaning = editorForm.querySelector('#kanjiMeaning');
                if (kanjiMeaning) {
                    var kmVal = kanjiMeaning.value.trim();
                    if (!kmVal) {
                        flagError(kanjiMeaning, 'Vui lòng nhập ý nghĩa của chữ Hán.');
                    } else if (kmVal.length > 200) {
                        flagError(kanjiMeaning, 'Ý nghĩa chữ Hán tối đa 200 ký tự.');
                    }
                }

                var kanjiStrokes = editorForm.querySelector('#kanjiStrokes');
                if (kanjiStrokes) {
                    var stVal = kanjiStrokes.value.trim();
                    var stNum = Number(stVal);
                    if (!stVal || isNaN(stNum) || stNum < 1 || stNum > 40) {
                        flagError(kanjiStrokes, 'Số nét của chữ Hán phải từ 1 đến 40.');
                    }
                }

                var kanjiOn = editorForm.querySelector('#kanjiOn');
                var kanjiKun = editorForm.querySelector('#kanjiKun');
                if (kanjiOn && kanjiKun && !kanjiOn.value.trim() && !kanjiKun.value.trim()) {
                    flagError(kanjiOn, 'Chữ Hán cần có ít nhất một âm On hoặc âm Kun.');
                }

                var compoundRows = editorForm.querySelectorAll('[data-repeat="compounds"] .lesson-compound-row');
                compoundRows.forEach(function (row, idx) {
                    var cpWord = row.querySelector('input[name="compoundWord"]');
                    var cpRead = row.querySelector('input[name="compoundReading"]');
                    var cpMean = row.querySelector('input[name="compoundMeaning"]');
                    var w = cpWord ? cpWord.value.trim() : '';
                    var r = cpRead ? cpRead.value.trim() : '';
                    var m = cpMean ? cpMean.value.trim() : '';
                    if (w || r || m) {
                        if (!w && cpWord) flagError(cpWord, 'Vui lòng nhập từ ghép (dòng #' + (idx + 1) + ').');
                        if (!r && cpRead) flagError(cpRead, 'Vui lòng nhập cách đọc từ ghép (dòng #' + (idx + 1) + ').');
                        if (!m && cpMean) flagError(cpMean, 'Vui lòng nhập nghĩa từ ghép (dòng #' + (idx + 1) + ').');
                    }
                });
            }

            // 7. Validate Kỹ năng Ngữ pháp (GRAMMAR)
            var grammarPattern = editorForm.querySelector('#grammarPattern');
            if (grammarPattern) {
                var gpVal = grammarPattern.value.trim();
                if (!gpVal) {
                    flagError(grammarPattern, 'Vui lòng nhập mẫu câu ngữ pháp.');
                } else if (gpVal.length > 100) {
                    flagError(grammarPattern, 'Mẫu câu không vượt quá 100 ký tự.');
                }

                var grammarMeaning = editorForm.querySelector('#grammarMeaning');
                if (grammarMeaning) {
                    var gmVal = grammarMeaning.value.trim();
                    if (!gmVal) {
                        flagError(grammarMeaning, 'Vui lòng nhập ý nghĩa của ngữ pháp.');
                    } else if (gmVal.length > 255) {
                        flagError(grammarMeaning, 'Ý nghĩa không vượt quá 255 ký tự.');
                    }
                }

                var grammarFormation = editorForm.querySelector('#grammarFormation');
                if (grammarFormation) {
                    var gfVal = grammarFormation.value.trim();
                    if (!gfVal) {
                        flagError(grammarFormation, 'Vui lòng nhập cấu trúc kết hợp của ngữ pháp.');
                    } else if (gfVal.length > 500) {
                        flagError(grammarFormation, 'Cấu trúc kết hợp không vượt quá 500 ký tự.');
                    }
                }

                var grammarUsage = editorForm.querySelector('#grammarUsage');
                if (grammarUsage && grammarUsage.value && grammarUsage.value.length > 1000) {
                    flagError(grammarUsage, 'Giải thích cách dùng tối đa 1.000 ký tự.');
                }

                var exampleRows = editorForm.querySelectorAll('[data-repeat="examples"] .lesson-example-row');
                exampleRows.forEach(function (row, idx) {
                    var exJp = row.querySelector('input[name="exampleJp"]');
                    var exVi = row.querySelector('input[name="exampleVi"]');
                    if (exJp && exVi) {
                        var jp = exJp.value.trim();
                        var vi = exVi.value.trim();
                        if (jp && !vi) {
                            flagError(exVi, 'Vui lòng nhập bản dịch tiếng Việt cho ví dụ #' + (idx + 1) + '.');
                        } else if (vi && !jp) {
                            flagError(exJp, 'Vui lòng nhập câu ví dụ tiếng Nhật cho ví dụ #' + (idx + 1) + '.');
                        }
                    }
                });
            }

            // 8. Validate Kỹ năng Đọc hiểu (READING)
            var readingEditor = editorForm.querySelector('#readingPassage');
            var readingVal = editorForm.querySelector('textarea[name="passage"]');
            if (readingEditor || readingVal) {
                var text = (readingEditor ? readingEditor.innerText : (readingVal ? readingVal.value : '')).trim();
                if (!text) {
                    flagError(readingEditor || readingVal, 'Vui lòng nhập nội dung đoạn văn cho bài Đọc hiểu.');
                }

                var readingQuestions = editorForm.querySelectorAll('[data-repeat="questions"] .lesson-question');
                if (readingQuestions.length === 0) {
                    var addQBtn = editorForm.querySelector('[data-add-row="questionRowTemplate"]');
                    flagError(addQBtn || readingEditor, 'Bài đọc hiểu cần ít nhất một câu hỏi.');
                } else {
                    readingQuestions.forEach(function (card, idx) {
                        var qText = card.querySelector('input[name="questionText"]');
                        var optA = card.querySelector('input[name="optionA"]');
                        var optB = card.querySelector('input[name="optionB"]');
                        var optC = card.querySelector('input[name="optionC"]');
                        var optD = card.querySelector('input[name="optionD"]');

                        if (qText && !qText.value.trim()) {
                            flagError(qText, 'Vui lòng nhập nội dung câu hỏi #' + (idx + 1) + '.');
                        }
                        if (optA && !optA.value.trim()) flagError(optA, 'Vui lòng nhập phương án A (câu #' + (idx + 1) + ').');
                        if (optB && !optB.value.trim()) flagError(optB, 'Vui lòng nhập phương án B (câu #' + (idx + 1) + ').');
                        if (optC && !optC.value.trim()) flagError(optC, 'Vui lòng nhập phương án C (câu #' + (idx + 1) + ').');
                        if (optD && !optD.value.trim()) flagError(optD, 'Vui lòng nhập phương án D (câu #' + (idx + 1) + ').');
                    });
                }
            }

            // 9. Validate Kỹ năng Nghe hiểu (LISTENING)
            var listeningCards = editorForm.querySelectorAll('.listening-question-card');
            if (editorForm.querySelector('.listening-question-section')) {
                if (listeningCards.length === 0) {
                    var addListeningBtn = editorForm.querySelector('[data-add-row="listeningQuestionRowTemplate"]');
                    flagError(addListeningBtn || editorForm.querySelector('.listening-question-section'), 'Bài nghe hiểu cần ít nhất một câu hỏi.');
                } else {
                    listeningCards.forEach(function (card, idx) {
                        var mode = card.querySelector('[name="questionAudioMode"]');
                        var audioFile = card.querySelector('[name="questionAudioFile"]');
                        var audioUrl = card.querySelector('[name="questionAudioUrl"]');
                        var fileMode = mode && mode.value === 'file';

                        var hasFile = audioFile && audioFile.files && audioFile.files.length > 0;
                        var hasUrl = audioUrl && audioUrl.value.trim().length > 0;

                        if (fileMode) {
                            if (!hasFile && !hasUrl) {
                                flagError(audioFile, 'Vui lòng chọn file nghe cho câu hỏi #' + (idx + 1) + '.');
                            }
                        } else {
                            if (!hasUrl) {
                                flagError(audioUrl, 'Vui lòng nhập URL bài nghe cho câu hỏi #' + (idx + 1) + '.');
                            }
                        }

                        var qText = card.querySelector('input[name="questionText"]');
                        var optA = card.querySelector('input[name="optionA"]');
                        var optB = card.querySelector('input[name="optionB"]');
                        var optC = card.querySelector('input[name="optionC"]');
                        var optD = card.querySelector('input[name="optionD"]');

                        if (qText && !qText.value.trim()) {
                            flagError(qText, 'Vui lòng nhập nội dung câu hỏi #' + (idx + 1) + '.');
                        }
                        if (optA && !optA.value.trim()) flagError(optA, 'Vui lòng nhập phương án A (câu #' + (idx + 1) + ').');
                        if (optB && !optB.value.trim()) flagError(optB, 'Vui lòng nhập phương án B (câu #' + (idx + 1) + ').');
                        if (optC && !optC.value.trim()) flagError(optC, 'Vui lòng nhập phương án C (câu #' + (idx + 1) + ').');
                        if (optD && !optD.value.trim()) flagError(optD, 'Vui lòng nhập phương án D (câu #' + (idx + 1) + ').');
                    });
                }
            }

            // Nếu có bất kỳ trường nào sai validate
            if (firstInvalid) {
                event.preventDefault();
                event.stopImmediatePropagation();

                var clientAlert = editorForm.querySelector('.lesson-client-alert');
                if (clientAlert) {
                    clientAlert.style.display = 'block';
                    clientAlert.textContent = 'Vui lòng kiểm tra và sửa lại các trường thông tin bị lỗi bên dưới.';
                }

                firstInvalid.scrollIntoView({ behavior: 'smooth', block: 'center' });
                setTimeout(function () {
                    try {
                        firstInvalid.focus();
                    } catch (e) {}
                }, 150);
                return false;
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
    });
})();
