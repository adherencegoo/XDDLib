package com.example.xddlib.xddpref.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.example.xddlib.xddpref.ui.XddPrefContainer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by adher on 2017/7/14.
 * Usage:<br/>
 * <ol>
 *     <li>Create data:
 *          <br/>{@link XddPrefEnumData#XddPrefEnumData(Class, String, Object, Object, Object[])},
 *          <br/>{@link com.example.xddlib.xddpref.data.XddPrefBinaryData#XddPrefBinaryData(String, boolean, String, String)}</li>
 *     <li>Init: {@link #initAfterDataCreated(Context)}</li>
 *     <li>Mount UI: {@link #showDialog(Activity)}</li>
 *     <li>Use it: {@link XddPrefEnumData#getCachedValue()}</li>
 * </ol>
 */

public final class XddPrefUtils {
    private XddPrefUtils() {}
    private static final String TAG = XddPrefUtils.class.getSimpleName();

    //LinkedHashMap: make sure no elements with the same key
    private static final LinkedHashMap<String, XddPrefEnumData<?>> sXddPrefs = new LinkedHashMap<>();

    static void addPref(@NonNull final XddPrefEnumData pref) {
        sXddPrefs.put(pref.getKey(), pref);
    }

    /**Must be invoked after {@link com.example.xddlib.xddpref.data.NativePreferenceHelper#init(Context)}*/
    @SuppressWarnings("unused")
    static void removeAllPref() {
        sXddPrefs.clear();
        final SharedPreferences.Editor editor = NativePreferenceHelper.getInstance().edit();
        editor.clear();
        editor.apply();
    }

    public static void initAfterDataCreated(@NonNull final Context context) {
        NativePreferenceHelper.init(context);
        for (Map.Entry<String, XddPrefEnumData<?>> entry : sXddPrefs.entrySet()) {
            entry.getValue().init();
        }
    }

    public static void showDialog(@NonNull final Activity activity/*Dialog must be created via ActivityContext*/) {
        final XddPrefContainer dialogBody = XddPrefContainer.inflate(activity, null).init(sXddPrefs);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(TAG)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogBody.saveToNative();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(true)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialogBody.removeAllViews();
                    }
                })
                .setView(dialogBody);

        //show dialog
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialogBuilder.show();
            }
        });
    }


}
