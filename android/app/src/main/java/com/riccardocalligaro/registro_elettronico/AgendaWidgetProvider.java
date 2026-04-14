package com.riccardocalligaro.registro_elettronico;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AgendaWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "com.riccardocalligaro.registro_elettronico.WIDGET_REFRESH";
    private static final String PREFS_NAME = "FlutterSharedPreferences";
    private static final String EVENTS_KEY = "flutter.widget_agenda_events";

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
                    new ComponentName(context, AgendaWidgetProvider.class));
            // Notify the list to reload data
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_events_list);
            onUpdate(context, appWidgetManager, ids);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.agenda_widget_layout);

        // Set tomorrow's date in the header
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);
        long startOfTomorrow = tomorrow.getTimeInMillis();
        long endOfTomorrow = startOfTomorrow + 24 * 60 * 60 * 1000;

        SimpleDateFormat sdf = new SimpleDateFormat("EEEE d MMMM", Locale.ITALIAN);
        String dateStr = sdf.format(tomorrow.getTime());
        // Capitalize first letter
        dateStr = dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1);
        views.setTextViewText(R.id.widget_date, dateStr);

        // Check if there are events for tomorrow
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String eventsJson = prefs.getString(EVENTS_KEY, "[]");
        boolean hasEvents = false;
        try {
            JSONArray arr = new JSONArray(eventsJson);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                long beginDateMs = obj.optLong("beginDateMs", 0);
                if (beginDateMs >= startOfTomorrow && beginDateMs < endOfTomorrow) {
                    hasEvents = true;
                    break;
                }
            }
        } catch (Exception e) {
            hasEvents = false;
        }

        if (hasEvents) {
            views.setViewVisibility(R.id.widget_events_list, View.VISIBLE);
            views.setViewVisibility(R.id.widget_empty_text, View.GONE);
        } else {
            views.setViewVisibility(R.id.widget_events_list, View.GONE);
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE);
        }

        // Set up the RemoteViews adapter for the ListView
        Intent serviceIntent = new Intent(context, AgendaWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(R.id.widget_events_list, serviceIntent);
        views.setEmptyView(R.id.widget_events_list, R.id.widget_empty_text);

        // Click on widget opens the agenda page
        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setAction("OPEN_AGENDA");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, flags);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Called from Flutter via MethodChannel to refresh the widget.
     */
    public static void refreshWidget(Context context) {
        Intent intent = new Intent(context, AgendaWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        context.sendBroadcast(intent);
    }
}
