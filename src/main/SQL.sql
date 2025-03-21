-- Criar as bases de dados
CREATE DATABASE barrel1;
CREATE DATABASE barrel2;
CREATE DATABASE queue;

-- Usar Barrel1 e criar as tabelas
\c barrel1;

CREATE TABLE word (
                      word TEXT PRIMARY KEY,
                      top INT NOT NULL DEFAULT 0
);

CREATE TABLE urls (
                      url TEXT PRIMARY KEY,
                      ranking INT NOT NULL DEFAULT 0,
                      titulo TEXT NOT NULL,
                      citacao TEXT NOT NULL
);

CREATE TABLE word_url (
                          word TEXT NOT NULL,
                          url TEXT NOT NULL,
                          frequency INTEGER not null,
                          PRIMARY KEY (word, url),
                          FOREIGN KEY (word) REFERENCES word(word) ON DELETE CASCADE,
                          FOREIGN KEY (url) REFERENCES urls(url) ON DELETE CASCADE
);

CREATE TABLE url_links (
                           from_url TEXT NOT NULL,
                           to_url TEXT NOT NULL,
                           PRIMARY KEY (from_url, to_url),
                           FOREIGN KEY (from_url) REFERENCES urls(url) ON DELETE CASCADE

);

CREATE OR REPLACE FUNCTION update_ranking()
RETURNS TRIGGER AS $$
BEGIN
    -- Verificar se o to_url existe na tabela urls antes de atualizar o ranking
    IF EXISTS (SELECT 1 FROM urls WHERE url = NEW.to_url) THEN
        -- Atualizar o ranking da URL na tabela urls com base nas ligações
UPDATE urls
SET ranking = (
    SELECT COUNT(from_url)
    FROM url_links
    WHERE url_links.to_url = urls.url
)
WHERE urls.url = NEW.to_url;  -- Usando NEW.to_url diretamente
END IF;

RETURN NULL;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER trg_update_ranking
    AFTER INSERT OR DELETE OR UPDATE ON url_links
    FOR EACH ROW
    EXECUTE FUNCTION update_ranking();

-- Criar as mesmas tabelas para Barrel2
\c barrel2;

CREATE TABLE word (
                      word TEXT PRIMARY KEY,
                      top INT NOT NULL DEFAULT 0
);

CREATE TABLE urls (
                      url TEXT PRIMARY KEY,
                      ranking INT NOT NULL DEFAULT 0,
                      titulo TEXT NOT NULL,
                      citacao TEXT NOT NULL
);

CREATE TABLE word_url (
                          word TEXT NOT NULL,
                          url TEXT NOT NULL,
                          frequency INTEGER not null,
                          PRIMARY KEY (word, url),
                          FOREIGN KEY (word) REFERENCES word(word) ON DELETE CASCADE,
                          FOREIGN KEY (url) REFERENCES urls(url) ON DELETE CASCADE
);

CREATE TABLE url_links (
                           from_url TEXT NOT NULL,
                           to_url TEXT NOT NULL,
                           PRIMARY KEY (from_url, to_url),
                           FOREIGN KEY (from_url) REFERENCES urls(url) ON DELETE CASCADE

);

CREATE OR REPLACE FUNCTION update_ranking()
RETURNS TRIGGER AS $$
BEGIN
    -- Verificar se o to_url existe na tabela urls antes de atualizar o ranking
    IF EXISTS (SELECT 1 FROM urls WHERE url = NEW.to_url) THEN
        -- Atualizar o ranking da URL na tabela urls com base nas ligações
UPDATE urls
SET ranking = (
    SELECT COUNT(from_url)
    FROM url_links
    WHERE url_links.to_url = urls.url
)
WHERE urls.url = NEW.to_url;  -- Usando NEW.to_url diretamente
END IF;

RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_ranking
    AFTER INSERT OR DELETE OR UPDATE ON url_links
    FOR EACH ROW
    EXECUTE FUNCTION update_ranking();

-- Criar a tabela para a base de dados Queue
\c queue;

CREATE TABLE queue (
                       url TEXT PRIMARY KEY
);
