package com.riccardocalligaro.registro_elettronico;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GradesWidgetDataProvider implements RemoteViewsService.RemoteViewsFactory {

    private static final String PREFS_NAME = "FlutterSharedPreferences";
    private static final String GRADES_KEY = "flutter.widget_grades_data";

    private final Context context;
    private List<SubjectAverage> subjects = new ArrayList<>();

    public GradesWidgetDataProvider(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        loadSubjects();
    }

    @Override
    public void onDataSetChanged() {
        loadSubjects();
    }

    @Override
    public void onDestroy() {
        subjects.clear();
    }

    @Override
    public int getCount() {
        return subjects.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position >= subjects.size()) {
            return null;
        }

        SubjectAverage subject = subjects.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.grades_widget_item);

        views.setTextViewText(R.id.subject_name, subject.name);
        views.setTextViewText(R.id.subject_average, String.format("%.2f", subject.average));
        views.setTextColor(R.id.subject_average, getColorForAverage(subject.average));
        
        // Set indicator color
        views.setInt(R.id.subject_indicator, "setBackgroundColor", getColorForAverage(subject.average));

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadSubjects() {
        subjects.clear();
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String gradesJson = prefs.getString(GRADES_KEY, "{}");
            JSONObject data = new JSONObject(gradesJson);
            JSONArray subjectsArray = data.optJSONArray("subjects");
            
            if (subjectsArray != null) {
                for (int i = 0; i < subjectsArray.length(); i++) {
                    JSONObject subj = subjectsArray.getJSONObject(i);
                    String name = subj.optString("name", "");
                    double average = subj.optDouble("average", 0.0);
                    if (average > 0) {
                        subjects.add(new SubjectAverage(name, average));
                    }
                }
            }
            
            // Sort by average (worst first)
            subjects.sort((a, b) -> Double.compare(a.average, b.average));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getColorForAverage(double average) {
        if (average >= 7.5) return Color.parseColor("#FF4CAF50"); // Green
        if (average >= 6.0) return Color.parseColor("#FFFFEB3B"); // Yellow  
        if (average >= 5.0) return Color.parseColor("#FFFF9800"); // Orange
        return Color.parseColor("#FFF44336"); // Red
    }

    private static class SubjectAverage {
        String name;
        double average;

        SubjectAverage(String name, double average) {
            this.name = name;
            this.average = average;
        }
    }
}
