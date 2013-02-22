package com.chanapps.four.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.chanapps.four.component.GlobalAlarmReceiver;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanHelper;
import com.chanapps.four.service.FetchChanDataService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: mpop
 * Date: 11/22/12
 * Time: 11:30 PM
 */
public class BoardWidgetProvider extends AppWidgetProvider {

    public static final String TAG = BoardWidgetProvider.class.getSimpleName();

    public static final String WIDGET_CACHE_DIR = "widgets";

    private static final boolean DEBUG = false;

    public static int[] getAppWidgetIds(Context context) {
        ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
        return appWidgetIds;
    }

    public static Set<String> getActiveWidgetPref(Context context) {
        ComponentName widgetProvider = new ComponentName(context, BoardWidgetProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgetProvider);
        Set<Integer> activeWidgetIds = new HashSet<Integer>();
        for (int appWidgetId : appWidgetIds) {
            activeWidgetIds.add(appWidgetId);
        }

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetConf = pref.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());

        Set<String> savedWidgetConf = new HashSet<String>();
        for (String widget : widgetConf) {
            String widgetBoard = new String(widget);
            String[] components = widgetBoard.split("/");
            int widgetId = Integer.valueOf(components[0]);
            if (activeWidgetIds.contains(widgetId))
                savedWidgetConf.add(widgetBoard);
        }

        if (DEBUG) {
            Log.i(TAG, "Dumping active widget conf:");
            for (String widgetBoard : savedWidgetConf) {
                Log.i(TAG, widgetBoard);
            }
        }

        return savedWidgetConf;
    }

    public static void saveWidgetPref(Context context, Set<String> savedWidgetConf) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putStringSet(ChanHelper.PREF_WIDGET_BOARDS, savedWidgetConf)
                .commit();
    }

    public static String getBoardCodeForWidget(Context context, int appWidgetId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int widgetId = Integer.valueOf(components[0]);
            String widgetBoardCode = components[1];
            if (widgetId == appWidgetId)
                return widgetBoardCode;
        }
        return null;
    }

    public static void fetchAllWidgets(Context context) {
        if (DEBUG) Log.i(TAG, "fetchAllWidgets");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> boardsToFetch = new HashSet<String>();
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            String widgetBoardCode = components[1];
            boardsToFetch.add(widgetBoardCode);
        }
        for (String boardCode : boardsToFetch) {
            if (DEBUG) Log.i(TAG, "fetchAllWidgets board=" + boardCode + " scheduling fetch");
            FetchChanDataService.scheduleBoardFetch(context, boardCode);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // we handle this ourselves after widget configure
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (DEBUG) Log.i(TAG, "deleting widgets: " + Arrays.toString(appWidgetIds));
        Set<Integer> widgetsToDelete = new HashSet<Integer>();
        for (int i = 0; i < appWidgetIds.length; i++)
            widgetsToDelete.add(appWidgetIds[i]);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> newWidgetBoards = new HashSet<String>();
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int widgetId = Integer.valueOf(components[0]);
            if (!widgetsToDelete.contains(widgetId))
                newWidgetBoards.add(widgetBoard);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ChanHelper.PREF_WIDGET_BOARDS, newWidgetBoards);
        editor.commit();
    }

    @Override
    public void onEnabled(Context context) {
        // handled by config task
    }

    @Override
    public void onDisabled(Context context) {
        if (DEBUG) Log.i(TAG, "disabled all widgets");
        GlobalAlarmReceiver.cancelGlobalAlarm(context);
    }

    public static void update(Context context, int appWidgetId) {
        if (DEBUG) Log.i(TAG, "calling update widget service for widget=" + appWidgetId);
        Intent updateIntent = new Intent(context, UpdateWidgetService.class);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        context.startService(updateIntent);
    }

    public static void updateFirstTime(Context context, int appWidgetId) {
        if (DEBUG) Log.i(TAG, "calling first time update widget service for widget=" + appWidgetId);
        Intent updateIntent = new Intent(context, UpdateWidgetService.class);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        updateIntent.putExtra(ChanHelper.FIRST_TIME_INIT, true);
        context.startService(updateIntent);
        String boardCode = getBoardCodeForWidget(context, appWidgetId);
        FetchChanDataService.scheduleBoardFetch(context, boardCode); // make it fresh
    }

    public static void updateAll(Context context) {
        if (DEBUG) Log.i(TAG, "Updating all widgets");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int appWidgetId = Integer.valueOf(components[0]);
            String widgetBoardCode = components[1];
                update(context, appWidgetId);
        }

    }

    public static void updateAll(Context context, String boardCode) {
        if (DEBUG) Log.i(TAG, "updateAll boardCode=" + boardCode);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int widgetId = Integer.valueOf(components[0]);
            String widgetBoardCode = components[1];
            if (widgetBoardCode.equals(boardCode))
                update(context, widgetId);
        }
    }

    public static void initWidget(final Context context, final int appWidgetId, final String boardCode) {
        if (DEBUG) Log.i(TAG, "Configuring widget=" + appWidgetId + " with board=" + boardCode);
        if (boardCode == null || ChanBoard.getBoardByCode(context, boardCode) == null) {
            Log.e(TAG, "Couldn't find board=" + boardCode + " for widget=" + appWidgetId + " not adding widget");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> widgetBoards = prefs.getStringSet(ChanHelper.PREF_WIDGET_BOARDS, new HashSet<String>());
        Set<String> newWidgetBoards = new HashSet<String>();
        String newWidgetBoard = appWidgetId + "/" + boardCode;
        boolean found = false;
        for (String widgetBoard : widgetBoards) {
            String[] components = widgetBoard.split("/");
            int widgetId = Integer.valueOf(components[0]);
            if (widgetId == appWidgetId) {
                found = true;
                newWidgetBoards.add(newWidgetBoard);
            }
            else {
                newWidgetBoards.add(widgetBoard);
            }
        }
        if (!found) {
            newWidgetBoards.add(newWidgetBoard);
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ChanHelper.PREF_WIDGET_BOARDS, newWidgetBoards);
        editor.commit();
        updateFirstTime(context, appWidgetId);
    }


}