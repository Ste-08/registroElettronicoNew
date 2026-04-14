import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:registro_elettronico/feature/grades/domain/model/grade_domain_model.dart';
import 'package:registro_elettronico/feature/grades/domain/model/grades_section.dart';
import 'package:registro_elettronico/feature/subjects/domain/model/subject_domain_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Service that bridges Flutter grades data to the native home screen widget.
/// Shows subject averages and overall average of the current term.
class GradesWidgetService {
  static const String _gradesKey = 'widget_grades_data';
  static const MethodChannel _channel =
      MethodChannel('com.riccardocalligaro.registro_elettronico/grades_widget');

  final SharedPreferences sharedPreferences;

  GradesWidgetService({this.sharedPreferences});

  /// Updates the widget data with grades and subjects information from the current period.
  Future<void> updateWidgetData({
    GradesPagesDomainModel gradesPagesDomainModel,
  }) async {
    if (gradesPagesDomainModel == null || gradesPagesDomainModel.periodsWithGrades.isEmpty) return;

    try {
      final now = DateTime.now();
      
      // Find current period
      PeriodWithGradesDomainModel currentPeriod;
      if (gradesPagesDomainModel.periodsWithGrades != null) {
        for (final periodWithGrades in gradesPagesDomainModel.periodsWithGrades) {
          if (periodWithGrades?.period?.start != null &&
              periodWithGrades?.period?.end != null &&
              periodWithGrades.period.start.isBefore(now) &&
              periodWithGrades.period.end.isAfter(now)) {
            currentPeriod = periodWithGrades;
            break;
          }
        }
      }
      
      // Fallback: If no current period is active (e.g. over the summer), pick the closest or the last one.
      if (currentPeriod == null && gradesPagesDomainModel.periodsWithGrades != null && gradesPagesDomainModel.periodsWithGrades.isNotEmpty) {
        int closestIndex = 0;
        int minDays = 366;
        for (var i = 0; i < gradesPagesDomainModel.periodsWithGrades.length; i++) {
          final end = gradesPagesDomainModel.periodsWithGrades[i]?.period?.end;
          if (end != null) {
            final diff = now.difference(end).inDays.abs();
            if (diff < minDays) {
              minDays = diff;
              closestIndex = i;
            }
          }
        }
        currentPeriod = gradesPagesDomainModel.periodsWithGrades[closestIndex];
      }

      if (currentPeriod == null) return;

      // Calculate subject averages from the already calculated period data
      final List<Map<String, dynamic>> subjectAverages = [];

      if (currentPeriod.gradesForList != null) {
        for (final periodGrade in currentPeriod.gradesForList) {
          final double average = periodGrade?.average;
          if (average != null && average > 0 && periodGrade.grades != null && periodGrade.grades.isNotEmpty) {
            int validGradesCount = periodGrade.grades.where((g) => _isValidGrade(g)).length;
            
            subjectAverages.add({
              'id': periodGrade.subject?.id ?? -1,
              'name': _truncateSubjectName(periodGrade.subject?.name ?? ''),
              'average': average,
              'gradesCount': validGradesCount,
            });
          }
        }
      }

      final double overallAverage = currentPeriod.average ?? 0.0;

      // Sort by average (worst first to highlight struggling subjects)
      subjectAverages.sort((a, b) => (a['average'] as double).compareTo(b['average'] as double));

      // Prepare JSON data
      final widgetData = {
        'overallAverage': overallAverage,
        'subjects': subjectAverages,
        'lastUpdate': DateTime.now().toIso8601String(),
      };

      final jsonString = json.encode(widgetData);
      await sharedPreferences.setString(_gradesKey, jsonString);

      // Notify native widget to refresh
      await _notifyWidgetUpdate();
    } catch (e) {
      print('GradesWidgetService: Failed to update widget data: $e');
    }
  }

  /// Checks if a grade is valid for average calculation.
  bool _isValidGrade(GradeDomainModel grade) {
    if (grade == null) return false;
    return grade.decimalValue != null &&
        grade.decimalValue != -1.00 &&
        (grade.cancelled == null || grade.cancelled == false) &&
        (grade.localllyCancelled == null || grade.localllyCancelled == false);
  }

  /// Truncates subject name to fit in widget.
  String _truncateSubjectName(String name) {
    if (name.length > 25) {
      return '${name.substring(0, 22)}...';
    }
    return name;
  }

  /// Tells the native side to refresh the grades widget.
  Future<void> _notifyWidgetUpdate() async {
    try {
      if (Platform.isAndroid) {
        await _channel.invokeMethod('updateGradesWidget');
      } else if (Platform.isIOS) {
        await _channel.invokeMethod('reloadGradesWidget');
      }
    } on MissingPluginException {
      // Expected when widget extension is not available
    } catch (e) {
      print('GradesWidgetService: Failed to notify widget: $e');
    }
  }
}
