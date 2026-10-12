-- Datos sintéticos para desarrollo. No modifica perfiles existentes.
BEGIN;
CREATE TEMP TABLE seed_profiles (id UUID, subject TEXT, name TEXT, state TEXT, make TEXT, model TEXT, plate TEXT, type TEXT) ON COMMIT DROP;
INSERT INTO seed_profiles VALUES
('e0000000-0000-0000-0000-000000000001','user-sub-123','Lucía Fernández · Ejemplo','APPROVED','Toyota','Etios','ZZ001AA','AUTO'),
('e0000000-0000-0000-0000-000000000002','ejemplo-mateo','Mateo Gómez · Ejemplo','PENDING','Fiat','Cronos','ZZ002AA','AUTO'),
('e0000000-0000-0000-0000-000000000003','ejemplo-sofia','Sofía Benítez · Ejemplo','OBSERVED','Honda','Wave','ZZ003AA','MOTO'),
('e0000000-0000-0000-0000-000000000004','ejemplo-tomas','Tomás López · Ejemplo','SUSPENDED','Renault','Logan','ZZ004AA','AUTO'),
('e0000000-0000-0000-0000-000000000005','ejemplo-valentina','Valentina Ruiz · Ejemplo','APPROVED','Chevrolet','Onix','ZZ005AA','AUTO');
INSERT INTO drivers(id,identity_subject,display_name,qualification_status,qualification_reason,version)
SELECT id,subject,name,state,'Datos sintéticos de desarrollo',1 FROM seed_profiles ON CONFLICT DO NOTHING;
-- Asociar solamente los UUID reservados que conservan la identidad del ejemplo.
DELETE FROM seed_profiles s WHERE NOT EXISTS (SELECT 1 FROM drivers d WHERE d.id=s.id AND d.identity_subject=s.subject);
INSERT INTO vehicles(id,driver_id,vehicle_type,service_types,plate,make,model,model_year,qualification_status,color,capacity,service_categories)
SELECT md5(s.id::text || ':vehicle')::uuid,s.id,s.type,s.type,s.plate,s.make,s.model,2023,s.state,'Gris',CASE WHEN s.type='MOTO' THEN 1 ELSE 4 END,CASE WHEN s.type='MOTO' THEN 'DELIVERY' ELSE 'STANDARD' END
FROM seed_profiles s ON CONFLICT DO NOTHING;
INSERT INTO vehicle_services(vehicle_id,service_type)
SELECT md5(id::text || ':vehicle')::uuid,type FROM seed_profiles ON CONFLICT DO NOTHING;
INSERT INTO documents(id,driver_id,vehicle_id,document_type,document_number,issuing_entity,issued_at,valid_until,revision,status,is_current,service_types,driver_version)
SELECT md5(s.id::text || ':' || t.kind)::uuid,s.id,
CASE WHEN t.kind='LICENSE' THEN NULL ELSE md5(s.id::text || ':vehicle')::uuid END,t.kind,'EJEMPLO-' || right(s.id::text,4) || '-' || t.kind,'Entidad ficticia de prueba',
CURRENT_TIMESTAMP - INTERVAL '30 days',CURRENT_TIMESTAMP + INTERVAL '180 days',1,
CASE WHEN s.state IN ('APPROVED','SUSPENDED') THEN 'APPROVED' WHEN s.state='OBSERVED' THEN 'OBSERVED' ELSE 'PENDING' END,TRUE,s.type,1
FROM seed_profiles s CROSS JOIN (VALUES ('LICENSE'),('INSURANCE'),('VEHICLE_REGISTRATION')) t(kind)
ON CONFLICT DO NOTHING;
INSERT INTO document_service_types(document_id,service_type)
SELECT md5(id::text || ':LICENSE')::uuid,type FROM seed_profiles ON CONFLICT DO NOTHING;
COMMIT;
