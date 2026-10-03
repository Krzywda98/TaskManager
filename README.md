# TaskManager

REST API do zarządzania użytkownikami i zadaniami, zbudowane w Spring Boot. Projekt używa JPA, PostgreSQL, Spring Security i migracji Flyway.

## Wymagania

- JDK 17 lub nowszy.
- Docker Desktop z działającym Docker Compose i kontenerami Linux.

## Uruchomienie lokalnej bazy

W katalogu projektu skopiuj przykładową konfigurację:

```powershell
Copy-Item .env.example .env
```

Ustaw własne lokalne hasło w `.env`, a następnie uruchom bazę:

```powershell
docker compose up -d --wait
docker compose ps
```

Compose uruchamia PostgreSQL 18 i tworzy bazę `taskmanager`. Domyślny adres połączenia to `localhost:5433`. Port można zmienić przez `DB_PORT` w `.env`. Plik `.env` jest wykluczony z Gita.

## Uruchomienie aplikacji w IntelliJ

Otwórz `Run > Edit Configurations`, wybierz konfigurację `TaskManagerApplication` i ustaw `Environment variables`:

```text
DB_URL=jdbc:postgresql://localhost:5433/taskmanager
DB_USERNAME=taskmanager
DB_PASSWORD=taskmanager_local_password
```

Użyj użytkownika i hasła z lokalnego `.env`. Jeżeli zmienisz `DB_PORT`, ustaw ten sam port w `DB_URL`.

Uruchom `TaskManagerApplication`. API będzie dostępne pod adresem `http://localhost:8080`. Flyway utworzy tabele, a Hibernate zweryfikuje ich zgodność z encjami.

Compose odczytuje `.env` na potrzeby konfiguracji kontenera. Spring Boot uruchamiany w IntelliJ wymaga osobnego ustawienia powyższych zmiennych.

## Obsługa bazy

Logi:

```powershell
docker compose logs -f postgres
```

Zatrzymanie i usunięcie kontenera:

```powershell
docker compose down
```

Dane pozostają w wolumenie `postgres_data` i są dostępne po kolejnym `docker compose up -d --wait`. Kontener tworzy odrębną bazę od PostgreSQL zainstalowanego bezpośrednio w systemie.

Użytkownik i hasło są ustawiane przy pierwszej inicjalizacji wolumenu. Późniejsza edycja `.env` nie zmienia danych logowania istniejącej bazy.

## Testy

Testy na H2:

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

Testy na PostgreSQL wymagają osobnej bazy `taskmanager_test`, ponieważ usuwają użytkowników i zadania. Aby utworzyć ją w kontenerze:

```powershell
docker compose exec postgres sh -c 'createdb -U "$POSTGRES_USER" taskmanager_test'
```

Ustaw w PowerShell dane logowania zgodne z `.env` i uruchom testy:

```powershell
$env:TEST_DB_USERNAME='taskmanager'
$env:TEST_DB_PASSWORD='taskmanager_local_password'
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dspring.profiles.active=postgres-test' '-Dspring.datasource.url=jdbc:postgresql://localhost:5433/taskmanager_test' verify
```

Utworzenie bazy testowej jest potrzebne tylko raz dla danego wolumenu. Jeżeli zmienisz `DB_PORT`, dostosuj port w komendzie testowej.

GitHub Actions uruchamia testy na H2 i PostgreSQL 18 przy każdym pushu i dla pull requestów do `main`. Raporty są dostępne jako artefakty workflow.
