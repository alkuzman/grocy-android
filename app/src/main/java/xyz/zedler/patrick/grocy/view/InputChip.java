package xyz.zedler.patrick.grocy.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import xyz.zedler.patrick.grocy.R;

public class InputChip extends LinearLayout {

    private final static String TAG = "InputChip";
    private final static boolean DEBUG = false;

    private Context context;
    private ImageView imageViewIcon;
    private FrameLayout frameLayoutContainer;
    private View viewClose;
    private TextView textView;
    private int width;

    public InputChip(@NonNull Context context) {
        super(context);

        this.context = context;
        init(null, -1, false, null);
    }

    public InputChip(
            Context context,
            String text,
            @DrawableRes int iconRes,
            boolean animate,
            Runnable onClose
    ) {
        super(context);

        this.context = context;
        init(text, iconRes, animate, onClose);
    }

    private void init(String text, int iconRes, boolean animate, Runnable onClose) {
        inflate(context, R.layout.view_input_chip, this);

        frameLayoutContainer = findViewById(R.id.frame_input_chip_container);
        imageViewIcon = findViewById(R.id.image_input_chip_icon);
        textView = findViewById(R.id.text_input_chip);
        viewClose = findViewById(R.id.view_input_chip_close);

        if(animate) {
            frameLayoutContainer.setAlpha(0);
            frameLayoutContainer.animate().alpha(1).setDuration(200).setStartDelay(200).start();
        }

        setIcon(iconRes);
        setText(text);

        viewClose.setOnClickListener(v -> {
            // DURATIONS
            int fade = 200, disappear = 300;
            // run action
            if(onClose!= null) new Handler().postDelayed(onClose, fade + disappear);
            // first fade out
            frameLayoutContainer.animate().alpha(0).setDuration(fade).start();
            // then make shape disappear
            width = frameLayoutContainer.getWidth();
            // animate height
            ValueAnimator animatorHeight = ValueAnimator.ofInt(frameLayoutContainer.getHeight(), 0);
            animatorHeight.addUpdateListener(
                    animation -> {
                        frameLayoutContainer.setLayoutParams(
                                new LayoutParams(width, (int) animation.getAnimatedValue())
                        );
                        frameLayoutContainer.invalidate();
                    }
            );
            animatorHeight.setInterpolator(new FastOutSlowInInterpolator());
            animatorHeight.setStartDelay(fade);
            animatorHeight.setDuration(disappear).start();
            // animate width
            ValueAnimator animatorWidth = ValueAnimator.ofInt(frameLayoutContainer.getWidth(), 0);
            animatorWidth.addUpdateListener(
                    animation -> width = (int) animation.getAnimatedValue()
            );
            animatorWidth.setInterpolator(new FastOutSlowInInterpolator());
            animatorWidth.setStartDelay(fade);
            animatorWidth.setDuration(disappear).start();
        });
    }

    public void setIcon(@DrawableRes int iconRes) {
        if(iconRes != -1) imageViewIcon.setImageResource(iconRes);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void change(String text) {
        textView.setText(text);
        // TODO: animate changes
    }
}
