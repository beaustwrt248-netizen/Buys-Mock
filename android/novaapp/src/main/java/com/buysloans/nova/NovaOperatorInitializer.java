package com.buysloans.nova;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.WeakHashMap;

public final class NovaOperatorInitializer extends ContentProvider {
    private final WeakHashMap<Activity, ViewTreeObserver.OnGlobalLayoutListener> watchers = new WeakHashMap<>();

    @Override
    public boolean onCreate() {
        if (getContext() == null) return true;
        NovaAndroidOperator.scheduleBackgroundAlerts(getContext());
        Object app = getContext().getApplicationContext();
        if (app instanceof Application) {
            ((Application) app).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(Activity activity, Bundle state) {}
                @Override public void onActivityStarted(Activity activity) {}
                @Override public void onActivityPaused(Activity activity) {}
                @Override public void onActivityStopped(Activity activity) {}
                @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}

                @Override
                public void onActivityResumed(Activity activity) {
                    if (!(activity instanceof MainActivity)) return;
                    installWatcher(activity);
                    activity.getWindow().getDecorView().post(() -> {
                        NovaOperatorUi.install(activity);
                        UpdateManager.resumePendingInstall(activity);
                    });
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    View decor = activity.getWindow().getDecorView();
                    ViewTreeObserver.OnGlobalLayoutListener listener = watchers.remove(activity);
                    if (listener != null && decor.getViewTreeObserver().isAlive()) {
                        decor.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
                    }
                    if (activity instanceof MainActivity) NovaOperatorUi.clearSession();
                }
            });
        }
        return true;
    }

    private void installWatcher(Activity activity) {
        if (watchers.containsKey(activity)) return;
        View decor = activity.getWindow().getDecorView();
        ViewTreeObserver.OnGlobalLayoutListener listener = () -> decor.post(() -> NovaOperatorUi.install(activity));
        watchers.put(activity, listener);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
