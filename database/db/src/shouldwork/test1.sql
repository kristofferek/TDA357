
BEGIN;


-- Countries and areas
INSERT INTO countries VALUES ('Denmark');

INSERT INTO areas VALUES ('Denmark', 'Köpenhamn', 1000000);
INSERT INTO areas VALUES ('Denmark', 'Åhus', 100000);
UPDATE areas SET population = 97000 WHERE name = 'Åhus';
SELECT assert((SELECT population FROM areas WHERE name = 'Åhus'), 97000);

INSERT INTO cities VALUES ('Denmark', 'Köpenhamn', 500.32);
SELECT assert((SELECT visitbonus FROM cities WHERE name = 'Köpenhamn'), 500.32);

UPDATE cities SET visitbonus=700.50 WHERE name='Köpenhamn';
SELECT assert((SELECT visitbonus FROM cities WHERE name = 'Köpenhamn'), 700.50);

INSERT INTO towns VALUES ('Denmark', 'Åhus');
DELETE FROM towns WHERE name='Åhus';
-- Countries and areas test done

-- Persons and roads
INSERT INTO persons VALUES ('Denmark', '19970203-9906', 'Alex Sundbäck', 'Denmark', 'Köpenhamn', 5000000);
INSERT INTO persons VALUES ('Denmark', '19860404-4040', 'Claes Magnusson', 'Denmark', 'Åhus', 400000);

INSERT INTO roads VALUES ('Denmark', 'Köpenhamn', 'Denmark', 'Åhus', 'Denmark', '19970203-9906', 7000);
SELECT assert((SELECT budget FROM persons WHERE name='Alex Sundbäck'), (5000000-456.9));

UPDATE persons SET locationarea = 'Köpenhamn' WHERE name = 'Claes Magnusson';
SELECT assert ((SELECT budget FROM persons WHERE name = 'Claes Magnusson'), (400000-7000));
SELECT assert ((SELECT budget FROM persons WHERE name = 'Alex Sundbäck'), ((5000000-456.9)+7000));
-- Persons and roads test done

-- Hotels
--Reset budgets
UPDATE persons SET budget = 300000 WHERE name = 'Claes Magnusson';
UPDATE persons SET budget = 300000 WHERE name = 'Alex Sundbäck';

INSERT INTO hotels VALUES ('Hilton', 'Denmark', 'Köpenhamn', 'Denmark', '19860404-4040');

SELECT assert((SELECT budget FROM persons WHERE name='Claes Magnusson'), (300000-789.2));


UPDATE hotels SET ownerpersonnummer = '19970203-9906' WHERE name = 'Hilton';

SELECT assert((SELECT budget FROM persons WHERE name='Claes Magnusson'), ((300000-789.2) + (0.5*789.2)));
SELECT assert((SELECT budget FROM persons WHERE name='Alex Sundbäck'), (300000-789.2));
-- Hotels test done

ROLLBACK;