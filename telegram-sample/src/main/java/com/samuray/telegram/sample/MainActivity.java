package com.samuray.telegram.sample;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView view = new TextView(this);
        view.setText(
                "Telegram Core\n\n"
                        + "Standalone user-account client foundation.\n"
                        + "Authentication, chats, history, text and photo APIs are provided by the embedded client module.");
        view.setPadding(40, 40, 40, 40);
        setContentView(view);
    }
}
