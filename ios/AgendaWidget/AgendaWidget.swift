import WidgetKit
import SwiftUI

struct AgendaEvent: Codable, Identifiable {
    let id: Int
    let title: String
    let notes: String
    let author: String
    let isFullDay: Bool
    let labelColor: String
    let subjectName: String
    let beginDateMs: Int?
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> AgendaEntry {
        AgendaEntry(date: Date(), events: [])
    }

    func getSnapshot(in context: Context, completion: @escaping (AgendaEntry) -> ()) {
        let entry = AgendaEntry(date: Date(), events: loadEvents())
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        let events = loadEvents()
        let entry = AgendaEntry(date: Date(), events: events)
        
        // Update every hour
        let nextUpdate = Calendar.current.date(byAdding: .hour, value: 1, to: Date())!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        completion(timeline)
    }
    
    private func loadEvents() -> [AgendaEvent] {
        guard let userDefaults = UserDefaults(suiteName: "group.com.riccardocalligaro.registroelettronico"),
              let jsonString = userDefaults.string(forKey: "widget_agenda_events"),
              let jsonData = jsonString.data(using: .utf8) else {
            return []
        }
        
        do {
            let allEvents = try JSONDecoder().decode([AgendaEvent].self, from: jsonData)
            
            let calendar = Calendar.current
            if let tomorrowStart = calendar.date(byAdding: .day, value: 1, to: calendar.startOfDay(for: Date())),
               let tomorrowEnd = calendar.date(byAdding: .day, value: 1, to: tomorrowStart) {
                
                let startMs = Int(tomorrowStart.timeIntervalSince1970 * 1000)
                let endMs = Int(tomorrowEnd.timeIntervalSince1970 * 1000)
                
                return allEvents.filter {
                    guard let beginDateMs = $0.beginDateMs else { return false }
                    return beginDateMs >= startMs && beginDateMs < endMs
                }
            }
            return []
        } catch {
            return []
        }
    }
}

struct AgendaEntry: TimelineEntry {
    let date: Date
    let events: [AgendaEvent]
}

struct AgendaWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var family
    @Environment(\.colorScheme) var colorScheme
    
    var tomorrowDateString: String {
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: Date())!
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "it_IT")
        formatter.dateFormat = "EEEE d MMMM"
        return formatter.string(from: tomorrow).capitalized
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Header
            HStack {
                Image(systemName: "calendar")
                    .foregroundColor(.blue)
                Text("Agenda di domani")
                    .font(.headline)
                    .fontWeight(.bold)
                Spacer()
                Text(tomorrowDateString)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Divider()
            
            if entry.events.isEmpty {
                Spacer()
                HStack {
                    Spacer()
                    Text("Nessun evento per domani 🎉")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                Spacer()
            } else {
                ForEach(entry.events.prefix(family == .systemSmall ? 2 : 4)) { event in
                    HStack(spacing: 8) {
                        Rectangle()
                            .fill(colorForEvent(event))
                            .frame(width: 4)
                            .cornerRadius(2)
                        
                        VStack(alignment: .leading, spacing: 2) {
                            Text(event.title.isEmpty ? event.notes : event.title)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .lineLimit(1)
                            
                            if !event.author.isEmpty {
                                Text(event.author.capitalized)
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                        }
                        
                        Spacer()
                        
                        Image(systemName: iconForEvent(event))
                            .foregroundColor(.secondary)
                            .font(.caption)
                    }
                    .padding(.vertical, 4)
                }
                
                if entry.events.count > (family == .systemSmall ? 2 : 4) {
                    Text("+\(entry.events.count - (family == .systemSmall ? 2 : 4)) altri")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer(minLength: 0)
        }
        .padding()
    }
    
    func colorForEvent(_ event: AgendaEvent) -> Color {
        let notes = event.notes.lowercased()
        if notes.contains("verifica") || notes.contains("interrogazione") || notes.contains("compito in classe") {
            return .red
        } else if notes.contains("compito") || notes.contains("esercizi") {
            return .orange
        }
        return .blue
    }
    
    func iconForEvent(_ event: AgendaEvent) -> String {
        let notes = event.notes.lowercased()
        if notes.contains("verifica") || notes.contains("interrogazione") {
            return "exclamationmark.triangle.fill"
        } else if notes.contains("compito") {
            return "pencil"
        }
        return "calendar"
    }
}

@main
struct AgendaWidget: Widget {
    let kind: String = "AgendaWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
            AgendaWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Agenda di domani")
        .description("Mostra gli eventi dell'agenda per domani.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

struct AgendaWidget_Previews: PreviewProvider {
    static var previews: some View {
        AgendaWidgetEntryView(entry: AgendaEntry(date: Date(), events: []))
            .previewContext(WidgetPreviewContext(family: .systemMedium))
    }
}
