# V4War

V4War, sansure karsi dayaniklilik ve internet ozgurlugu odakli bir ag aracidir.

Motto: `Internet Freedom War & Censorship Resistance`

## Genel Bakis

V4War; V2Ray ve Sing-box hibrit cekirdek yapisini kullanan, farkli proxy senaryolarini tek bir uygulamada yonetmeyi hedefleyen teknik bir istemcidir. Proje, ozellikle kisitli ag ortamlarinda daha esnek baglanti yonetimi, profil duzenleme ve abonelik senkronizasyonu ihtiyacina odaklanir.

- Proje adi: `V4War`
- Paket adi: `tr.theyusa.v4war`
- Gelistirici: [TheYusa](https://github.com/TheYusa)
- Isim ve marka haklari: `TheYusa`

## Ozellikler

- V2Ray ve Sing-box hibrit altyapi
- VLESS, VMess, Trojan, Shadowsocks ve cesitli modern protokol destegi
- Abonelik yonetimi, filtreleme ve otomatik guncelleme
- Gelismis TLS/SNI ayarlari ve baglanti testi araclari
- AMOLED tema dahil ozellestirilebilir arayuz
- Android odakli kullanim, log alma ve tanilama araclari

## Kurulum

### Kaynak Koddan Derleme

Gerekli temel araclar:

- JDK 21
- Android NDK `29.0.14206865`
- Go surumu: `buildScript/init/version.sh` ile uyumlu

Depoyu alin:

```sh
git clone https://github.com/TheYusa/husi.git --depth=1
cd husi
```

Android icin cekirdek kutuphanelerini hazirlayin:

```sh
make libcore_android
```

Varliklari indirin:

```sh
make assets
```

Acik kaynak lisans verilerini olusturun:

```sh
./gradlew :composeApp:exportLibraryDefinitions
```

APK derleyin:

```sh
make apk
```

Olusan APK dosyalari `androidApp/build/outputs/apk` altinda yer alir.

## Gelistirme Notlari

- Android uygulama kimligi `tr.theyusa.v4war` olarak ayarlanmistir.
- UI ve metinsel marka ogeleri V4War kimligine gore duzenlenmistir.
- Cekirdek binary adlarini degistirmeyin; bunlar calisma zamani entegrasyonu icin sabit tutulmalidir.

## Yasal Uyari

V4War yasal, etik ve sorumlu kullanim amaciyla sunulur. Bu proje;

- yerel yasalari ihlal etmek,
- yetkisiz erisim saglamak,
- zararli faaliyetleri gizlemek

icin tasarlanmamistir.

Uygulamayi kullanmadan once bulundugunuz ulkedeki mevzuati ve servis saglayicinizin kurallarini kontrol edin. Tum sorumluluk kullaniciya aittir.

## Baglantilar

- GitHub: https://github.com/TheYusa
- Profil: https://github.com/TheYusa
