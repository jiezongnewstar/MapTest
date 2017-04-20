package com.example.maptest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.opengltest.R;

public class MyMap extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = MyMap.class.getSimpleName();

	private static final long DOUBLE_CLICK_TIME_SPACE = 300;

	private float mCurrentScaleMax;
	private float mCurrentScale;
	private float mCurrentScaleMin;

	private float windowWidth, windowHeight;

	private Bitmap mBitmap;
	private Paint mPaint;

	private PointF mStartPoint, mapCenter;// mapCenter表示地图中心在屏幕上的坐标
	private long lastClickTime;// 记录上一次点击屏幕的时间，以判断双击事件
	private Status mStatus = Status.NONE;

	private float oldRate = 1;
	private float oldDist = 1;
	private float offsetX, offsetY;

	private boolean isShu = true;

	private boolean isHave = false;


	private boolean mIsLongPressed = false;

	private int i = 1;

	float lastX;
	float lastY;
	long lastDownTime;


	private enum Status {
		NONE, ZOOM, DRAG
	};

	private List<MarkObject> markList = new ArrayList<MarkObject>();

	public MyMap(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init();
	}

	public MyMap(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init();
	}

	public MyMap(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init();
	}

	private void init() {
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		// 获取屏幕的宽和高
		windowWidth = getResources().getDisplayMetrics().widthPixels;
		windowHeight = getResources().getDisplayMetrics().heightPixels - getStatusBarHeight();
		mPaint = new Paint();

		mStartPoint = new PointF();
		mapCenter = new PointF();
	}

	public void setBitmap(Bitmap bitmap) {
		this.mBitmap = bitmap;
		// 设置最小缩放为铺满屏幕，最大缩放为最小缩放的4倍
		mCurrentScaleMin = Math.min(windowHeight / mBitmap.getHeight(), windowWidth / mBitmap.getWidth());

		mCurrentScale = mCurrentScaleMin;

		mCurrentScaleMax = mCurrentScaleMin * 10;

		mapCenter.set(mBitmap.getWidth() * mCurrentScale / 2, mBitmap.getHeight() * mCurrentScale / 2);

		float bitmapRatio = mBitmap.getHeight() / mBitmap.getWidth();
		float winRatio = windowHeight / windowWidth;
		// 判断屏幕铺满的情况，isShu为true表示屏幕横向被铺满，为false表示屏幕纵向被铺满
		if (bitmapRatio <= winRatio) {
			isShu = true;
		} else {
			isShu = false;
		}
		draw();
	}

	/**
	 * 为当前地图添加标记
	 * 
	 * @param object
	 */
	public void addMark(MarkObject object) {
		markList.add(object);
		draw();

	}


	//删除
	public void removeMark(int i) {
		markList.remove(i);
		draw();
	}
	/**
	 * 地图放大
	 */
	public void zoomIn() {
		mCurrentScale *= 1.5f;
		if (mCurrentScale > mCurrentScaleMax) {
			mCurrentScale = mCurrentScaleMax;
		}
		draw();
	}

	/**
	 * 地图缩小
	 */
	public void zoomOut() {
		mCurrentScale /= 1.5f;
		if (mCurrentScale < mCurrentScaleMin) {
			mCurrentScale = mCurrentScaleMin;
		}
		if (isShu) {
			if (mapCenter.x - mBitmap.getWidth() * mCurrentScale / 2 > 0) {
				mapCenter.x = mBitmap.getWidth() * mCurrentScale / 2;
			} else if (mapCenter.x + mBitmap.getWidth() * mCurrentScale / 2 < windowWidth) {
				mapCenter.x = windowWidth - mBitmap.getWidth() * mCurrentScale
						/ 2;
			}
			if (mapCenter.y - mBitmap.getHeight() * mCurrentScale / 2 > 0) {
				mapCenter.y = mBitmap.getHeight() * mCurrentScale / 2;
			}
		} else {

			if (mapCenter.y - mBitmap.getHeight() * mCurrentScale / 2 > 0) {
				mapCenter.y = mBitmap.getHeight() * mCurrentScale / 2;
			} else if (mapCenter.y + mBitmap.getHeight() * mCurrentScale / 2 < windowHeight) {
				mapCenter.y = windowHeight - mBitmap.getHeight()
						* mCurrentScale / 2;
			}

			if (mapCenter.x - mBitmap.getWidth() * mCurrentScale / 2 > 0) {
				mapCenter.x = mBitmap.getWidth() * mCurrentScale / 2;
			}
		}
		draw();
	}

	// 处理拖拽事件
	private void drag(MotionEvent event) {
		PointF currentPoint = new PointF();
		currentPoint.set(event.getX(), event.getY());
		offsetX = currentPoint.x - mStartPoint.x;
		offsetY = currentPoint.y - mStartPoint.y;
		// 以下是进行判断，防止出现图片拖拽离开屏幕
		if (offsetX > 0
				&& mapCenter.x + offsetX - mBitmap.getWidth() * mCurrentScale
						/ 2 > 0) {
			offsetX = 0;
		}
		if (offsetX < 0
				&& mapCenter.x + offsetX + mBitmap.getWidth() * mCurrentScale
						/ 2 < windowWidth) {
			offsetX = 0;
		}
		if (offsetY > 0
				&& mapCenter.y + offsetY - mBitmap.getHeight() * mCurrentScale
						/ 2 > 0) {
			offsetY = 0;
		}
		if (offsetY < 0
				&& mapCenter.y + offsetY + mBitmap.getHeight() * mCurrentScale
						/ 2 < windowHeight) {
			offsetY = 0;
		}
		mapCenter.x += offsetX;
		mapCenter.y += offsetY;
		draw();
		mStartPoint = currentPoint;
	}

	// 处理多点触控缩放事件
	private void zoomAction(MotionEvent event) {
		float newDist = spacing(event);
		if (newDist > 10.0f) {
			mCurrentScale = oldRate * (newDist / oldDist);
			if (mCurrentScale < mCurrentScaleMin) {
				mCurrentScale = mCurrentScaleMin;
			} else if (mCurrentScale > mCurrentScaleMax) {
				mCurrentScale = mCurrentScaleMax;
			}

			if (isShu) {
				if (mapCenter.x - mBitmap.getWidth() * mCurrentScale / 2 > 0) {
					mapCenter.x = mBitmap.getWidth() * mCurrentScale / 2;
				} else if (mapCenter.x + mBitmap.getWidth() * mCurrentScale / 2 < windowWidth) {
					mapCenter.x = windowWidth - mBitmap.getWidth() * mCurrentScale / 2;
				}
				if (mapCenter.y - mBitmap.getHeight() * mCurrentScale / 2 > 0) {
					mapCenter.y = mBitmap.getHeight() * mCurrentScale / 2;
				}
			} else {

				if (mapCenter.y - mBitmap.getHeight() * mCurrentScale / 2 > 0) {
					mapCenter.y = mBitmap.getHeight() * mCurrentScale / 2;
				} else if (mapCenter.y + mBitmap.getHeight() * mCurrentScale
						/ 2 < windowHeight) {
					mapCenter.y = windowHeight - mBitmap.getHeight()
							* mCurrentScale / 2;
				}

				if (mapCenter.x - mBitmap.getWidth() * mCurrentScale / 2 > 0) {
					mapCenter.x = mBitmap.getWidth() * mCurrentScale / 2;
				}
			}
		}
		draw();
		
	}

	// 处理点击标记的事件
	private void clickAction(MotionEvent event) {

		int clickX = (int) event.getX();
		int clickY = (int) event.getY();

		for (MarkObject object : markList) {
			Bitmap location = object.getmBitmap();
			int objX = (int) (mapCenter.x - location.getWidth() / 2
					- mBitmap.getWidth() * mCurrentScale / 2 + mBitmap
					.getWidth() * object.getMapX() * mCurrentScale);
			int objY = (int) (mapCenter.y - location.getHeight()
					- mBitmap.getHeight() * mCurrentScale / 2 + mBitmap
					.getHeight() * object.getMapY() * mCurrentScale);
			// 判断当前object是否包含触摸点，在这里为了得到更好的点击效果，我将标记的区域放大了
			if (objX - location.getWidth() < clickX
					&& objX + location.getWidth() > clickX
					&& objY + location.getHeight() > clickY
					&& objY - location.getHeight() < clickY) {
				if (object.getMarkListener() != null) {
					object.getMarkListener().onMarkClick(clickX, clickY);
					isHave = true;
				}else {
					isHave = false;
				}
				break;
			}

		}

		if(mIsLongPressed){

			if (!isHave){
				MarkObject markObject = new MarkObject();

				Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.bgbtn);
				markObject.setmBitmap(drawTextAtBitmap(b,"1-"+i,(int)b.getWidth()*4/5,(int)(b.getHeight()*3/4)));
				markObject.setMapX((event.getX()/mCurrentScale- (mapCenter.x/mCurrentScale-(mBitmap.getWidth())/2))/mBitmap.getWidth());
				markObject.setMapY((event.getY()/mCurrentScale-(mapCenter.y/mCurrentScale-(mBitmap.getHeight())/2))/mBitmap.getHeight());
				addMark(markObject);
				i++;
				Log.e("点的屏幕的xy"+event.getX()+"----"+event.getY(),"图的xy"+mCurrentScale);

				isHave = false;
			}
		}else{
			//移动模式所做的事
		}
	}



	// 计算两个触摸点的距离
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	private void draw() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Canvas canvas = getHolder().lockCanvas();
				if (canvas != null && mBitmap != null) {
					canvas.drawColor(Color.GRAY);
					Matrix matrix = new Matrix();

					matrix.setScale(mCurrentScale, mCurrentScale, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);

					matrix.postTranslate(mapCenter.x - mBitmap.getWidth() / 2, mapCenter.y - mBitmap.getHeight() / 2);
					canvas.drawBitmap(mBitmap, matrix, mPaint);
					for (MarkObject object : markList) {
						Bitmap location = object.getmBitmap();
						matrix.setScale(1.0f, 1.0f);
						// 使用Matrix使得Bitmap的宽和高发生变化，在这里使用的mapX和mapY都是相对值
						matrix.postTranslate(
								mapCenter.x - location.getWidth() / 2
										- mBitmap.getWidth() * mCurrentScale
										/ 2 + mBitmap.getWidth()
										* object.getMapX() * mCurrentScale,
								mapCenter.y - location.getHeight()
										- mBitmap.getHeight() * mCurrentScale
										/ 2 + mBitmap.getHeight()
										* object.getMapY() * mCurrentScale);
						canvas.drawBitmap(location, matrix, mPaint);
					}

				}
				if (canvas != null) {
					getHolder().unlockCanvasAndPost(canvas);
				}
			}
		}).start();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// TODO Auto-generated method stub
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (event.getPointerCount() == 1) {
				// 如果两次点击时间间隔小于一定值，则默认为双击事件
				if (event.getEventTime() - lastClickTime < DOUBLE_CLICK_TIME_SPACE) {
					zoomIn();
				} else {
					mStartPoint.set(event.getX(), event.getY());
					mStatus = Status.DRAG;
				}
			}

			lastClickTime = event.getEventTime();
			lastDownTime =event.getDownTime();
			lastX = event.getX();
			lastY = event.getY();
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			float distance = spacing(event);
			if (distance > 10f) {
				mStatus = Status.ZOOM;
				oldDist = distance;
			}
			break;

		case MotionEvent.ACTION_MOVE:

			if (mStatus == Status.DRAG) {
				drag(event);
			} else if (mStatus == Status.ZOOM) {
				zoomAction(event);
			}
			//检测是否长按,在非长按时检测
			if(!mIsLongPressed){
				mIsLongPressed = isLongPressed(lastX, lastY, event.getX(), event.getY(), lastDownTime,event.getEventTime(),200);
			}
			break;
		case MotionEvent.ACTION_UP:
			lastClickTime = event.getEventTime();
			if (mStatus != Status.ZOOM) {
				clickAction(event);
			}
			mIsLongPressed = false;
		case MotionEvent.ACTION_POINTER_UP:
			oldRate = mCurrentScale;
			mStatus = Status.NONE;
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		draw();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (mBitmap != null) {
			mBitmap.recycle();
		}
		for (MarkObject object : markList) {
			if (object.getmBitmap() != null) {
				object.getmBitmap().recycle();
			}
		}
	}

	// 获得状态栏高度
	private int getStatusBarHeight() {
		Class<?> c = null;
		Object obj = null;
		Field field = null;
		int x = 0;
		try {
			c = Class.forName("com.android.internal.R$dimen");
			obj = c.newInstance();
			field = c.getField("status_bar_height");
			x = Integer.parseInt(field.get(obj).toString());
			return getResources().getDimensionPixelSize(x);
		} catch (Exception e1) {
			e1.printStackTrace();
			return 75;
		}
	}



	/**
	 *  处理图片
	 * @param bm 所要转换的bitmap
	 * @return 指定宽高的bitmap
	 */
	public static Bitmap zoomImg(Bitmap bm, int newWidth){
		// 获得图片的宽高
		int width = bm.getWidth();
		int height = bm.getHeight();
		// 计算缩放比例
		float scaleWidth = ((float) newWidth) / width;
		// 取得想要缩放的matrix参数
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleWidth);
		// 得到新的图片
		Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
		return newbm;
	}


	/**
	 * * 判断是否有长按动作发生 * @param lastX 按下时X坐标 * @param lastY 按下时Y坐标 *
	 *
	 * @param thisX
	 *            移动时X坐标 *
	 * @param thisY
	 *            移动时Y坐标 *
	 * @param lastDownTime
	 *            按下时间 *
	 * @param thisEventTime
	 *            移动时间 *
	 * @param longPressTime
	 *            判断长按时间的阀值
	 */
	static boolean isLongPressed(float lastX, float lastY, float thisX,
								 float thisY, long lastDownTime, long thisEventTime,
								 long longPressTime) {
		float offsetX = Math.abs(thisX - lastX);
		float offsetY = Math.abs(thisY - lastY);
		long intervalTime = thisEventTime - lastDownTime;
		if (offsetX <= 10 && offsetY <= 10 && intervalTime >= longPressTime) {
			return true;
		}
		return false;
	}



	/**
	 * 图片上画文字
	 *
	 * @param bitmap
	 * @param text  文字内容
	 * @param textX  文字X坐标
	 * @param textY  文字Y坐标
	 * @return Bitmap
	 */
	private Bitmap drawTextAtBitmap(Bitmap bitmap, String text, float textX, float textY) {
		int x = bitmap.getWidth();
		int y = bitmap.getHeight();

		// 创建一个和原图同样大小的位图
		Bitmap newbit = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(newbit);

		Paint paint = new Paint();

		// 在原始位置0，0插入原图
		canvas.drawBitmap(bitmap, 0, 0, paint);
		paint.setColor(Color.WHITE);
		paint.setTextSize(24);
		paint.setTextAlign(Paint.Align.CENTER);

		// 在原图指定位置写上字
		canvas.drawText(text, textX, textY, paint);

		canvas.save(Canvas.ALL_SAVE_FLAG);

		// 存储
		canvas.restore();

		return newbit;
	}

}
