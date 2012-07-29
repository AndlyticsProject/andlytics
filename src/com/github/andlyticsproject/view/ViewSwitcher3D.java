
package com.github.andlyticsproject.view;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Animation.AnimationListener;

/**
 * This code was extracted from the Transition3D sample activity found in the Android ApiDemos. The
 * animation is made of two smaller animations: the first half rotates the list by 90 degrees on the
 * Y axis and the second half rotates the picture by 90 degrees on the Y axis. When the first half
 * finishes, the list is made invisible and the picture is set visible.
 */
public class ViewSwitcher3D {
	private static final String TAG = "ViewSwitcher";
	private ViewGroup mContainer;
	private View mFrondside;
	private View mBackside;

	private long mDuration = 600;
	private float mDepthOfRotation = 300f;

	private ViewSwitcherListener listener;

	public ViewSwitcher3D(ViewGroup container) {

		mContainer = container;
		mFrondside = container.getChildAt(0);
		mBackside = container.getChildAt(1);

		// Since we are caching large views, we want to keep their cache
		// between each animation
		mContainer.setPersistentDrawingCache(ViewGroup.PERSISTENT_ANIMATION_CACHE);

	}

	public void setDuration(long duration) {
		mDuration = duration;
	}

	public void swap() {
		float start, end;

		if (isFrontsideVisible()) {
			Log.v(TAG, "turning to the backside!");
			start = 0;
			end = 90;
		} else {
			Log.v(TAG, "turning to the frontside!");
			start = 180;
			end = 90;
		}

		Rotate3dAnimation rotation = new Rotate3dAnimation(start, end,
				mContainer.getWidth() / 2.0f, mContainer.getHeight() / 2.0f, mDepthOfRotation, true, getListener());
		rotation.setDuration(mDuration / 2);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new TurnAroundListener());

		mContainer.startAnimation(rotation);
	}

	public boolean isFrontsideVisible() {
		return mFrondside.getVisibility() == View.VISIBLE;
	}

	public boolean isBacksideVisible() {
		return mBackside.getVisibility() == View.VISIBLE;
	}

	/**
	 * Listen for the end of the first half of the animation. Then post a new action that
	 * effectively swaps the views when the container is rotated 90 degrees and thus invisible.
	 */
	private final class TurnAroundListener implements Animation.AnimationListener {

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			mContainer.post(new SwapViews());
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * Swapping the views and start the second half of the animation.
	 */
	private final class SwapViews implements Runnable {

		public void run() {
			final float centerX = mContainer.getWidth() / 2.0f;
			final float centerY = mContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (isFrontsideVisible()) {
				mFrondside.setVisibility(View.GONE);
				mBackside.setVisibility(View.VISIBLE);
				unmirrorTheBackside();
				mBackside.requestFocus();

				rotation = new Rotate3dAnimation(90, 180, centerX, centerY, mDepthOfRotation, false, getListener());
			} else {
				mBackside.setVisibility(View.GONE);
				mBackside.clearAnimation(); // remove the mirroring
				mFrondside.setVisibility(View.VISIBLE);
				mFrondside.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, mDepthOfRotation, false, getListener());
			}

			rotation.setDuration(mDuration / 2);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());
			rotation.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
				}

				@Override
				public void onAnimationRepeat(Animation animation) {
				}

				@Override
				public void onAnimationEnd(Animation animation) {
					if (listener != null) {
						listener.onViewChanged(isFrontsideVisible());
					}
				}
			});

			mContainer.startAnimation(rotation);

		}
	}

	private void unmirrorTheBackside() {
		Rotate3dAnimation rotation = new Rotate3dAnimation(0, 180, mContainer.getWidth() / 2.0f,
				mContainer.getHeight() / 2.0f, mDepthOfRotation, false, getListener());
		rotation.setDuration(0);
		rotation.setFillAfter(true);
		mBackside.startAnimation(rotation);
	}

	public void setListener(ViewSwitcherListener listener) {
		this.listener = listener;
	}

	public ViewSwitcherListener getListener() {
		return listener;
	}

	public interface ViewSwitcherListener {
		public void onViewChanged(boolean frontsideVisible);

		public void onRender();
	}
}
