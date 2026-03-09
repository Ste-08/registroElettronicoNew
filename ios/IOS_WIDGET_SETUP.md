# iOS Widget Setup Guide

Per completare la configurazione del widget iOS, devi seguire questi passaggi su un **Mac con Xcode**:

## 1. Aggiungi il Widget Extension in Xcode

1. Apri `ios/Runner.xcworkspace` in Xcode
2. Vai su **File → New → Target**
3. Seleziona **Widget Extension**
4. Nome: `AgendaWidget`
5. **Deseleziona** "Include Configuration Intent"
6. Click **Finish**

## 2. Configura l'App Group

Per permettere all'app principale e al widget di condividere dati:

1. Seleziona il target **Runner** → **Signing & Capabilities**
2. Click **+ Capability** → **App Groups**
3. Aggiungi: `group.com.riccardocalligaro.registroelettronico`

4. Seleziona il target **AgendaWidgetExtension** → **Signing & Capabilities**
5. Click **+ Capability** → **App Groups**
6. Aggiungi lo stesso gruppo: `group.com.riccardocalligaro.registroelettronico`

## 3. Sostituisci il codice del Widget

Sostituisci il contenuto di `AgendaWidget/AgendaWidget.swift` con il file che si trova in:
`ios/AgendaWidget/AgendaWidget.swift`

## 4. Aggiorna il Flutter code per iOS

Modifica `lib/feature/agenda/data/datasource/local/agenda_widget_service.dart` per usare App Groups su iOS:

```dart
// Per iOS, usa UserDefaults con App Group
if (Platform.isIOS) {
  // I dati vengono già salvati in SharedPreferences
  // che su iOS usa UserDefaults standard
}
```

## 5. Build e Test

```bash
cd ios
pod install
cd ..
flutter build ios
```

## Note

- Il widget iOS richiede **iOS 14.0+**
- Il widget supporta tre dimensioni: Small, Medium, Large
- I colori si adattano automaticamente al tema chiaro/scuro del sistema
- Il widget si aggiorna automaticamente ogni ora
