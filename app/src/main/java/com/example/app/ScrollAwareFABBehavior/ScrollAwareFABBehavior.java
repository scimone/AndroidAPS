package com.example.app.ScrollAwareFABBehavior;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class ScrollAwareFABBehavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
    private static boolean mIsAnimatingOut = false;
    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();

    public ScrollAwareFABBehavior(Context context, AttributeSet attrs) {
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton fabButton, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fabButton, View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        fabButton.setTranslationY(translationY);
        return true;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
                                       FloatingActionButton fabButton, View directTargetChild, View target, int nestedScrollAxes ,int type) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout, fabButton, directTargetChild, target,
                        nestedScrollAxes, type);

    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton fabButton,
                               View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed , int type) {
        super.onNestedScroll(coordinatorLayout, fabButton, target, dxConsumed, dyConsumed, dxUnconsumed,
                dyUnconsumed, type);

            if (dyConsumed > 0 && !mIsAnimatingOut && fabButton.isShown()) {
                animateOut(fabButton);
                //fabButton.setEnabled(false);
                //fabButton.setClickable(false);
                //fabButton.setAlpha(0.3f);
            } else if ((dyConsumed <= 0 || ( dyUnconsumed < 0) && fabButton.isOrWillBeHidden())) {
                animateIn(fabButton);
                //fabButton.setEnabled(true);
                //fabButton.setClickable(true);
                //fabButton.setAlpha(1.0f);
        }
    }

    public static void animateOut(final FloatingActionButton fabButton) {
            Animation anim = AnimationUtils.loadAnimation(fabButton.getContext(), android.R.anim.fade_in);
            anim.setInterpolator(INTERPOLATOR);
            anim.setDuration(100L);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mIsAnimatingOut = true;
                }

                public void onAnimationEnd(Animation animation) {
                    mIsAnimatingOut = false;
                    fabButton.setEnabled(false);
                    fabButton.setClickable(false);
                    fabButton.setAlpha(0.3f);
                }

                @Override
                public void onAnimationRepeat(final Animation animation) {
                }
            });
            fabButton.startAnimation(anim);
        }

    public static void animateIn(FloatingActionButton fabButton) {
        fabButton.setEnabled(true);
        fabButton.setClickable(true);
        fabButton.setAlpha(1.0f);
        ViewCompat.animate(fabButton).translationY(0).scaleX(1.0F).scaleY(1.0F).alpha(1.0F)
                .setInterpolator(INTERPOLATOR).withLayer().setListener(null)
                .setDuration(100L)
                .start();

        }
    }