-- Migration V3: Seed demo driver and vehicle for development and testing

INSERT INTO vehicles (id, driver_id, vehicle_type, plate, make, model, model_year, qualification_status, driver_version, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'd0000000-0000-0000-0000-000000000001',
    'CAR',
    'AB123CD',
    'Toyota',
    'Corolla',
    2022,
    'APPROVED',
    1,
    NOW()
) ON CONFLICT (id) DO NOTHING;
