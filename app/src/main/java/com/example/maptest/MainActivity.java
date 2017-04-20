package com.example.maptest;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.maptest.MarkObject.MarkClickListener;
import com.example.opengltest.R;


public class MainActivity extends Activity {

	private MyMap sceneMap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sceneMap = (MyMap) findViewById(R.id.sceneMap);

		Bitmap b = MyMap.zoomImg(BitmapFactory.decodeResource(getResources(), R.drawable.test),getResources().getDisplayMetrics().widthPixels);

		ViewGroup.LayoutParams lp = sceneMap.getLayoutParams();

		lp.height=b.getHeight();

		lp.width = b.getWidth();

		sceneMap.setLayoutParams(lp);

		sceneMap.setBitmap(b);


		MarkObject markObject = new MarkObject();
		markObject.setMapX(0.34f);
		markObject.setMapY(0.5f);
		markObject.setmBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_marka));

		markObject.setMarkListener(new MarkClickListener() {

			@Override
			public void onMarkClick(int x, int y) {

				Toast.makeText(MainActivity.this, "点击覆盖物"+x+"----"+y, Toast.LENGTH_SHORT)
						.show();
			}
		});

		sceneMap.addMark(markObject);
		((Button) findViewById(R.id.button_in))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
//						sceneMap.zoomIn();
						sceneMap.removeMark(1);
					}
				});
		((Button) findViewById(R.id.button_out))
				.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						sceneMap.zoomOut();
					}
				});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}


}
