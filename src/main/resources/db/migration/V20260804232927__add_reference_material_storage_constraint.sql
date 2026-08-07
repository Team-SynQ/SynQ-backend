ALTER TABLE reference_material
    ADD CONSTRAINT chk_reference_material_storage_location
    CHECK (
        (type = 'FILE' AND storage_key IS NOT NULL AND url IS NULL)
        OR
        (type = 'LINK' AND storage_key IS NULL AND url IS NOT NULL)
    );
