# Moodify (Swing GUI)

Projek Java sederhana dengan tampilan GUI (Swing). Tampilkan jendela dengan label suasana hati dan tombol untuk mengubah mood: Happy, Calm, Sad, dan Reset.

## Struktur
- `src/main/java/org/example/Main.java` — Entry point aplikasi, meluncurkan GUI di Event Dispatch Thread.
- `src/main/java/org/example/MoodifyFrame.java` — JFrame utama dengan label dan tombol.

Catatan: Ada juga `src/main/java/org/example/ui/MoodifyFrame.java` (varian dengan package berbeda). Yang digunakan oleh `Main` adalah kelas di package `org.example`.

## Prasyarat
- Java JDK 17+ (Swing sudah termasuk di JDK). POM saat ini diset ke Java 23; jika JDK Anda 17/21, Anda bisa menurunkannya di `pom.xml` (maven.compiler.source/target).
- Maven (opsional) jika ingin build lewat Maven.

## Cara menjalankan

### 1) Jalankan di IntelliJ IDEA (direkomendasikan)
- Buka folder proyek ini.
- Pastikan Project SDK mengarah ke JDK yang terpasang (File > Project Structure > Project SDK).
- Klik kanan `Main.java` > Run 'Main'.

### 2) Jalankan via Maven (jika Maven terpasang)
Di Command Prompt (cmd.exe):

```
cd /d "D:\TUGAS DAN MATERI\SMT3\Praktek Pemrograman Berorientasi Obyek\projek_uas\moodify"
mvn clean package
java -jar target\moodify-1.0-SNAPSHOT.jar
```

Atau langsung jalankan tanpa membuat jar:
```
mvn -DskipTests exec:java
```

Jika perintah `mvn` tidak dikenali, instal Maven atau gunakan cara ke-3 di bawah ini.

### 3) Jalankan tanpa Maven (kompilasi manual)
Di Command Prompt (cmd.exe), dengan JDK sudah terpasang dan `javac`/`java` ada di PATH:

```
cd /d "D:\TUGAS DAN MATERI\SMT3\Praktek Pemrograman Berorientasi Obyek\projek_uas\moodify"
mkdir out 2>nul
javac -d out -sourcepath src\main\java src\main\java\org\example\Main.java
java -cp out org.example.Main
```

Perintah di atas akan mengompilasi dan menjalankan aplikasi GUI.

## Troubleshooting
- `mvn` tidak dikenali: Instal Maven atau jalankan dengan IntelliJ / kompilasi manual.
- `javac`/`java` tidak dikenali: Instal JDK dan tambahkan `bin` JDK ke PATH.
- Versi Java tidak cocok: Ubah `maven.compiler.source` dan `maven.compiler.target` di `pom.xml` sesuai JDK Anda (misal 17).

## Lisensi
Kode ini ditujukan sebagai contoh pembelajaran.

