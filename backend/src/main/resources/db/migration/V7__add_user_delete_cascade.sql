ALTER TABLE auth_sessions
DROP CONSTRAINT fkpu507182mdfutajr71rgk67l;
ALTER TABLE auth_sessions
    ADD CONSTRAINT fkpu507182mdfutajr71rgk67l
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE social_accounts
DROP CONSTRAINT fk6rmxxiton5yuvu7ph2hcq2xn7;
ALTER TABLE social_accounts
    ADD CONSTRAINT fk6rmxxiton5yuvu7ph2hcq2xn7
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE push_tokens
DROP CONSTRAINT fkgisqbur2nbpemhidpyqv501nd;
ALTER TABLE push_tokens
    ADD CONSTRAINT fkgisqbur2nbpemhidpyqv501nd
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
