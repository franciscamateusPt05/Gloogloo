document.addEventListener('DOMContentLoaded', function() {
    const searchButton = document.querySelector('.button:first-child');
    const searchInput = document.querySelector('input[name="input"]');
    const sound = document.getElementById('clickSound');

    if (sound) sound.volume = 0.5;

    searchButton.addEventListener('click', function(event) {
        event.preventDefault();

        const inputValue = searchInput.value.trim();
        if (!inputValue) {
            alert("Please, insert a search terms.");
            return;
        }

        if (sound) {
            sound.currentTime = 0;
            sound.play().catch(function(error) {
                console.error("Erro ao tocar som:", error);
            });

            sound.addEventListener('ended', function redirectAfterSound() {
                // Redireciona para o controlador Spring
                window.location.href = `/index?input=${encodeURIComponent(inputValue)}&page=1`;
                sound.removeEventListener('ended', redirectAfterSound); // Evita múltiplos triggers
            });
        } else {
            // Se o som não existir, redireciona logo
            window.location.href = `/index?input=${encodeURIComponent(inputValue)}&page=1`;
        }
    });
});
