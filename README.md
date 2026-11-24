# Courier Tracker

Kurye konumlarını alan, 100 metre yarıçapındaki Migros mağazalarına girişleri kaydeden ve kuryelerin toplam seyahat mesafelerini hesaplayan Spring Boot REST API.

## Gereksinimler

- JDK 21 veya üzeri
- Maven 3.8 veya üzeri

## Çalıştırma

```bash
mvn spring-boot:run
```

Uygulama varsayılan olarak `http://localhost:8080` adresinde çalışır.

## Testler

```bash
mvn test
```

## Yapılandırma

`src/main/resources/application.yml` dosyasından ayarlanabilir:

- `courier.tracking.store-radius-meters`: mağaza giriş yarıçapı (varsayılan `100.0` metre)
- `courier.tracking.reentry-cooldown-seconds`: aynı mağaza için tekrar giriş bekleme süresi (varsayılan `60` saniye)
- `courier.tracking.stores-file-path`: mağaza JSON dosyasının yolu

## API Örnekleri

Konum gönderme:

```bash
curl -X POST http://localhost:8080/api/v1/locations \
  -H 'Content-Type: application/json' \
  -d '{"time":"2026-11-23T10:00:00Z","courier":"courier-1","lat":40.9924000,"lng":29.1244500}'
```

Toplam seyahat mesafesini sorgulama:

```bash
curl http://localhost:8080/api/v1/couriers/courier-1/distance
```

Tüm girişleri sorgulama:

```bash
curl http://localhost:8080/api/v1/entrances
```

Belirli bir kuryenin girişlerini sorgulama:

```bash
curl 'http://localhost:8080/api/v1/entrances?courierId=courier-1'
```

## Kriterlerin karşılanması

- Mağaza konumları `src/main/resources/stores.json` dosyasından yüklenir.
- Mesafe hesaplamasında Haversine yöntemi kullanılır (**Strategy** tasarım deseni).
- Mağaza girişi olduğunda dinleyicilere bildirim yapılır (**Observer** tasarım deseni).
- Aynı kurye aynı mağazaya 60 saniye içinde tekrar girerse yeni giriş kaydı oluşturmaz.
