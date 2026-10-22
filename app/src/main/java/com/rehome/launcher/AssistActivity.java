package com.rehome.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Invisible activity: the steering-wheel voice button can open this, which calls the primary contact. */
public class AssistActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("autocall", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
