package com.tonicartos.superslimexample;

import android.content.Context;
import android.content.Intent;

/**
 * Created by hesk on 26/8/15.
 */
public class IntentSwitcher {

    public static void switchIntent(int id, Context ctx) {
        Intent hk_scn_demo;
        if (id == R.id.loadingtest) {
            hk_scn_demo = new Intent(ctx, LoadingDataActivity.class);
        } else {
            return;
        }
        ctx.startActivity(hk_scn_demo);

    }
}
