-- Ejecutar como dueño de m3_db después de las migraciones. No cambia tablas de negocio.
CREATE SCHEMA IF NOT EXISTS observability;
CREATE OR REPLACE VIEW observability.available_drivers WITH (security_barrier = true) AS
SELECT count(*)::bigint AS value
FROM public.drivers d
JOIN public.availability_intents a ON a.driver_id = d.id
JOIN public.vehicles v ON v.id = a.vehicle_id AND v.driver_id = d.id
WHERE d.qualification_status = 'APPROVED'
  AND v.qualification_status = 'APPROVED'
  AND a.connected AND a.available AND a.status = 'APPLIED'
  AND a.applied_version = a.desired_version
  -- La aprobación histórica no basta si la documentación ya venció.
  AND EXISTS (
    SELECT 1 FROM public.documents l
    WHERE l.driver_id = d.id AND l.vehicle_id IS NULL AND l.document_type = 'LICENSE'
      AND l.is_current AND l.status = 'APPROVED'
      AND l.issued_at <= CURRENT_TIMESTAMP AND l.valid_until > CURRENT_TIMESTAMP
      AND EXISTS (SELECT 1 FROM public.document_service_types st
                  WHERE st.document_id = l.id
                    AND st.service_type = ANY(string_to_array(v.service_types, ',')))
  )
  AND NOT EXISTS (
    SELECT 1 FROM (VALUES ('INSURANCE'), ('VEHICLE_REGISTRATION')) AS required(document_type)
    WHERE NOT EXISTS (
      SELECT 1 FROM public.documents doc
      WHERE doc.vehicle_id = v.id AND doc.driver_id = d.id
        AND doc.document_type = required.document_type AND doc.is_current
        AND doc.status = 'APPROVED'
        AND doc.issued_at <= CURRENT_TIMESTAMP AND doc.valid_until > CURRENT_TIMESTAMP
    )
  );

REVOKE ALL ON SCHEMA observability FROM PUBLIC;
REVOKE ALL ON observability.available_drivers FROM PUBLIC;
GRANT CONNECT ON DATABASE m3_db TO m3_otel;
GRANT USAGE ON SCHEMA observability TO m3_otel;
GRANT SELECT ON observability.available_drivers TO m3_otel;
ALTER ROLE m3_otel SET default_transaction_read_only = on;
ALTER ROLE m3_otel SET statement_timeout = '5s';
