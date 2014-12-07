package com.acesse.youchat;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class NetworkAlertDialogFragment extends DialogFragment {

    private static final String TAG = "YOUC";

    static NetworkAlertDialogFragment newInstance() {
        NetworkAlertDialogFragment f = new NetworkAlertDialogFragment();
        return f;
    }

    public NetworkAlertDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
        .setIcon(R.drawable.ic_error_icon)
        .setTitle(R.string.error_title)
        .setMessage(R.string.error_network_unreachable)
        .setNegativeButton(this.getResources().getString(R.string.ok),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        return builder.create();
    }
}