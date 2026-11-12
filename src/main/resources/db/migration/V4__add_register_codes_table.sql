CREATE TABLE registration_codes (
                                    register_code VARCHAR(255) PRIMARY KEY,
                                    code_role VARCHAR(30) NOT NULL,
                                    status VARCHAR(30) NOT NULL,
                                    generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_registration_codes_code_role ON registration_codes (register_code, code_role);
CREATE INDEX idx_registration_codes_generated_at ON registration_codes (generated_at);
CREATE INDEX idx_registration_codes_code_role_status
    ON registration_codes (register_code, code_role, status);

