# FA26_SEP490-G10
JLPP - Japanese Learning Path Platform 

## Connect to Supabase

The default configuration uses an in-memory H2 database. To use Supabase PostgreSQL, get the connection details from **Supabase Dashboard → Project Settings → Database → Connect**, then set the following environment variables in the PowerShell terminal used to run the application:

```powershell
$env:SUPABASE_DB_URL = 'jdbc:postgresql://HOST:PORT/postgres?sslmode=require'
$env:SUPABASE_DB_USERNAME = 'YOUR_SUPABASE_DATABASE_USERNAME'
$env:SUPABASE_DB_PASSWORD = 'YOUR_SUPABASE_DATABASE_PASSWORD'
$env:SPRING_PROFILES_ACTIVE = 'supabase'
mvn spring-boot:run
```

Replace the example URL, username, and password with the connection values from Supabase. Use the exact host, port, database, and username shown for the selected connection method. Do not commit database credentials to source control.

The Supabase profile validates the existing database schema (`ddl-auto: validate`) and does not automatically create or modify tables. Create the required tables or migrations in Supabase before connecting. Remove `SPRING_PROFILES_ACTIVE` from the terminal environment to return to the default H2 profile.

## Project organization

- Backend Java code is under `src/main/java/com/jlpp`, grouped by feature module. Admin user controllers, models, repositories, and services live in `modules/user`; system log code lives in `modules/systemlog`; dashboard routes live in `modules/dashboard`.
- Thymeleaf frontend pages are under `src/main/resources/templates/pages/admin`, grouped into `user`, `logs`, and `dashboard`.
- Shared Thymeleaf layouts and fragments are under `src/main/resources/templates/layouts` and `src/main/resources/templates/fragments`.
- Static CSS and JavaScript are under `src/main/resources/static`.
- Spring configuration belongs under `src/main/resources`; tests belong under `src/test/java/com/jlpp`.
