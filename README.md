# Mini-store Backend

## 🛠 Процесс разработки и ветвление

В качестве основной ветки разработки по умолчанию (Default Branch) используется **`develop`**.

### Назначение основных веток:
* **`develop`** — ветка активной разработки. Код из этой ветки автоматически собирается и разворачивается на **тестовом стенде**.
* **`main`** — стабильная ветка. Код попадает сюда только после успешного тестирования на тестовом стенде при релизе.

---

### Правила работы с репозиторием:
1. **Прямые пуши запрещены:** Вливание кода в ветки `develop` и `main` происходит исключительно через **Pull Request (PR)**.
2. **Создание новых веток:**
    * Все новые ветки создаются строго **от ветки `develop`**:
      ```bash
      git checkout develop
      git pull origin develop
      ```
    * Для новой функциональности используйте префикс `feature/`:  
      `git checkout -b feature/название-задачи`
    * Для исправления багов используйте префикс `bugfix/`:  
      `git checkout -b bugfix/описание-бага`

---

## Локальный запуск

Для запуска backend и PostgreSQL необходим Docker.

```bash
docker compose --profile db up -d --build
```

Проверить состояние контейнеров:

```bash
docker compose ps
```

Посмотреть логи backend:

```bash
docker compose logs -f backend
```

Остановить контейнеры:

```bash
docker compose down
```

Данные PostgreSQL сохраняются в именованном Docker volume `postgres-data`, поэтому обычный
`docker compose down` не удаляет базу. Для полного сброса локальных данных используйте:

```bash
docker compose down -v
```

После запуска доступны:

| Сервис | Адрес |
| --- | --- |
| Backend | <http://localhost:8080> |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| Health-check | <http://localhost:8080/actuator/health> |
| PostgreSQL | `localhost:5432` |

## API

### Health-check

Проверяет доступность backend-сервиса и подключённых компонентов.

```http
GET /actuator/health
```

Успешный ответ — `200 OK`:

```json
{
  "status": "UP"
}
```

Интерактивная документация реализованных endpoint'ов автоматически формируется
в формате OpenAPI и доступна в Swagger UI после запуска приложения.

### Вход кассира (US-05)

Для первого запуска задайте `MINISTORE_OWNER_USERNAME` и `MINISTORE_OWNER_PASSWORD`
(минимум 12 символов) в окружении backend или в локальном `.env` для Docker Compose.
Пользователь создаётся один раз с хешем BCrypt; повторный запуск не меняет его пароль.
Не добавляйте `.env` в Git. Если обе переменные не заданы, учётная запись не создаётся.

`POST /api/auth/login` принимает JSON `{"username":"...","password":"..."}` и
возвращает `token`, `expiresAt`, `username`, `role`. Ошибка учётных данных даёт `401`.
Передавайте токен в `Authorization: Bearer <token>` для `GET /api/auth/me` и
`POST /api/auth/logout`. Сессия действует 30 дней, хранится в БД как SHA-256 хеш
и может быть отозвана через logout. Пароль и токен в открытом виде в БД не сохраняются.
