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
});
