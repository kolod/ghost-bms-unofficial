# Ghost BMS Monitor (unofficial)

Unofficial Android app for monitoring Ghost-series BMS units (96S/192S and
similar) over Bluetooth Low Energy. See [format.md](../format.md) for the
reverse-engineered BLE protocol notes.

- `app/` — Compose UI, ViewModel
- `bms-ble/` — protocol-only Kotlin library (parser, state, commands, BLE client)

Contact / BLE logs from other hardware or firmware versions: oleksandr.kolodkin@ukr.net

## Play Store listing

### 🇺🇦 Українська

**Короткий опис**
```
Неофіційний BLE-монітор Ghost BMS: живі показники, напруги комірок, температура
```

**Повний опис**
```
Ghost BMS Monitor — неофіційний застосунок для моніторингу BMS (Battery Management System) серії Ghost (варіанти 96S/192S та подібні) через Bluetooth Low Energy.

Що показує:
- Живі напруга, струм і потужність батареї
- Напруги кожної комірки (до 192 комірок, два незалежні банки)
- Температура балансувальних модулів
- Статус захисту, реле заряду/розряду, аварійні сигнали
- Читання та (як гіпотеза) запис основних налаштувань BMS
- Демо-режим без реального BMS — щоб спробувати інтерфейс

Мови інтерфейсу: українська, English, Deutsch, Français, Italiano, Español.

⚠ Важливо:
Цей застосунок створено незалежним розробником шляхом реверс-інжинірингу BLE-протоколу, без доступу до офіційної специфікації виробника. Частина команд запису налаштувань — це гіпотеза, не підтверджена офіційно і не перевірена на всіх версіях апаратного забезпечення чи прошивки. Розробник НЕ гарантує коректність показників чи команд і НЕ несе відповідальності за будь-які матеріальні збитки, пошкодження обладнання чи інші наслідки використання застосунку. Використовуйте на власний ризик і завжди звіряйте критичні налаштування у штатному застосунку виробника.

Застосунок безкоштовний, без реклами, не потребує інтернету і нічого не передає на сторонні сервери.

Маєте BMS іншої версії чи прошивки? Буду вдячний за BLE-логи з вашого пристрою — вони допомагають розширити підтримку. Пишіть на oleksandr.kolodkin@ukr.net
```

### 🇬🇧 English

**Short description**
```
Unofficial BLE monitor for Ghost BMS: live readings, cell voltages, temperatures
```

**Full description**
```
Ghost BMS Monitor is an unofficial app for monitoring Ghost-series BMS units (96S/192S and similar) over Bluetooth Low Energy.

What it shows:
- Live battery voltage, current and power
- Per-cell voltages (up to 192 cells, two independent banks)
- Balancing-module temperatures
- Protection status, charge/discharge relay state, alarms
- Read (and, as a hypothesis, write) core BMS settings
- A demo mode with no real BMS required, so you can try the interface first

Interface languages: English, українська, Deutsch, Français, Italiano, Español.

⚠ Important:
This app was built by an independent developer by reverse-engineering the BLE protocol, without access to the manufacturer's official specification. Some of the settings-write commands are a hypothesis, not officially confirmed, and have not been tested on every hardware or firmware version. The developer does NOT guarantee the accuracy of any reading or command, and is NOT responsible for any material loss, equipment damage, or other consequences from using this app. Use at your own risk, and always double-check critical settings in the manufacturer's own app.

The app is free, has no ads, needs no internet connection, and sends nothing to any server.

Have a BMS of a different version or firmware? BLE logs from your device are very welcome — they help extend support. Send them to oleksandr.kolodkin@ukr.net
```

### 🇩🇪 Deutsch

**Kurzbeschreibung**
```
Inoffizieller BLE-Monitor für Ghost BMS: Live-Daten, Zellspannungen, Temperatur
```

**Ausführliche Beschreibung**
```
Ghost BMS Monitor ist eine inoffizielle App zur Überwachung von Ghost-BMS-Geräten (96S/192S und ähnliche) über Bluetooth Low Energy.

Was die App anzeigt:
- Live-Spannung, -Strom und -Leistung der Batterie
- Spannung jeder einzelnen Zelle (bis zu 192 Zellen, zwei unabhängige Bänke)
- Temperatur der Balancing-Module
- Schutzstatus, Zustand der Lade-/Entladerelais, Alarme
- Lesen (und, als Hypothese, Schreiben) der wichtigsten BMS-Einstellungen
- Ein Demomodus ohne echtes BMS, um die Oberfläche vorab auszuprobieren

Sprachen der Oberfläche: Deutsch, English, українська, Français, Italiano, Español.

⚠ Wichtiger Hinweis:
Diese App wurde von einem unabhängigen Entwickler durch Reverse Engineering des BLE-Protokolls erstellt, ohne Zugriff auf die offizielle Spezifikation des Herstellers. Ein Teil der Schreibbefehle für Einstellungen ist eine Hypothese, nicht offiziell bestätigt und nicht auf jeder Hardware- oder Firmware-Version getestet. Der Entwickler übernimmt KEINE Garantie für die Richtigkeit von Anzeigen oder Befehlen und haftet NICHT für materielle Schäden, Geräteschäden oder sonstige Folgen der Nutzung dieser App. Nutzung auf eigenes Risiko — überprüfen Sie kritische Einstellungen immer zusätzlich in der Original-App des Herstellers.

Die App ist kostenlos, werbefrei, benötigt keine Internetverbindung und sendet nichts an einen Server.

Haben Sie ein BMS einer anderen Version oder Firmware? BLE-Logs Ihres Geräts sind sehr willkommen — sie helfen, die Unterstützung zu erweitern. Schreiben Sie an oleksandr.kolodkin@ukr.net
```

### 🇫🇷 Français

**Description courte**
```
Moniteur BLE non officiel pour Ghost BMS : mesures, tensions, températures
```

**Description complète**
```
Ghost BMS Monitor est une application non officielle pour surveiller les BMS de la série Ghost (96S/192S et similaires) via Bluetooth Low Energy.

Ce qu'elle affiche :
- Tension, courant et puissance de la batterie en direct
- Tension de chaque cellule (jusqu'à 192 cellules, deux bancs indépendants)
- Température des modules d'équilibrage
- État de protection, état des relais de charge/décharge, alarmes
- Lecture (et, à titre d'hypothèse, écriture) des principaux paramètres du BMS
- Un mode démo sans BMS réel, pour essayer l'interface d'abord

Langues de l'interface : Français, English, українська, Deutsch, Italiano, Español.

⚠ Important :
Cette application a été créée par un développeur indépendant par rétro-ingénierie du protocole BLE, sans accès à la spécification officielle du fabricant. Certaines commandes d'écriture des paramètres sont une hypothèse, non confirmée officiellement, et n'ont pas été testées sur toutes les versions de matériel ou de firmware. Le développeur NE garantit PAS l'exactitude des mesures ou des commandes, et N'EST PAS responsable des pertes matérielles, dommages à l'équipement ou autres conséquences liées à l'utilisation de cette application. Utilisez-la à vos propres risques et vérifiez toujours les paramètres critiques dans l'application d'origine du fabricant.

L'application est gratuite, sans publicité, ne nécessite pas de connexion internet et n'envoie rien à un serveur.

Vous avez un BMS d'une autre version ou d'un autre firmware ? Les journaux BLE de votre appareil sont les bienvenus — ils aident à étendre la compatibilité. Écrivez à oleksandr.kolodkin@ukr.net
```

### 🇮🇹 Italiano

**Descrizione breve**
```
Monitor BLE non ufficiale per Ghost BMS: dati live, tensioni celle, temperature
```

**Descrizione completa**
```
Ghost BMS Monitor è un'app non ufficiale per monitorare i BMS della serie Ghost (96S/192S e simili) via Bluetooth Low Energy.

Cosa mostra:
- Tensione, corrente e potenza della batteria in tempo reale
- Tensione di ogni singola cella (fino a 192 celle, due banchi indipendenti)
- Temperatura dei moduli di bilanciamento
- Stato di protezione, stato dei relè di carica/scarica, allarmi
- Lettura (e, come ipotesi, scrittura) delle principali impostazioni del BMS
- Una modalità demo senza BMS reale, per provare prima l'interfaccia

Lingue dell'interfaccia: Italiano, English, українська, Deutsch, Français, Español.

⚠ Importante:
Questa app è stata realizzata da uno sviluppatore indipendente tramite reverse engineering del protocollo BLE, senza accesso alla specifica ufficiale del produttore. Alcuni comandi di scrittura delle impostazioni sono un'ipotesi, non confermata ufficialmente, e non sono stati testati su tutte le versioni di hardware o firmware. Lo sviluppatore NON garantisce l'accuratezza di letture o comandi e NON è responsabile per eventuali perdite materiali, danni all'apparecchiatura o altre conseguenze derivanti dall'uso di questa app. Usala a tuo rischio e verifica sempre le impostazioni critiche nell'app originale del produttore.

L'app è gratuita, senza pubblicità, non richiede connessione a internet e non invia nulla ad alcun server.

Hai un BMS di una versione o firmware diversi? I log BLE del tuo dispositivo sono benvenuti — aiutano ad ampliare il supporto. Scrivi a oleksandr.kolodkin@ukr.net
```

### 🇪🇸 Español

**Descripción breve**
```
Monitor BLE no oficial para Ghost BMS: datos en vivo, voltajes, temperaturas
```

**Descripción completa**
```
Ghost BMS Monitor es una app no oficial para supervisar BMS de la serie Ghost (96S/192S y similares) mediante Bluetooth de baja energía (BLE).

Qué muestra:
- Voltaje, corriente y potencia de la batería en vivo
- Voltaje de cada celda (hasta 192 celdas, dos bancos independientes)
- Temperatura de los módulos de balanceo
- Estado de protección, estado de los relés de carga/descarga, alarmas
- Lectura (y, como hipótesis, escritura) de los ajustes principales del BMS
- Un modo demo sin BMS real, para probar la interfaz primero

Idiomas de la interfaz: Español, English, українська, Deutsch, Français, Italiano.

⚠ Importante:
Esta app fue creada por un desarrollador independiente mediante ingeniería inversa del protocolo BLE, sin acceso a la especificación oficial del fabricante. Algunos comandos de escritura de ajustes son una hipótesis, no confirmada oficialmente, y no se han probado en todas las versiones de hardware o firmware. El desarrollador NO garantiza la exactitud de las lecturas ni de los comandos, y NO se hace responsable de pérdidas materiales, daños al equipo u otras consecuencias derivadas del uso de esta app. Úsala bajo tu propia responsabilidad y verifica siempre los ajustes críticos en la app original del fabricante.

La app es gratuita, sin anuncios, no necesita conexión a internet y no envía nada a ningún servidor.

¿Tienes un BMS de otra versión o firmware? Los registros BLE de tu dispositivo son muy bienvenidos — ayudan a ampliar la compatibilidad. Escribe a oleksandr.kolodkin@ukr.net
```
