# V4War

V4War, Android icin gelistirilmis bir sansur direnci ve internet ozgurlugu istemcisidir.

`Internet Freedom War & Censorship Resistance`

V4War; V2Ray tabanli paylasim formatlari ile Sing-box cekirdegini ayni uygulamada birlestiren, teknik kullanicilara odakli bir proxy yonetim aracidir. Amaç; farkli ag kosullarinda profilleri hizli ithal etmek, abonelikleri yonetmek, baglanti davranisini ince ayarlamak ve sansure karsi daha esnek bir istemci deneyimi sunmaktir.

## Nedir?

V4War bir VPN servisi veya hazir sunucu saglayicisi degildir. Uygulama; kullanicinin sahip oldugu ya da guvendigi proxy/profil baglantilarini Android cihaz uzerinde calistirmasina yardim eder.

Temel odak noktalarimiz:

- Android uzerinde guclu proxy istemci deneyimi
- V2Ray ve Sing-box hibrit cekirdek yapisi
- Profil ve abonelik yonetiminde hiz ve esneklik
- Sansure karsi dayaniklilik ve baglanti kontrolu
- Teknik ama sade, hizli ve ozellestirilebilir arayuz

## Ozellikler

- VLESS, VMess, Trojan, Shadowsocks ve diger modern protokol destegi
- Subscription import, filtreleme, deduplication ve otomatik guncelleme
- Baglanti testi, hiz testi ve log disa aktarma araclari
- Gelismis TLS/SNI ayarlari
- AMOLED dahil tema secenekleri
- Android odakli arayuz ve sistem entegrasyonu

## Teknik Yapi

- Uygulama adi: `V4War`
- Paket adi: `tr.theyusa.v4war`
- Gelistirici: [TheYusa](https://github.com/TheYusa)
- Isim ve marka haklari: `TheYusa`
- Cekirdek yapisi: `V2Ray + Sing-box hibrit altyapi`

## Derleme

Gereksinimler:

- JDK 21
- Android NDK `29.0.14206865`
- Go surumu: `buildScript/init/version.sh` ile uyumlu

Kaynak kodu alin:

```sh
git clone https://github.com/TheYusa/husi.git --depth=1
cd husi
```

Android icin gerekli libcore derlemesini alin:

```sh
make libcore_android
```

Varlik dosyalarini indirin:

```sh
make assets
```

Acik kaynak lisans ciktilarini olusturun:

```sh
./gradlew :composeApp:exportLibraryDefinitions
```

FOSS release APK derleyin:

```sh
make apk
```

APK ciktilari:

```text
androidApp/build/outputs/apk
```

## Yasal Uyari

V4War, yasal ve sorumlu kullanim icin sunulur. Bu proje:

- yerel yasalari ihlal etmek,
- yetkisiz erisim saglamak,
- zararli faaliyetleri gizlemek,
- ucuncu taraf sistemlere izinsiz mudahale etmek

amaciyla tasarlanmamistir.

Bulundugunuz ulkedeki mevzuat ve servis saglayici kurallari sizin sorumlulugunuzdadir. Uygulamanin kullanimindan dogan tum hukuki ve teknik sorumluluk son kullaniciya aittir.

## Baglantilar

- GitHub: https://github.com/TheYusa
- Profil: https://github.com/TheYusa
