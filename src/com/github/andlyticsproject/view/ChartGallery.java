
package com.github.andlyticsproject.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

public class ChartGallery extends Gallery {

	//  private static String LOG_TAG=ChartGallery.class.toString();
	private static final float SWIPE_MIN_DISTANCE = 100;

	private boolean interceptTouchEvents;

	private boolean useMultiImageFling;

	private boolean ignoreLayoutCalls;

	private boolean allowChangePageSliding = true;

	public ChartGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public ChartGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ChartGallery(Context context) {
		super(context);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!isIgnoreLayoutCalls())
			super.onLayout(changed, l, t, r, b);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		if (interceptTouchEvents) {
			return true;
		}
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

		if (useMultiImageFling) {
			return super.onFling(e1, e2, velocityX, velocityY);

		} else {
			boolean result = false;

			if (Math.abs(velocityX) > 900) {

				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && velocityX <= 0) {

					// hack - send event to simulate right key press
					KeyEvent rightKey = new KeyEvent(KeyEvent.ACTION_DOWN,
							KeyEvent.KEYCODE_DPAD_RIGHT);
					onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, rightKey);

					rightKey = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT);
					onKeyUp(KeyEvent.KEYCODE_DPAD_RIGHT, rightKey);

					result = true;

				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) {

					// hack - send event to simulate left key press
					KeyEvent leftKey = new KeyEvent(KeyEvent.ACTION_DOWN,
							KeyEvent.KEYCODE_DPAD_LEFT);
					onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, leftKey);

					leftKey = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT);
					onKeyUp(KeyEvent.KEYCODE_DPAD_LEFT, leftKey);

					result = true;
				}

			}

			return result;

		}
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		if (!allowChangePageSliding) {
			if(getSelectedView() != null && getSelectedView().getTag() != null) {
				int[] tag = (int[]) getSelectedView().getTag();
				if (distanceX < 0 && tag[1] <= 1)
					return true;
				if (distanceX > 0 && tag[1] >= (tag[2] - 1))
					return true;	
			}
		}
		return super.onScroll(e1, e2, distanceX, distanceY);
	}

	public void setInterceptTouchEvents(boolean interceptTouchEvents) {
		this.interceptTouchEvents = interceptTouchEvents;
	}

	public boolean isInterceptTouchEvents() {
		return interceptTouchEvents;
	}

	public void setUseMultiImageFling(boolean useMultiImageFling) {
		this.useMultiImageFling = useMultiImageFling;
	}

	public boolean isUseMultiImageFling() {
		return useMultiImageFling;
	}

	public void setIgnoreLayoutCalls(boolean ignoreLayoutCalls) {
		this.ignoreLayoutCalls = ignoreLayoutCalls;
	}

	public boolean isIgnoreLayoutCalls() {
		return ignoreLayoutCalls;
	}

	public void setAllowChangePageSliding(boolean allowChangePageSliding) {
		this.allowChangePageSliding = allowChangePageSliding;
	}

}
