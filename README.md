# 💳 YoPayment

> Плагин для создания платёжных ссылок через ЮKassa на Minecraft серверах
---

## Обзор

**YoPayment** — это мощный плагин для Minecraft серверов, который интегрирует платёжную систему ЮKassa. Позволяет администраторам создавать и управлять платёжными ссылками прямо из игры, а также отслеживать статус платежей в реальном времени.

---

## ✨ Основные возможности

- 🔗 **Создание платёжных ссылок** — быстро генерируйте ссылки для оплаты через ЮKassa
- 📊 **Отслеживание платежей** — автоматическая проверка статуса платежей
- 🎁 **Система донатов** — гибкая конфигурация товаров и цен
- 🗄️ **Множество БД** — поддержка H2, MySQL и PostgreSQL
- 📝 **PlaceholderAPI** — встроенная интеграция с популярным плагином
- 🌈 **Поддержка цветов** — HEX и стандартный формат Minecraft цветов
- ⚙️ **Гибкая конфигурация** — подробные настройки через YAML файлы

---

## Установка

### Требования

- **Minecraft сервер**: Paper 1.16+
- **Java**: 17+
- **ЮKassa аккаунт**: с действительными shop-id и secret-key

### Шаги установки

1. Скачайте JAR файл плагина
2. Поместите его в папку `/plugins`
3. Перезагрузите сервер: `/reload confirm`
4. Отредактируйте конфиг: `/plugins/YoPayment/config.yml`
5. Перезагрузите конфиг: `/yopayment reload`

```bash
# Пример структуры папок
plugins/
├── YoPayment/
│   ├── config.yml
│   ├── messages.yml
│   ├── donates.yml
│   └── yopayment.db (H2 база)
```

---

## Команды

### Основные команды

| Команда | Описание | Права доступа |
|---------|---------|---------------|
| `/yopayment create <player> <item>` | Создать ссылку оплаты для игрока | `yopayment.create.others` |
| `/yopayment create <item>` | Создать ссылку оплаты для себя | `yopayment.create` |
| `/yopayment reload` | Перезагрузить конфигурацию | `yopayment.reload` |
| `/yopayment list [player]` | Просмотреть список платежей | `yopayment.list` |

### Примеры использования

```
/yopayment create premium
/yopayment create @nickname vip_pass
/yopayment reload
/yopayment list
/yopayment list @nickname
```

### Алиасы

- `/yp` — короткий алиас
- `/yopay` — альтернативный алиас

---

## Конфигурация

### config.yml

Основной конфиг плагина:

```yaml
# API ЮKassa
yookassa:
  shop-id: "000000"           # Ваш ID магазина
  secret-key: "test_XXX"      # Секретный ключ
  return-url: "https://example.com/"

# Проверка платежей (в секундах)
check-interval: 5

# Время жизни платежа (в минутах)
payment-timeout: 10

# Формат сообщения при создании ссылки
# Доступные плейсхолдеры: {display_name}, {price}, {url}
message-format: "&7[&#00bfffОплата&7] &fТовар: &#00bfff{display_name}"

# Режим логирования
silent-mode: true

# Выбор БД
database:
  type: h2              # h2, mysql или postgresql
```

### donates.yml

Настройка товаров и цен:

```yaml
items:
  premium:
    display_name: "Premium Статус"
    price: 99
    description: "Месячный премиум доступ"
  
  vip_pass:
    display_name: "VIP Пропуск"
    price: 299
    description: "Пожизненный VIP доступ"
```

### messages.yml

Кастомные сообщения:

```yaml
not_found: "❌ Товар не найден"
error: "⚠️ Ошибка при создании ссылки"
success: "✅ Ссылка создана!"
```

---

## Placeholder-api

**YoPayment** полностью совместим с PlaceholderAPI. Используйте плейсхолдеры в других плагинах:

### Доступные плейсхолдеры

#### `%YoPayment_Link%`
Показывает последнюю рабочую ссылку для оплаты

```
/tell @nickname Ваша ссылка: %YoPayment_Link%
```

#### `%YoPayment_Custom:<format>%`
Кастомный плейсхолдер с полной информацией о платеже

**Доступные переменные:**
- `{display_name}` — Название товара
- `{item}` — ID товара
- `{price}` — Цена (форматированная)
- `{description}` — Описание товара
- `{link}` — Ссылка на оплату
- `{status}` — Статус платежа (pending, succeeded, canceled)
- `{created_at}` — Дата создания

**Примеры:**

```
%YoPayment_Custom:&#00bfffПоследняя покупка: {display_name} ({price})%
%YoPayment_Custom:&cСтатус: &e{status}%
%YoPayment_Custom:{description} - {price} руб.%
```

---

## Базы данных

Плагин поддерживает три типа БД:

### H2 (по умолчанию)
Встроенная БД, не требует установки:

```yaml
database:
  type: h2
  h2:
    file: "yopayment"
```

### MySQL

```yaml
database:
  type: mysql
  mysql:
    host: "localhost"
    port: 3306
    database: "yopayment"
    username: "root"
    password: "password"
    pool-size: 10
```

### PostgreSQL

```yaml
database:
  type: postgresql
  postgresql:
    host: "localhost"
    port: 5432
    database: "yopayment"
    username: "postgres"
    password: "password"
    pool-size: 10
```

---

## Система прав

| Право | Описание | По умолчанию |
|-------|---------|-------------|
| `yopayment.create` | Создавать ссылки для себя | OP |
| `yopayment.create.others` | Создавать ссылки для других | OP |
| `yopayment.reload` | Перезагружать конфиг | OP |
| `yopayment.list` | Просматривать свои платежи | OP |
| `yopayment.list.others` | Просматривать платежи других | OP |

---

## Архитектура

### Структура проекта

```
src/main/java/com/noloverme/yopayment/
├── YoPaymentPlugin.java           # Главный класс плагина
├── api/
│   ├── YooKassaClient.java        # Клиент ЮKassa API
│   └── model/                     # Модели API
├── command/
│   └── YoPaymentCommand.java      # Обработчик команд
├── config/
│   ├── MainConfig.java            # Основная конфигурация
│   ├── DonatesConfig.java         # Конфиг товаров
│   └── MessagesConfig.java        # Кастомные сообщения
├── database/
│   ├── AbstractSQLDatabase.java   # Абстрактный класс БД
│   ├── H2Database.java            # H2 реализация
│   ├── MySQLDatabase.java         # MySQL реализация
│   └── PostgreSQLDatabase.java    # PostgreSQL реализация
├── model/
│   ├── DonateItem.java            # Модель товара
│   └── PaymentRecord.java         # Запись платежа
├── task/
│   └── PaymentCheckTask.java      # Асинхронная проверка платежей
├── placeholder/
│   └── YoPaymentPlaceholder.java  # PlaceholderAPI интеграция
└── util/
    └── TextUtil.java              # Утилиты для работы с текстом
```

---

## Разработка

### Сборка проекта

```bash
# Требует Maven
mvn clean package

# JAR файл будет в target/YoPayment-1.0a.jar
```

### Зависимости

- **Paper API 1.21.4** — Minecraft сервер API
- **HikariCP 7.0.2** — Пулинг подключений к БД
- **H2 2.4.240** — Встроенная база данных
- **MySQL Connector 9.7.0** — MySQL драйвер
- **PostgreSQL 42.7.11** — PostgreSQL драйвер
- **Gson 2.14.0** — JSON парсер
- **PlaceholderAPI 2.12.2** — Интеграция PlaceholderAPI

---

## Статусы платежей

| Статус | Описание |
|--------|---------|
| `pending` | ⏳ Платёж ожидает обработки |
| `succeeded` | ✅ Платёж успешно завершён |
| `canceled` | ❌ Платёж отменён пользователем или истёк срок |

---

## Поиск неисправностей

### Ссылка не создаётся
1. Проверьте shop-id и secret-key в config.yml
2. Убедитесь, что интернет соединение активно
3. Проверьте логи: `silent-mode: false`

### Платежи не отслеживаются
1. Убедитесь, что check-interval > 0
2. Проверьте права доступа в БД
3. Перезагрузите плагин: `/yopayment reload`

### PlaceholderAPI не работает
1. Установите PlaceholderAPI плагин
2. Убедитесь, что YoPayment загружен после PlaceholderAPI
3. Перезагрузите сервер

---

## Автор

**noloverme** — разработчик плагина

---

## Поддержка

Если у вас есть вопросы или проблемы:

1. 📖 Проверьте документацию выше
2. 🐛 Посмотрите раздел [Поиск неисправностей](#поиск-неисправностей)
3. 💬 Создайте issue на GitHub

---

**Спасибо, что используете YoPayment!**
