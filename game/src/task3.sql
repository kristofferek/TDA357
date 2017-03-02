
-- TABELS

-- Countries
CREATE TABLE public.countries (
  name   text   NOT NULL   PRIMARY KEY
);

-- Areas
CREATE TABLE public.areas (
  country   text   NOT NULL   REFERENCES countries(name),
  name   text   NOT NULL   PRIMARY KEY,
  population   integer   NOT NULL   CHECK (population >= 0),
  UNIQUE (country, name)
);

-- Cities
CREATE TABLE public.cities (
  country   text   NOT NULL,
  name   text   NOT NULL,
  visitbonus   numeric   NOT NULL   CHECK (visitbonus >= 0::numeric),
  FOREIGN KEY (country, name) REFERENCES areas(country, name),
  UNIQUE (country, name)
);

--Towns
CREATE TABLE public.towns (
  country   text   NOT NULL,
  name   text   NOT NULL,
  FOREIGN KEY (country, name) REFERENCES areas(country, name),
  UNIQUE (country, name)
);

-- Persons
CREATE TABLE public.persons (
  country   text   NOT NULL   REFERENCES countries(name),
  personnummer   text   NOT NULL   PRIMARY KEY CHECK (personnummer ~ '^\d{8}-\d{4}$'::text OR personnummer = ''::text),
  name   text   NOT NULL,
  locationcountry   text   NOT NULL,
  locationarea   text   NOT NULL,
  budget   numeric   NOT NULL   CHECK (budget >= 0::numeric),
  FOREIGN KEY (locationcountry, locationarea) REFERENCES areas(country, name),
  UNIQUE (country, personnummer)
);

-- Roads
CREATE TABLE public.roads (
  fromcountry   text   NOT NULL,
  fromarea   text   NOT NULL,
  tocountry   text   NOT NULL,
  toarea   text   NOT NULL,
  ownercountry   text,
  ownerpersonnummer   text,
  roadtax   numeric   NOT NULL   CHECK (roadtax >= 0::numeric),
  FOREIGN KEY (ownercountry, ownerpersonnummer) REFERENCES persons(country, personnummer),
  FOREIGN KEY (fromcountry, fromarea) REFERENCES areas(country, name),
  FOREIGN KEY (tocountry, toarea) REFERENCES areas(country, name)
);

-- Hotels
CREATE TABLE public.hotels (
  name   text   NOT NULL,
  locationcountry   text   NOT NULL,
  locationname   text   NOT NULL,
  ownercountry   text   NOT NULL,
  ownerpersonnummer   text   NOT NULL,
  FOREIGN KEY (ownercountry, ownerpersonnummer) REFERENCES persons(country, personnummer),
  FOREIGN KEY (locationcountry, locationname) REFERENCES cities(country, name),
  UNIQUE (locationcountry, locationname, ownercountry, ownerpersonnummer)
);

-- VIEWS

-- assetsummary
CREATE VIEW public.assetsummary AS
  SELECT r1.country, r1.personnummer, r1.name, r1.budget,
    0::numeric + r2.amount::numeric * getval('roadprice'::text) + r3.amount::numeric * getval('hotelprice'::text) AS assets,
    0::numeric + r3.amount::numeric * getval('hotelprice'::text) * getval('hotelrefund'::text) AS reclaimable
  FROM persons r1,
    ( SELECT persons.country, persons.personnummer,
        0 + count(roads.roadtax) AS amount
      FROM persons
        LEFT JOIN roads ON persons.personnummer <> ''::text AND persons.personnummer = roads.ownerpersonnummer AND persons.country = roads.ownercountry
      GROUP BY persons.country, persons.personnummer) r2,
    ( SELECT persons.country, persons.personnummer,
        0 + count(hotels.name) AS amount
      FROM persons
        LEFT JOIN hotels ON persons.personnummer <> ''::text AND persons.personnummer = hotels.ownerpersonnummer AND persons.country = hotels.ownercountry
      GROUP BY persons.country, persons.personnummer) r3
  WHERE r1.personnummer <> ''::text AND r1.country = r2.country AND r1.personnummer = r2.personnummer AND r1.country = r3.country AND r1.personnummer = r3.personnummer
  GROUP BY r1.country, r1.personnummer, r1.name, r2.amount, r3.amount;

-- nextmoves
CREATE VIEW public.nextmoves AS
  SELECT a1.country AS personcountry, a1.personnummer,
         temp4.locationcountry AS country, temp4.locationarea AS area,
         temp4.tocountry AS destcountry, temp4.toarea AS destarea,
         min(temp4.roadtax) AS cost
  FROM persons a1,
    (         SELECT temp2.country, temp2.personnummer, temp2.locationcountry,
                temp2.locationarea, temp2.tocountry, temp2.toarea,
                temp2.roadtax
              FROM ( SELECT persons.country, persons.personnummer,
                       persons.locationcountry, persons.locationarea,
                       temp1.tocountry, temp1.toarea,
                       0::numeric AS roadtax
                     FROM persons
                       JOIN (         SELECT roads.fromcountry, roads.fromarea,
                                        roads.tocountry, roads.toarea,
                                        roads.roadtax, roads.ownercountry,
                                        roads.ownerpersonnummer
                                      FROM roads
                                      UNION
                                      SELECT roads.tocountry AS fromcountry,
                                             roads.toarea AS fromarea,
                                             roads.fromcountry AS tocountry,
                                             roads.fromarea AS toarea,
                                        roads.roadtax, roads.ownercountry,
                                        roads.ownerpersonnummer
                                      FROM roads
                                      GROUP BY roads.fromcountry, roads.fromarea, roads.tocountry, roads.toarea, roads.roadtax, roads.ownercountry, roads.ownerpersonnummer) temp1 ON (persons.personnummer = temp1.ownerpersonnummer OR temp1.ownerpersonnummer = ''::text) AND persons.locationcountry = temp1.fromcountry AND (persons.country = temp1.ownercountry OR temp1.ownercountry = ''::text) AND persons.locationarea = temp1.fromarea) temp2
              UNION
              SELECT temp3.country, temp3.personnummer,
                temp3.locationcountry, temp3.locationarea, temp3.tocountry,
                temp3.toarea, temp3.roadtax
              FROM ( SELECT persons.country, persons.personnummer,
                       persons.locationcountry, persons.locationarea,
                       temp1.tocountry, temp1.toarea, temp1.roadtax
                     FROM persons
                       JOIN (         SELECT roads.fromcountry, roads.fromarea,
                                        roads.tocountry, roads.toarea,
                                        roads.roadtax, roads.ownercountry,
                                        roads.ownerpersonnummer
                                      FROM roads
                                      UNION
                                      SELECT roads.tocountry AS fromcountry,
                                             roads.toarea AS fromarea,
                                             roads.fromcountry AS tocountry,
                                             roads.fromarea AS toarea,
                                        roads.roadtax, roads.ownercountry,
                                        roads.ownerpersonnummer
                                      FROM roads
                                      GROUP BY roads.fromcountry, roads.fromarea, roads.tocountry, roads.toarea, roads.roadtax, roads.ownercountry, roads.ownerpersonnummer) temp1 ON persons.locationcountry = temp1.fromcountry AND persons.locationarea = temp1.fromarea AND persons.personnummer <> ''::text) temp3) temp4
  WHERE a1.country = temp4.country AND a1.personnummer = temp4.personnummer AND a1.locationcountry = temp4.locationcountry AND a1.locationarea = temp4.locationarea
  GROUP BY a1.country, a1.personnummer, temp4.locationcountry, temp4.locationarea, temp4.tocountry, temp4.toarea
  ORDER BY a1.country, a1.personnummer;

-- FUNCTIONS

CREATE FUNCTION public.city_check() RETURNS trigger AS $$

BEGIN
  IF EXISTS (SELECT * FROM towns WHERE country = NEW.country AND name = NEW.name) THEN
    RAISE EXCEPTION 'The area is already a Town';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public.town_check() RETURNS trigger AS $$

BEGIN
  IF EXISTS (SELECT * FROM cities WHERE country = NEW.country AND name = NEW.name) THEN
    RAISE EXCEPTION 'The area is already a City';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public.buyhotel() RETURNS trigger AS $$

BEGIN
  IF EXISTS (SELECT * FROM hotels WHERE ownerpersonnummer = NEW.ownerpersonnummer
                                        AND ownercountry = NEW.ownercountry
                                        AND locationcountry = NEW.locationcountry
                                        AND locationname = NEW.locationname) THEN
    RAISE EXCEPTION 'A person can only own one hotel in one city';
    RETURN NULL;
  END IF;

  IF EXISTS (SELECT * FROM cities WHERE country = NEW.locationcountry AND name = NEW.locationname) THEN
    UPDATE persons
    SET budget = budget - getval('hotelprice')
    WHERE personnummer = NEW.ownerpersonnummer AND country = NEW.ownercountry;
    RETURN NEW;
  END IF;
  RAISE EXCEPTION 'Hotels can only be built in a city';
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public."create_road_check"() RETURNS trigger AS $$
BEGIN
  PERFORM * FROM roads p WHERE NEW.fromcountry = p.tocountry
                               AND NEW.tocountry = p.fromcountry
                               AND NEW.fromarea = p.toarea
                               AND NEW.toarea = p.fromarea
                               AND NEW.ownercountry = p.ownercountry
                               AND NEW.ownerpersonnummer = p.ownerpersonnummer;
  IF FOUND THEN
    RAISE EXCEPTION 'INSERT failed: A person can only own one road between two areas';
  END IF;

  PERFORM * FROM roads p WHERE NEW.tocountry = p.tocountry
                               AND NEW.fromcountry = p.fromcountry
                               AND NEW.toarea = p.toarea
                               AND NEW.fromarea = p.fromarea
                               AND NEW.ownercountry = p.ownercountry
                               AND NEW.ownerpersonnummer = p.ownerpersonnummer;
  IF FOUND THEN
    RAISE EXCEPTION 'INSERT failed: A person can only own one road between two areas';
  END IF;

  IF NEW.fromcountry=NEW.tocountry AND NEW.fromarea=NEW.toarea THEN
    RAISE EXCEPTION 'INSERT failed: Can not create road between one area';
  END IF;

  PERFORM * FROM persons WHERE NEW.ownerpersonnummer = personnummer
                               AND NEW.fromcountry = locationcountry
                               AND NEW.fromarea = locationarea
                               OR NEW.ownerpersonnummer = '';
  IF FOUND THEN
    UPDATE persons SET budget = budget-getval('roadprice') WHERE personnummer = NEW.ownerpersonnummer;
    RETURN NEW;
  END IF;

  PERFORM * FROM persons WHERE NEW.ownerpersonnummer = personnummer
                               AND NEW.tocountry = locationcountry
                               AND NEW.toarea = locationarea;
  IF FOUND THEN
    UPDATE persons SET budget = budget-getval('roadprice') WHERE personnummer = NEW.ownerpersonnummer;
    RETURN NEW;
  END IF;
  RAISE EXCEPTION 'INSERT failed: person has to be located in one of the areas';
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public."gov_noupdate"() RETURNS trigger AS $$
BEGIN
  IF OLD.personnummer = '' THEN
    RETURN NULL;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public."person_move"() RETURNS trigger AS $$
DECLARE
  hotelowners INTEGER;
  visitmoney NUMERIC;
  roadcost NUMERIC;
BEGIN

  IF NOT EXISTS (SELECT * FROM nextmoves WHERE destcountry = NEW.locationcountry
                                               AND destarea = NEW.locationarea
                                               AND personcountry = NEW.country
                                               AND personnummer = NEW.personnummer) THEN
    RAISE EXCEPTION 'There is no road between the areas';
  END IF;
  roadcost:= (SELECT cost FROM nextmoves WHERE destcountry = NEW.locationcountry
                                               AND destarea = NEW.locationarea
                                               AND personcountry = NEW.country
                                               AND personnummer = NEW.personnummer);
  IF NOT (roadcost = 0) THEN
    NEW.budget = NEW.budget - roadcost;
    IF (NEW.budget < 0) THEN
      RAISE EXCEPTION 'No money';
    END IF;

    UPDATE persons
    SET budget = budget + roadcost
    WHERE personnummer = (SELECT ownerpersonnummer
                          FROM (SELECT roads.fromcountry, roads.fromarea, roads.tocountry,
                                  roads.toarea, roads.roadtax, roads.ownercountry,
                                  roads.ownerpersonnummer
                                FROM roads
                                UNION
                                SELECT roads.tocountry AS fromcountry,
                                       roads.toarea AS fromarea,
                                       roads.fromcountry AS tocountry,
                                       roads.fromarea AS toarea, roads.roadtax , roads.ownercountry,
                                  roads.ownerpersonnummer
                                FROM roads
                                GROUP BY roads.fromcountry, roads.fromarea, roads.tocountry, roads.toarea, roads.roadtax, roads.ownercountry,
                                  roads.ownerpersonnummer) AS totalroads
                          WHERE fromcountry = OLD.locationcountry
                                AND fromarea = OLD.locationarea
                                AND tocountry = NEW.locationcountry
                                AND toarea = NEW.locationarea
                          ORDER BY roadtax, ownercountry, ownerpersonnummer DESC LIMIT 1)
          AND country = (SELECT ownercountry
                         FROM (SELECT roads.fromcountry, roads.fromarea, roads.tocountry,
                                 roads.toarea, roads.roadtax, roads.ownercountry,
                                 roads.ownerpersonnummer
                               FROM roads
                               UNION
                               SELECT roads.tocountry AS fromcountry,
                                      roads.toarea AS fromarea,
                                      roads.fromcountry AS tocountry,
                                      roads.fromarea AS toarea, roads.roadtax , roads.ownercountry,
                                 roads.ownerpersonnummer
                               FROM roads
                               GROUP BY roads.fromcountry, roads.fromarea, roads.tocountry, roads.toarea, roads.roadtax, roads.ownercountry,
                                 roads.ownerpersonnummer) AS totalroads
                         WHERE fromcountry = OLD.locationcountry
                               AND fromarea = OLD.locationarea
                               AND tocountry = NEW.locationcountry
                               AND toarea = NEW.locationarea
                         ORDER BY roadtax, ownercountry, ownerpersonnummer DESC LIMIT 1);
  END IF;

  IF EXISTS (SELECT * FROM cities WHERE country=NEW.locationcountry AND name=NEW.locationarea) THEN
    IF EXISTS (SELECT * FROM hotels WHERE locationcountry = NEW.locationcountry AND locationname = NEW.locationarea) THEN
      NEW.budget = NEW.budget - getval('cityvisit');


      hotelowners:=(SELECT Count(ownerpersonnummer) FROM hotels WHERE locationcountry=NEW.locationcountry
                                                                      AND locationname=NEW.locationarea);

      IF EXISTS(SELECT * FROM hotels WHERE locationcountry=NEW.locationcountry
                                                 AND locationname=NEW.locationarea
                                                 AND ownercountry = NEW.country
                                                 AND ownerpersonnummer = NEW.personnummer)THEN
              --The traveling person have a hotel in the city.
              NEW.budget = NEW.budget + getval('cityvisit')/hotelowners;
            END IF;

      FOR i IN 0..hotelowners LOOP
         UPDATE persons
               SET budget =  budget+ getval('cityvisit')/hotelowners
               WHERE country = (SELECT A1.country
                                       FROM (SELECT country, personnummer FROM persons WHERE country <> NEW.country OR personnummer <> NEW.personnummer) as A1
                                       JOIN
                                       hotels ON A1.country = hotels.ownercountry AND A1.personnummer = hotels.ownerpersonnummer
                                       AND hotels.locationcountry = NEW.locationcountry AND hotels.locationname = NEW.locationarea
                                ORDER BY A1.country, A1.personnummer LIMIT 1 OFFSET i)
                     AND personnummer = (SELECT A1.personnummer
                                       FROM (SELECT country, personnummer FROM persons WHERE country <> NEW.country OR personnummer <> NEW.personnummer) as A1
                                       JOIN
                                       hotels ON A1.country = hotels.ownercountry AND A1.personnummer = hotels.ownerpersonnummer
                                       AND hotels.locationcountry = NEW.locationcountry AND hotels.locationname = NEW.locationarea
                                       ORDER BY A1.country, A1.personnummer LIMIT 1 OFFSET i);
         END LOOP;
    END IF;
    visitmoney := (SELECT visitbonus FROM cities WHERE country=NEW.locationcountry AND name=NEW.locationarea);
    NEW.budget = NEW.budget + visitmoney;

    UPDATE cities
    SET visitbonus = 0
    WHERE country = NEW.locationcountry AND name = NEW.locationarea;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public.sellhotel() RETURNS trigger AS $$

BEGIN
  UPDATE persons
  SET budget = budget + (getval('hotelrefund') * getval('hotelprice'))
  WHERE country = OLD.ownercountry AND personnummer = OLD.ownerpersonnummer;
  RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION public."update_road_check"() RETURNS trigger AS $$
BEGIN
  IF NEW.fromarea <> OLD.fromarea
     OR NEW.fromcountry <> OLD.fromcountry
     OR NEW.toarea <> OLD.toarea
     OR NEW.tocountry <> OLD.tocountry
     OR NEW.ownercountry <> OLD.ownercountry
     OR NEW.ownerpersonnummer <> OLD.ownerpersonnummer
  THEN
    RAISE EXCEPTION 'UPDATE failed: Can not update other fields than roadtax';
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--

CREATE FUNCTION public.updatehotel() RETURNS trigger AS $$

BEGIN
  IF (NEW.locationcountry <> OLD.locationcountry OR NEW.locationname <> OLD.locationname) THEN
    RAISE EXCEPTION 'Can not change the location of a hotel';
  END IF;
  IF EXISTS (SELECT * FROM hotels WHERE ownerpersonnummer = NEW.ownerpersonnummer
                                        AND ownercountry = NEW.ownercountry
                                        AND locationcountry = NEW.locationcountry
                                        AND locationname = NEW.locationname) THEN
    RAISE EXCEPTION 'A person can only own one hotel in a city';
  END IF;

  IF (NEW.ownercountry <> OLD.ownercountry OR NEW.ownerpersonnummer <> OLD.ownerpersonnummer) THEN
    --Seller
    UPDATE persons
    SET budget = budget + (getval('hotelrefund')*getval('hotelprice'))
    WHERE country = OLD.ownercountry AND personnummer = OLD.ownerpersonnummer;

    --Buyer
    UPDATE persons
    SET budget = budget - getval('hotelprice')
    WHERE country = NEW.ownercountry AND personnummer = NEW.ownerpersonnummer;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGERS

-- Cities
CREATE TRIGGER city_check BEFORE INSERT OR UPDATE ON cities FOR EACH ROW EXECUTE PROCEDURE city_check();

--Towns
CREATE TRIGGER town_check BEFORE INSERT OR UPDATE ON towns FOR EACH ROW EXECUTE PROCEDURE town_check();

-- Hotels
CREATE TRIGGER buy_hotel BEFORE INSERT ON hotels FOR EACH ROW EXECUTE PROCEDURE buyhotel();
CREATE TRIGGER delete_hotel BEFORE DELETE ON hotels FOR EACH ROW EXECUTE PROCEDURE sellhotel();
CREATE TRIGGER update_check BEFORE UPDATE ON hotels FOR EACH ROW EXECUTE PROCEDURE updatehotel();

-- Persons
CREATE TRIGGER gov_noupdate BEFORE UPDATE ON persons FOR EACH ROW EXECUTE PROCEDURE gov_noupdate();
CREATE TRIGGER person_move BEFORE UPDATE OF locationcountry, locationarea ON persons FOR EACH ROW EXECUTE PROCEDURE person_move();

-- Roads
CREATE TRIGGER no_revers_roads BEFORE INSERT ON roads FOR EACH ROW EXECUTE PROCEDURE create_road_check();
CREATE TRIGGER update_road_check BEFORE UPDATE ON roads FOR EACH ROW EXECUTE PROCEDURE update_road_check();