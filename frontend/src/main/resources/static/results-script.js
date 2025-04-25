document.addEventListener('DOMContentLoaded', function() {
    // Paginação
    const pageLinks = document.querySelectorAll('.page-link');

    pageLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();

            // Remove a classe 'active' de todos os links
            pageLinks.forEach(l => l.classList.remove('active'));

            // Adiciona a classe 'active' apenas ao link clicado
            if(!this.classList.contains('first-page') &&
                !this.classList.contains('prev-page') &&
                !this.classList.contains('next-page') &&
                !this.classList.contains('last-page')) {
                this.classList.add('active');
            }

            // Aqui você pode adicionar lógica para carregar os resultados da página
            console.log('Ir para página:', this.textContent);
        });
    });

    // Barra de pesquisa
    const searchInput = document.getElementById('search-input');
    const searchButton = document.querySelector('.search-button');

    searchButton.addEventListener('click', function() {
        if(searchInput.value.trim() !== '') {
            // Aqui você pode adicionar lógica para nova busca
            console.log('Nova busca:', searchInput.value);
        }
    });

    // Permite busca ao pressionar Enter
    searchInput.addEventListener('keypress', function(e) {
        if(e.key === 'Enter' && this.value.trim() !== '') {
            console.log('Nova busca (Enter):', this.value);
        }
    });
});