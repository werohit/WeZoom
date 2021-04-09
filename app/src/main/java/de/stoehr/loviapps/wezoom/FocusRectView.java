package de.stoehr.loviapps.wezoom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

public class FocusRectView extends View {
    private Paint focusRectPaint;
    private boolean isTouched = false;
    private Rect touchRect;

    public FocusRectView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Paint paint = new Paint();
        this.focusRectPaint = paint;
        paint.setColor(ContextCompat.getColor(context, R.color.md_grey_300));
        this.focusRectPaint.setStyle(Paint.Style.STROKE);
        this.focusRectPaint.setStrokeWidth(3.0f);
        this.focusRectPaint.setShadowLayer(4.0f, 0.0f, 0.0f, ContextCompat.getColor(context, R.color.md_grey_900));
        setLayerType(1, this.focusRectPaint);
        this.isTouched = false;
    }

    public void setIsTouched(boolean z, Rect rect) {
        this.isTouched = z;
        this.touchRect = rect;
    }

    public void setFocusSuccessColor() {
        this.focusRectPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_green_700));
    }

    public void setFocusFailedColor() {
        this.focusRectPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_red_700));
    }

    public void setBasicColor() {
        this.focusRectPaint.setColor(ContextCompat.getColor(getContext(), R.color.md_grey_300));
    }

    public void onDraw(Canvas canvas) {
        if (this.isTouched) {
            canvas.drawRect((float) this.touchRect.left, (float) this.touchRect.top, (float) this.touchRect.right, (float) this.touchRect.bottom, this.focusRectPaint);
        }
    }
}
