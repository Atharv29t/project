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

    const timer = document.querySelector('.question-timer');
    const answerForm = document.querySelector('.answer-form');
    if (timer && answerForm) {
        let seconds = Number(timer.dataset.seconds);
        const output = timer.querySelector('#timerSeconds');
        const timeoutSubmit = answerForm.querySelector('[data-timeout-submit]');
        const tick = () => {
            if (output) {
                output.textContent = String(seconds);
            }
            if (seconds <= 0) {
                timer.classList.add('time-expired');
                timeoutSubmit?.click();
                return;
            }
            seconds -= 1;
            window.setTimeout(tick, 1000);
        };
        window.setTimeout(tick, 1000);
    }

    const voiceStatus = document.querySelector('.voice-status');
    const questionText = document.querySelector('.question-card h1')?.textContent?.trim() ?? '';
    document.querySelector('[data-voice-read]')?.addEventListener('click', () => {
        if (!('speechSynthesis' in window)) {
            if (voiceStatus) voiceStatus.textContent = 'Speech playback is not supported in this browser.';
            return;
        }
        window.speechSynthesis.cancel();
        const choices = [...document.querySelectorAll('.option-text')].map((item) => item.textContent.trim());
        const utterance = new SpeechSynthesisUtterance(`${questionText}. Options: ${choices.join('. ')}`);
        window.speechSynthesis.speak(utterance);
    });

    document.querySelector('[data-voice-answer]')?.addEventListener('click', () => {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            if (voiceStatus) voiceStatus.textContent = 'Voice answers are not supported in this browser.';
            return;
        }
        const recognition = new SpeechRecognition();
        recognition.lang = 'en-US';
        recognition.interimResults = false;
        recognition.maxAlternatives = 3;
        if (voiceStatus) voiceStatus.textContent = 'Listening…';
        recognition.addEventListener('result', (event) => {
            const transcripts = [];
            for (let index = 0; index < event.results[0].length; index++) {
                transcripts.push(normaliseSpeech(event.results[0][index].transcript));
            }
            const options = [...document.querySelectorAll('.option')];
            const match = options.find((option) => {
                const text = normaliseSpeech(option.querySelector('.option-text')?.textContent ?? '');
                return transcripts.some((transcript) => transcript.length > 0
                    && (transcript === text || transcript.includes(text) || text.includes(transcript)));
            });
            if (match) {
                const input = match.querySelector('input[type=radio]');
                input.checked = true;
                input.dispatchEvent(new Event('change', { bubbles: true }));
                if (voiceStatus) voiceStatus.textContent = 'Selected: ' + match.querySelector('.option-text').textContent.trim();
            } else if (voiceStatus) {
                voiceStatus.textContent = 'Could not match that answer. Please try again or choose an option.';
            }
        });
        recognition.addEventListener('error', (event) => {
            if (voiceStatus) voiceStatus.textContent = `Voice input error: ${event.error}.`;
        });
        recognition.addEventListener('end', () => {
            if (voiceStatus?.textContent === 'Listening…') voiceStatus.textContent = 'No answer heard. Try again.';
        });
        recognition.start();
    });

    const roomLobby = document.querySelector('[data-room-code]');
    if (roomLobby) {
        const code = roomLobby.dataset.roomCode;
        const token = roomLobby.dataset.roomToken;
        const contextPath = roomLobby.dataset.roomContext;
        const status = roomLobby.querySelector('[data-room-connection]');
        const players = roomLobby.querySelector('[data-room-players]');
        const quizForm = roomLobby.querySelector('[data-room-quiz-form]');
        const hostStartButton = roomLobby.querySelector('form[action$="/room/start"] button');
        let launchRequested = false;

        const launchRoomQuiz = (room) => {
            roomLobby.dataset.roomStarted = String(room.started);
            if (room.started && !launchRequested && roomLobby.dataset.roomActive !== 'true'
                && roomLobby.dataset.roomFinished !== 'true' && quizForm) {
                launchRequested = true;
                quizForm.requestSubmit();
            }
        };

        const socketUrl = new URL(`${contextPath}/ws/room/${encodeURIComponent(code)}/${encodeURIComponent(token)}`, window.location.href);
        socketUrl.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const socket = new WebSocket(socketUrl);
        socket.addEventListener('open', () => {
            if (status) status.textContent = 'Connected · live updates on';
        });
        socket.addEventListener('message', (message) => {
            let update;
            try {
                update = JSON.parse(message.data);
            } catch {
                if (status) status.textContent = 'Received an invalid room update.';
                return;
            }
            if (!update.room) return;
            if (players) {
                players.replaceChildren();
                update.room.players.forEach((player) => {
                    const row = document.createElement('li');
                    const name = document.createElement('span');
                    const score = document.createElement('strong');
                    name.textContent = player.name;
                    score.textContent = player.finished
                        ? `${player.points} pts · ${player.percentage}%`
                        : `${player.answered}/${player.total} · Playing`;
                    row.append(name, score);
                    players.append(row);
                });
            }
            if (hostStartButton && roomLobby.dataset.roomHost === 'true') {
                hostStartButton.disabled = update.room.started || update.room.players.length < 2;
            }
            launchRoomQuiz(update.room);
        });
        socket.addEventListener('close', () => {
            if (status) status.textContent = 'Live connection closed · refresh to reconnect';
        });
        socket.addEventListener('error', () => {
            if (status) status.textContent = 'Could not connect to live updates.';
        });

        roomLobby.querySelector('[data-copy-room]')?.addEventListener('click', async (event) => {
            const button = event.currentTarget;
            try {
                await navigator.clipboard.writeText(code);
                button.textContent = 'Code copied';
            } catch {
                if (status) status.textContent = `Share room code ${code} with your friends.`;
            }
        });

        if (roomLobby.dataset.roomStarted === 'true') {
            launchRoomQuiz({ started: true });
        }
    }

    // Count the final percentage up once on the result page.
    const scoreElement = document.querySelector('.score-percent');
    if (scoreElement && !reducedMotion) {
        countUp(scoreElement);
    }
});

function normaliseSpeech(value) {
    return value.toLocaleLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
}

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
