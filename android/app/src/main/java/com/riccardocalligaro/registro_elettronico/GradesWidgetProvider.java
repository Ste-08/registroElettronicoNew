package com.riccardocalligaro.registro_elettronico;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

public class GradesWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.riccardocalligaro.registro_elettronico.GRADES_WIDGET_REFRESH";
    private static final String PREFS_NAME = "FlutterSharedPreferences";
    private static final String GRADES_KEY = "flutter.widget_grades_data";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, GradesWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.grades_list);
            onUpdate(context, appWidgetManager, ids);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.grades_widget_layout);

        // Load grades data
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String gradesJson = prefs.getString(GRADES_KEY, "{}");
        
        try {
            JSONObject data = new JSONObject(gradesJson);
            double overallAverage = data.optDouble("overallAverage", 0.0);
            JSONArray subjects = data.optJSONArray("subjects");
            
            // Set overall average
            if (overallAverage > 0) {
                views.setTextViewText(R.id.overall_average, String.format("%.2f", overallAverage));
                views.setTextColor(R.id.overall_average, getColorForAverage(overallAverage));
            } else {
                views.setTextViewText(R.id.overall_average, "--");
            }
            
            // Show/hide empty view and list
            if (subjects == null || subjects.length() == 0) {
                views.setViewVisibility(R.id.grades_list, View.GONE);
                views.setViewVisibility(R.id.empty_view, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.grades_list, View.VISIBLE);
                views.setViewVisibility(R.id.empty_view, View.GONE);
                
                // Set up subject list
                Intent serviceIntent = new Intent(context, GradesWidgetService.class);
                serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
                views.setRemoteAdapter(R.id.grades_list, serviceIntent);
            }
            
        } catch (Exception e) {
            views.setTextViewText(R.id.overall_average, "--");
            views.setViewVisibility(R.id.grades_list, View.GONE);
            views.setViewVisibility(R.id.empty_view, View.VISIBLE);
        }

        // Click on widget opens the grades page
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setAction("OPEN_GRADES");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, launchIntent, flags);
        views.setOnClickPendingIntent(R.id.grades_widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private int getColorForAverage(double average) {
        if (average >= 7.5) return Color.parseColor("#FF4CAF50"); // Green
        if (average >= 6.0) return Color.parseColor("#FFFFEB3B"); // Yellow
        if (average >= 5.0) return Color.parseColor("#FFFF9800"); // Orange
        return Color.parseColor("#FFF44336"); // Red
    }

    public static void refreshWidget(Context context) {
        Intent intent = new Intent(context, GradesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        context.sendBroadcast(intent);
    }
}
