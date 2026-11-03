// JLPP Platform Main Scripts
document.addEventListener('DOMContentLoaded', () => {
    // Flashcard Flip Listener
    document.querySelectorAll('.flashcard-inner').forEach(card => {
        card.addEventListener('click', () => {
            card.classList.toggle('is-flipped');
        });
    });

    // Option Selection Listener for Exams & Quizzes
    document.querySelectorAll('.option-item').forEach(option => {
        option.addEventListener('click', function() {
            const radio = this.querySelector('input[type="radio"]');
            if (radio) {
                radio.checked = true;
                const parent = this.closest('.question-box');
                if (parent) {
                    parent.querySelectorAll('.option-item').forEach(opt => opt.classList.remove('selected'));
                }
                this.classList.add('selected');
            }
        });
    });

    // Auto-dismiss Alerts after 5s
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            try {
                const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                bsAlert.close();
            } catch(e) {}
        }, 5000);
    });

    // Auto-initialize standard table paginations
    if (document.querySelector('.timeline-row')) {
        initTablePagination({
            rowsSelector: '.timeline-row',
            navSelector: '#timelinePageNav',
            infoSelector: '#timelinePageInfo',
            pageSize: 5,
            itemName: 'bài học'
        });
    }

    if (document.querySelector('.recalibrate-row')) {
        initTablePagination({
            rowsSelector: '.recalibrate-row',
            navSelector: '#recalibratePageNav',
            infoSelector: '#recalibratePageInfo',
            pageSize: 5,
            itemName: 'bài học'
        });
    }

    if (document.querySelector('.order-row')) {
        initTablePagination({
            rowsSelector: '.order-row',
            navSelector: '#ordersPageNav',
            infoSelector: '#ordersPageInfo',
            pageSize: 5,
            itemName: 'đơn hàng'
        });
    }
});

// Universal Table Client-Side Pagination
window.initTablePagination = function(config) {
    const rows = document.querySelectorAll(config.rowsSelector);
    const nav = document.querySelector(config.navSelector);
    const info = document.querySelector(config.infoSelector);
    const pageSize = config.pageSize || 5;
    const itemName = config.itemName || 'mục';

    if (!rows || rows.length === 0 || !nav) {
        return;
    }

    const totalRows = rows.length;
    const totalPages = Math.max(1, Math.ceil(totalRows / pageSize));

    function renderPage(page) {
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        const startIdx = (page - 1) * pageSize;
        const endIdx = Math.min(startIdx + pageSize, totalRows);

        rows.forEach((row, idx) => {
            row.style.display = (idx >= startIdx && idx < endIdx) ? '' : 'none';
        });

        if (info) {
            info.innerHTML = `Hiển thị <strong>${totalRows > 0 ? startIdx + 1 : 0} - ${endIdx}</strong> trong tổng số <strong>${totalRows}</strong> ${itemName}`;
        }

        // Render pagination buttons
        nav.innerHTML = '';

        // Previous button
        const prevLi = document.createElement('li');
        prevLi.className = `page-item ${page === 1 ? 'disabled' : ''}`;
        prevLi.innerHTML = `<a class="page-link" href="javascript:void(0)" aria-label="Trước">&laquo;</a>`;
        if (page > 1) {
            prevLi.querySelector('a').addEventListener('click', (e) => {
                e.preventDefault();
                renderPage(page - 1);
            });
        }
        nav.appendChild(prevLi);

        // Page numbers
        for (let i = 1; i <= totalPages; i++) {
            const li = document.createElement('li');
            li.className = `page-item ${i === page ? 'active' : ''}`;
            li.innerHTML = `<a class="page-link" href="javascript:void(0)">${i}</a>`;
            if (i !== page) {
                li.querySelector('a').addEventListener('click', (e) => {
                    e.preventDefault();
                    renderPage(i);
                });
            }
            nav.appendChild(li);
        }

        // Next button
        const nextLi = document.createElement('li');
        nextLi.className = `page-item ${page === totalPages ? 'disabled' : ''}`;
        nextLi.innerHTML = `<a class="page-link" href="javascript:void(0)" aria-label="Sau">&raquo;</a>`;
        if (page < totalPages) {
            nextLi.querySelector('a').addEventListener('click', (e) => {
                e.preventDefault();
                renderPage(page + 1);
            });
        }
        nav.appendChild(nextLi);
    }

    renderPage(1);
};
