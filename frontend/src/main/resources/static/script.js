document.addEventListener('DOMContentLoaded', function() {
    const searchButton = document.querySelector('.button:first-child'); // Seleciona o primeiro botão
    const sound = document.getElementById('clickSound');

    // Configura o volume (opcional)
    if (sound) {
        sound.volume = 0.5; // Volume médio (0.0 a 1.0)
    }

    // Toca o som quando clica no botão
    searchButton.addEventListener('click', function(event) {
        event.preventDefault(); // Impede o comportamento padrão do botão (se houver)

        if (sound) {
            sound.currentTime = 0; // Reinicia o som se já estiver tocando
            sound.play().catch(function(error) {
                console.error("Erro ao tocar som:", error);
                // Mostra alerta se o navegador bloquear
                if (error.name === 'NotAllowedError') {
                    alert("Por favor, permita sons para uma experiência completa!");
                }
            });

            // Redireciona após o áudio terminar
            sound.addEventListener('ended', function() {
                window.location.href = 'templates/result-search.html'; // Substitua pelo link desejado
            });
        }
    });
});
