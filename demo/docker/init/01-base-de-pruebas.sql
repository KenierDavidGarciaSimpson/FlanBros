-- Se ejecuta SOLO la primera vez que se crea el contenedor (volumen vacio).
-- Crea la base que usan las pruebas de repositorios; asi no tocan los datos de desarrollo.
CREATE DATABASE flanbross_test OWNER flanbross ENCODING 'UTF8';
