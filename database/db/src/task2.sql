
-- TABELS

-- countries
CREATE TABLE public.countries (
  name   text   NOT NULL   PRIMARY KEY
);
-- areas
CREATE TABLE public.areas (
  country   text   NOT NULL   REFERENCES countries(name),
  name   text   NOT NULL   PRIMARY KEY,
  population   integer   NOT NULL   CHECK (population >= 0),
  FOREIGN KEY (country) REFERENCES countries(name)
);