-- Criar o trigger para a tabela 'urls' para atualizar o ranking dos URLs
CREATE OR REPLACE FUNCTION update_ranking()
RETURNS TRIGGER AS $$
BEGIN
    -- Verifica se o URL está completo antes de executar o update
    IF NEW.url IS NOT NULL AND NEW.titulo != '' AND NEW.citacao != '' THEN
UPDATE urls
SET ranking = (
    SELECT COUNT(from_url)
    FROM url_links
    WHERE url_links.to_url = NEW.url
)
WHERE urls.url = NEW.url;
END IF;

RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Criar o trigger 'AFTER INSERT' para acionar após inserção de um URL na tabela 'urls'
CREATE TRIGGER trg_update_ranking
    AFTER INSERT OR UPDATE ON urls
                        FOR EACH ROW
                        EXECUTE FUNCTION update_ranking();
