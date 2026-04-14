import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:registro_elettronico/feature/agenda/domain/model/agenda_event_domain_model.dart';
import 'package:registro_elettronico/utils/date_utils.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Service that bridges Flutter agenda data to the native home screen widget.
/// Supports both Android and iOS widgets.
class AgendaWidgetService {
  static const String _eventsKey = 'widget_agenda_events';
  static const MethodChannel _channel =
      MethodChannel('com.riccardocalligaro.registro_elettronico/agenda_widget');

  final SharedPreferences sharedPreferences;

  AgendaWidgetService({this.sharedPreferences});

  /// Filters upcoming events for the next 14 days and saves them
  /// as JSON in SharedPreferences, then notifies the native widget to refresh.
  Future<void> updateWidgetData(List<AgendaEventDomainModel> allEvents) async {
    try {
      final now = DateTime.now();
      final maxDate = now.add(Duration(days: 14));
      
      final upcomingEvents = allEvents.where((event) {
        // Keep events from today up to 14 days in the future
        return event.begin.isAfter(now.subtract(Duration(days: 1))) && event.begin.isBefore(maxDate);
      }).toList();

      // Sort by begin time
      upcomingEvents.sort((a, b) => a.begin.compareTo(b.begin));

      // Convert to JSON for the native widget to read
      final eventsJson = upcomingEvents.map((e) {
        return {
          'id': e.id ?? 0,
          'title': e.title ?? '',
          'notes': e.notes ?? '',
          'author': e.author ?? '',
          'isFullDay': e.isFullDay ?? false,
          'labelColor': e.labelColor ?? '',
          'subjectName': e.subjectName ?? '',
          'beginDateMs': e.begin.millisecondsSinceEpoch,
        };
      }).toList();

      final jsonString = json.encode(eventsJson);
      await sharedPreferences.setString(_eventsKey, jsonString);

      // Notify native widget to refresh (Android or iOS)
      await _notifyWidgetUpdate();
    } catch (e) {
      // Silently fail — widget update is non-critical
      print('AgendaWidgetService: Failed to update widget data: $e');
    }
  }

  /// Tells the native side to refresh the home screen widget.
  Future<void> _notifyWidgetUpdate() async {
    try {
      if (Platform.isAndroid) {
        await _channel.invokeMethod('updateWidget');
      } else if (Platform.isIOS) {
        // iOS uses WidgetKit which automatically refreshes based on timeline
        // But we can trigger a refresh via the method channel
        await _channel.invokeMethod('reloadWidgets');
      }
    } on MissingPluginException {
      // Expected when widget extension is not available
    } catch (e) {
      print('AgendaWidgetService: Failed to notify widget: $e');
    }
  }
}
