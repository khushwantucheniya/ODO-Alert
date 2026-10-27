# ODO Alert FullStack

## Requirements
- Java 17
- Maven 3.9+
- MySQL 8.x (or compatible)
- Database named `odo_alert`

## Configure MySQL
Open `src/main/resources/application.properties`.
The project defaults to MySQL user `root` with an empty password. If your local MySQL root account has a password, set it in the `spring.datasource.password` value or set the `DB_PASSWORD` environment variable before running.

Create the database once in phpMyAdmin or MySQL:
```sql
CREATE DATABASE IF NOT EXISTS odo_alert;
```

## Run on Windows PowerShell
Open a terminal in the folder containing `pom.xml`:
```powershell
mvn clean spring-boot:run
```
Then open http://localhost:8080

If port 8080 is already occupied, stop the older app instance or temporarily change `server.port` in `application.properties`.

## Enable Gmail expiry reminders
Reminders are scheduled daily at 9:00 AM India time and are sent exactly 5 days before Insurance, PUC, or Fitness/Permit expiry. By default, email sending is disabled until configured.

1. Enable 2-Step Verification on the Gmail account and create a Google App Password.
2. In PowerShell, set the values for the current terminal (replace the sample values):
```powershell
$env:DB_PASSWORD="your_mysql_password"
$env:GMAIL_USERNAME="your_gmail_address@gmail.com"
$env:GMAIL_APP_PASSWORD="your_16_character_google_app_password"
$env:EMAIL_REMINDERS_ENABLED="true"
mvn spring-boot:run
```
If your MySQL account has no password, set `$env:DB_PASSWORD=""`.
Never commit real passwords or app passwords into source control.

## Notes
- Hibernate `ddl-auto=update` creates/updates the application tables automatically.
- Existing accounts and vehicles are kept in the `odo_alert` database.
- Email reminders are sent to the email address used to create the vehicle owner's account.
