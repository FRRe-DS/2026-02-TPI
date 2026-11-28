-- Prueba de integración aislada: ningún dato sintético queda guardado.
BEGIN;
DO $$
DECLARE
  d uuid := gen_random_uuid();
  v uuid := gen_random_uuid();
  l uuid := gen_random_uuid();
  baseline bigint;
  actual bigint;
BEGIN
  SELECT value INTO baseline FROM observability.available_drivers;
  INSERT INTO drivers(id, identity_subject, display_name, qualification_status)
    VALUES(d, 'otel-test-' || d, 'Observability test', 'APPROVED');
  INSERT INTO vehicles(id,driver_id,service_types,plate,make,model,qualification_status)
    VALUES(v,d,'AUTO','OTEL-TEST','Test','Test','APPROVED');
  INSERT INTO documents(id,driver_id,document_type,document_number,issued_at,valid_until,status)
    VALUES(l,d,'LICENSE','TEST',now()-interval '1 day',now()+interval '1 day','APPROVED');
  INSERT INTO document_service_types(document_id,service_type) VALUES(l,'AUTO');
  INSERT INTO documents(id,driver_id,vehicle_id,document_type,document_number,issued_at,valid_until,status)
    SELECT gen_random_uuid(),d,v,t,'TEST',now()-interval '1 day',now()+interval '1 day','APPROVED'
    FROM unnest(ARRAY['INSURANCE','VEHICLE_REGISTRATION']) t;
  INSERT INTO availability_intents(id,driver_id,vehicle_id,connected,available,status,desired_version,applied_version)
    VALUES(gen_random_uuid(),d,v,true,true,'APPLIED',1,1);
  SELECT value INTO actual FROM observability.available_drivers;
  IF actual <> baseline+1 THEN RAISE EXCEPTION 'Conductor apto no contado'; END IF;

  UPDATE availability_intents SET status='PENDING' WHERE driver_id=d;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó PENDING'; END IF;
  UPDATE availability_intents SET status='APPLIED',desired_version=2 WHERE driver_id=d;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó versión no aplicada'; END IF;
  UPDATE availability_intents SET desired_version=1,available=false WHERE driver_id=d;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó no disponible'; END IF;
  UPDATE availability_intents SET available=true WHERE driver_id=d;
  UPDATE documents SET valid_until=now()-interval '1 hour' WHERE id=l;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó licencia vencida'; END IF;
  UPDATE documents SET valid_until=now()+interval '1 day' WHERE id=l;
  UPDATE vehicles SET qualification_status='SUSPENDED' WHERE id=v;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó vehículo suspendido'; END IF;
  UPDATE vehicles SET qualification_status='APPROVED' WHERE id=v;
  UPDATE drivers SET qualification_status='SUSPENDED' WHERE id=d;
  IF (SELECT value FROM observability.available_drivers) <> baseline THEN RAISE EXCEPTION 'Incluyó conductor suspendido'; END IF;

  IF NOT has_table_privilege('m3_otel','observability.available_drivers','SELECT')
      OR has_table_privilege('m3_otel','public.drivers','SELECT')
      OR has_table_privilege('m3_otel','public.drivers','INSERT')
      OR has_table_privilege('m3_otel','observability.available_drivers','UPDATE')
  THEN RAISE EXCEPTION 'Permisos de observabilidad incorrectos'; END IF;
END $$;
ROLLBACK;
