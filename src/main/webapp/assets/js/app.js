/* Quizly: small progressive enhancements. All quiz logic stays on the server. */

document.addEventListener('DOMContentLoaded', () => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Confirm leaving an unfinished quiz.
    document.querySelectorAll('a[data-confirm]').forEach((link) => {
        link.addEventListener('click', (event) => {
            if (!window.confirm(link.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });

    // Remember the player's name on this device. The server still validates it.
    const nameField = document.querySelector('#playerName');
    if (nameField) {
        const storedName = window.localStorage.getItem('quizly:playerName');
        if (!nameField.value && storedName) {
            nameField.value = storedName;
        }

        const quizPicker = document.querySelector('#quizPicker');
        quizPicker?.addEventListener('submit', () => {
            const name = nameField.value.trim();
            if (name.length >= 1 && name.length <= 24) {
                window.localStorage.setItem('quizly:playerName', name);
            }
        });
    }

    // Keep the selected option highlighted in browsers without :has() support.
    const optionInputs = document.querySelectorAll('.option input[type=radio]');
    optionInputs.forEach((input) => {
        input.addEventListener('change', () => {
            document.querySelectorAll('.option').forEach((option) => {
                option.classList.toggle('selected', option.contains(input) && input.checked);
            });
        });
    });

    // Count the final percentage up once on the result page.
    const scoreElement = document.querySelector('.score-percent');
    if (scoreElement && !reducedMotion) {
        countUp(scoreElement);
    }
});

function countUp(element) {
    const target = parseInt(element.textContent, 10);
    if (Number.isNaN(target)) {
        return;
    }

    const duration = 900;
    const startAt = performance.now();

    const tick = (now) => {
        const progress = Math.min(1, (now - startAt) / duration);
        const eased = 1 - Math.pow(1 - progress, 3);
        element.textContent = `${Math.round(target * eased)}%`;
        if (progress < 1) {
            window.requestAnimationFrame(tick);
        }
    };

    window.requestAnimationFrame(tick);
}
