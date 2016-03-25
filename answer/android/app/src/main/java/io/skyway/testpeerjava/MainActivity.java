package io.skyway.testpeerjava;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.view.View;

/**
 *
 * メイン画面
 *
 */

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final Context context = getApplicationContext();

		findViewById(R.id.btn_video).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, MediaActivity.class);
				startActivity(intent);
			}
		});

		findViewById(R.id.btn_data).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, DataActivity.class);
				startActivity(intent);
			}
		});
	}
}
