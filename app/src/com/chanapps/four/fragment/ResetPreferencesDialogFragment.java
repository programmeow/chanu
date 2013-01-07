package com.chanapps.four.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.DialogFragment;
import android.widget.BaseAdapter;
import android.widget.Toast;
import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanWatchlist;

/**
* Created with IntelliJ IDEA.
* User: arley
* Date: 12/14/12
* Time: 12:44 PM
* To change this template use File | Settings | File Templates.
*/
public class ResetPreferencesDialogFragment extends DialogFragment {
    public static final String TAG = ResetPreferencesDialogFragment.class.getSimpleName();
    private SettingsFragment fragment;
    public ResetPreferencesDialogFragment(SettingsFragment fragment) {
        super();
        this.fragment = fragment;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return (new AlertDialog.Builder(getActivity()))
                .setMessage(R.string.dialog_reset_preferences_confirm)
                .setPositiveButton(R.string.dialog_reset,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences.Editor ed = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                                ed.clear();
                                ed.commit();
                                // I tried to call notifyStateChange on the root adapter instead but it does nothing
                                Activity activity = fragment.getActivity();
                                Intent intent = activity.getIntent();
                                activity.finish();
                                activity.startActivity(intent);
                                Toast.makeText(getActivity(), R.string.dialog_reset_preferences, Toast.LENGTH_SHORT).show();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // ignore
                            }
                        })
                .create();
    }
}