-- SEVERAL OF THE FOLLOWING STATEMENTS SHOULD FAIL
BEGIN;
-- COUNTRIES
INSERT INTO countries VALUES ('Denmark');
INSERT INTO countries VALUES ('Denmark'); -- FAIL
INSERT INTO countries VALUES ('Japan');

-- AREAS
INSERT INTO areas VALUES ('Denmark', 'Köpenhamn', -100); -- FAIL
INSERT INTO areas VALUES ('Russia', 'Köpenhamn', 1000000); -- FAIL

INSERT INTO areas VALUES ('Denmark', 'Köpenhamn', 1000000);
INSERT INTO areas VALUES ('Japan', 'Tokyo', 20000000);
INSERT INTO areas VALUES ('Denmark', 'Åhus', 100000);

-- CITIES
INSERT INTO cities VALUES ('Denmark', 'Köpenhamn', -100); -- FAIL

INSERT INTO cities VALUES ('Denmark', 'Köpenhamn', 20000);
INSERT INTO cities VALUES ('Japan', 'Tokyo', 50);

-- TOWNS
INSERT INTO towns VALUES ('Denmark', 'Åhus');
INSERT INTO cities VALUES ('Denmark', 'Åhus'); -- FAIL
INSERT INTO towns VALUES ('Denmark', 'Köpenhamn'); -- FAIL

-- PERSONS
INSERT INTO persons VALUES ('Denmark', '199702039906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', 5000000); -- FAIL
INSERT INTO persons VALUES ('Denmark', '0123456-9906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', 5000000); -- FAIL
INSERT INTO persons VALUES ('Denmark', '19970203-9906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', -100); -- FAIL
INSERT INTO persons VALUES ('Denmark', '19970203-9906', 'Alex Sundbäck', 'Denmark', 'New York', 5000000); -- FAIL
INSERT INTO persons VALUES ('Finland', '19970203-9906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', 5000000); -- FAIL

INSERT INTO persons VALUES ('Denmark', '19860404-4040', 'Claes Magnusson', 'Denmark', 'Köpenhamn', 400000);
INSERT INTO persons VALUES ('Denmark', '19970203-9906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', 50);
INSERT INTO persons VALUES ('Denmark', '19770726-0505', 'Andreas Malm', 'Denmark', 'Åhus', 5000000);

-- ROADS
INSERT INTO roads VALUES ('Denmark', 'Köpenhamn', 'Japan', 'Tokyo', 'Denmark', '19970203-9906', 7000); -- FAIL
INSERT INTO roads VALUES ('Denmark', 'Köpenhamn', 'Japan', 'Tokyo', 'Denmark', '19770726-0505', 7000); -- FAIL

INSERT INTO roads VALUES ('Denmark', 'Köpenhamn', 'Japan', 'Tokyo', 'Denmark', '19860404-4040', 7000);
INSERT INTO roads VALUES ('Denmark', 'Köpenhamn', 'Japan', 'Tokyo', 'Denmark', '19860404-4040', 7000); -- FAIL
UPDATE roads SET ownerpersonnummer='19770726-0505' WHERE tocountry = 'Japan'; -- FAIL

-- Hotels
INSERT INTO hotels VALUES ('Hilton', 'Denmark', 'Köpenhamn', 'Denmark', '19860404-4040');

INSERT INTO hotels VALUES ('Hilton 2', 'Denmark', 'Köpenhamn', 'Denmark', '19860404-4040'); -- FAIL
INSERT INTO hotels VALUES ('Hilton 2', 'Denmark', 'Åhus', 'Denmark', '19860404-4040'); -- FAIL
UPDATE hotels SET locationcountry='Japan', locationname='Tokyo' WHERE name = 'Hilton'; -- FAIL

ROLLBACK;