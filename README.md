# 💳 YoPayment

> Плагин для создания платёжных ссылок через ЮKassa на Minecraft серверах (Paper 1.16+, Java 17)

---

## Обзор

**YoPayment** — это надёжный плагин для Minecraft серверов (Paper 1.16–1.21+), который интегрирует платёжную систему **ЮKassa**. Позволяет игрокам и администраторам создавать платёжные ссылки прямо из игры, автоматически проверяет оплату и выдаёт донат в реальном времени или при следующем входе игрока на сервер.

---

## ✨ Основные возможности

- 🔗 **Кликабельные ссылки в чате** — переход на оплату в один клик с красивым ховером
- 📊 **Автоматическое отслеживание** — асинхронная проверка статуса в ЮKassa без спама API
- 🛡️ **Защита от двойной выдачи** — предотвращение состояний гонки (Race conditions) при подтверждении
- ⏳ **Очередь для офлайн-игроков** — если игрок оплатил, находясь офлайн, донат выдастся автоматически при его входе (`PlayerJoinEvent`)
- 🧾 **Поддержка чеков по 54-ФЗ** — фискализация для самозанятых, ИП и юрлиц в ЮKassa
- 🗄️ **Поддержка баз данных** — H2 (встроенная), MySQL (с поддержкой SSL и без) и PostgreSQL
- ⚡ **Zero-Lag PlaceholderAPI** — моментальное чтение плейсхолдеров из памяти без зависания тиков сервера
- 🌈 **Цвета и стили** — поддержка HEX (`&#RRGGBB`) и классических кодов (`&a`, `&c`)
- 🔑 **Безопасность ключей** — поддержка чтения учетных данных через переменные окружения

---

## Установка

### Требования

- **Minecraft сервер**: Paper / Purpur / Spigot **1.16+**
- **Java**: **17+**
- **ЮKassa аккаунт**: с действующими `shop-id` и `secret-key`

### Шаги установки

1. Поместите JAR файл плагина в папку `/plugins/`.
2. Перезагрузите сервер или загрузите плагин через менеджер плагинов.
3. Отредактируйте конфиг: `/plugins/YoPayment/config.yml` (укажите ваш `shop-id` и `secret-key`).
4. Настройте товары в `/plugins/YoPayment/donates.yml`.
5. Перезагрузите конфиг: `/yopayment reload`.

```bash
# Структура файлов плагина
plugins/
└── YoPayment/
    ├── config.yml
    ├── messages.yml
    ├── donates.yml
    └── yopayment.mv.db (встроенная H2 база)
```

---

## Команды и права

### Команды

| Команда | Описание | Право |
|---|---|---|
| `/yopayment create <товар>` | Создать ссылку на оплату для себя | `yopayment.create` |
| `/yopayment create <товар> <игрок>` | Создать ссылку на оплату для другого игрока | `yopayment.create.others` |
| `/yopayment reload` | Перезагрузить конфигурацию плагина | `yopayment.reload` |
| `/yopayment list active` | Список всех активных неоплаченных счетов | `yopayment.list` |
| `/yopayment list <игрок>` | История покупок конкретного игрока | `yopayment.list.others` (или свои при `yopayment.list`) |

### Алиасы

- `/yp` — короткий алиас
- `/yopay` — альтернативный алиас

### Примеры использования

```text
/yp create vip
/yp create premium Steve
/yp list active
/yp list Steve
/yp reload
```

---

## Конфигурация

### `config.yml`

```yaml
# Учётные данные ЮKassa
yookassa:
  shop-id: "000000"           # Ваш ID магазина (или ENV: YOOKASSA_SHOP_ID)
  secret-key: "test_XXX"      # Секретный ключ (или ENV: YOOKASSA_SECRET_KEY)
  return-url: "https://pay.example.com/"

# Интервалы проверки
check-interval: 5             # Проверка статуса в секундах
payment-timeout: 10           # Время жизни счёта в минутах

silent-mode: true             # Тихий режим (только ошибки в консоли)

# Чеки по 54-ФЗ (если в ЮKassa включена онлайн-касса)
receipt:
  enabled: false
  default-email: "donate@example.com"
  tax-system-code: 0          # Код СНО (1-6, 0 - не передавать)
  vat-code: 1                 # 1 - без НДС, 2 - 0%, 4 - 20%

# База данных
database:
  type: h2                    # h2, mysql, postgresql
  
  h2:
    file: "yopayment"
    
  mysql:
    host: "localhost"
    port: 3306
    database: "yopayment"
    username: "root"
    password: ""
    ssl: false                # true/false
    pool-size: 10
```

### `donates.yml`

```yaml
donates:
  vip:
    display-name: "VIP"
    price: 30.0
    description: "Привилегия VIP на 30 дней"
    commands:
      - "lp user %player% parent add vip"
      - "bc Игрок %player% приобрёл VIP!"

  premium:
    display-name: "Premium"
    price: 60.0
    description: "Привилегия Premium"
    commands:
      - "lp user %player% parent add premium"
```

---

## PlaceholderAPI

Плагин регистрирует экспансию `%YoPayment%`. Все данные берутся моментально из оперативной памяти без задержки основного потока сервера:

| Плейсхолдер | Описание | Пример вывода |
|---|---|---|
| `%YoPayment_Link%` | Последняя активная ссылка игрока | `https://yookassa.ru/...` или `N/A` |
| `%YoPayment_Custom:<формат>%` | Настраиваемый вывод последнего платежа | См. переменные ниже |

### Переменные для `%YoPayment_Custom:<формат>%`:
- `{display_name}` — Название товара
- `{item}` — ID товара
- `{price}` — Форматированная цена
- `{description}` — Описание
- `{link}` — Ссылка на оплату
- `{status}` — Статус (`pending`, `pending_delivery`, `succeeded`, `canceled`)
- `{created_at}` — Дата и время создания

---

## Сборка из исходников

```bash
mvn clean package
```

Собранный JAR появится в папке `target/YoPayment-1.0b.jar`.
