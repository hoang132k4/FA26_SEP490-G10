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
        questionRowTemplate: 'questions'
    };

    document.addEventListener('click', function (event) {
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
            var row = removeButton.parentElement;
            if (container && row) {
                container.removeChild(row);
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
        editorForm.addEventListener('submit', function () {
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
