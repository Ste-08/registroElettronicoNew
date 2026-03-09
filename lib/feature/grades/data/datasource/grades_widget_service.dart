import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:registro_elettronico/feature/grades/domain/model/grade_domain_model.dart';
import 'package:registro_elettronico/feature/subjects/domain/model/subject_domain_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Service that bridges Flutter grades data to the native home screen widget.
/// Shows subject averages and overall average.
class GradesWidgetService {
  static const String _gradesKey = 'widget_grades_data';
  static const MethodChannel _channel =
      MethodChannel('com.riccardocalligaro.registro_elettronico/grades_widget');

  final SharedPreferences sharedPreferences;

  GradesWidgetService({this.sharedPreferences});

  /// Updates the widget data with grades and subjects information.
  /// Calculates average for each subject and overall average.
  Future<void> updateWidgetData({
    List<GradeDomainModel> grades,
    List<SubjectDomainModel> subjects,
  }) async {
    try {
      // Group grades by subject and calculate averages
      final Map<int, List<GradeDomainModel>> gradesBySubject = {};
      
      for (final grade in grades) {
        if (_isValidGrade(grade)) {
          if (!gradesBySubject.containsKey(grade.subjectId)) {
            gradesBySubject[grade.subjectId] = [];
          }
          gradesBySubject[grade.subjectId].add(grade);
        }
      }

      // Calculate subject averages
      final List<Map<String, dynamic>> subjectAverages = [];
      double overallSum = 0;
      int overallCount = 0;

      for (final subject in subjects) {
        final subjectGrades = gradesBySubject[subject.id] ?? [];
        if (subjectGrades.isNotEmpty) {
          double sum = 0;
          int count = 0;
          
          for (final grade in subjectGrades) {
            sum += grade.decimalValue;
            count++;
          }
          
          final average = sum / count;
          
          subjectAverages.add({
            'id': subject.id,
            'name': _truncateSubjectName(subject.name ?? ''),
            'average': average,
            'gradesCount': count,
          });
          
          overallSum += average;
          overallCount++;
        }
      }

      // Calculate overall average (average of subject averages)
      final double overallAverage = overallCount > 0 ? overallSum / overallCount : 0.0;

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
