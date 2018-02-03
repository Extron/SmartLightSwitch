package com.iot.extron.smartlightswitch.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.iot.extron.smartlightswitch.R;


public class ColorPickerView extends View
{
    //region Static Fields

    /** The value that <code>selectedColor</code> takes on if there is no selected color. */
    private static final int NO_COLOR = 0;

    /** The default size to make the selection radius. */
    private static final int DEFAULT_SELECTION_RADIUS = 16;

    /** The default size to make the hue wheel border. */
    private static final int DEFAULT_BORDER_SIZE = 32;

    /** The default size to make the selection border. */
    private static final int DEFAULT_SELECTION_BORDER_SIZE = 4;

    //endregion


    //region Fields

    /** The current color change listener. */
    OnSelectedColorChangedListener onSelectedColorChangedListener;

    /** The currently selected color in the color picker. */
    int selectedColor = NO_COLOR;

    /** The radius in pixels to draw the color selection circle. */
    int selectionRadius = DEFAULT_SELECTION_RADIUS;

    /** The color of the border of the hue wheel. */
    int borderColor = Color.BLACK;

    /** The size in pixels to draw the hue wheel border. */
    int borderSize = DEFAULT_BORDER_SIZE;

    /** The color of the border of the selection circle. */
    int selectionBorderColor = Color.BLACK;

    /** The size in pixels to draw the color selection circle border. */
    int selectionBorderSize = DEFAULT_SELECTION_BORDER_SIZE;

    //endregion


    //region UI Elements

    Bitmap hueWheel;
    Paint hueWheelBorderBrush;
    Paint selectionBrush;
    Paint selectionBorderBrush;

    //endregion


    //region Constructors

    public ColorPickerView(Context context)
    {
        super(context);
        initialize(null, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initialize(attrs, defStyle);
    }

    //endregion


    //region View Methods

    private void initialize(AttributeSet attrs, int defStyle)
    {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPickerView, defStyle, 0);

        selectionRadius = a.getDimensionPixelSize(R.styleable.ColorPickerView_selectionRadius, DEFAULT_SELECTION_RADIUS);
        borderSize = a.getDimensionPixelSize(R.styleable.ColorPickerView_borderSize, DEFAULT_BORDER_SIZE);
        selectionBorderSize = a.getDimensionPixelSize(R.styleable.ColorPickerView_selectionBorderSize, DEFAULT_SELECTION_BORDER_SIZE);

        int borderColorRes = a.getResourceId(R.styleable.ColorPickerView_borderColor, -1);
        if (borderColorRes > -1)
            borderColor = getResources().getColor(borderColorRes, null);

        int selectionBorderColorRes = a.getResourceId(R.styleable.ColorPickerView_selectionBorderColor, -1);
        if (selectionBorderColorRes > -1)
            selectionBorderColor = getResources().getColor(selectionBorderColorRes, null);

        hueWheelBorderBrush = new Paint(Paint.ANTI_ALIAS_FLAG);
        hueWheelBorderBrush.setColor(borderColor);
        hueWheelBorderBrush.setStyle(Paint.Style.STROKE);
        hueWheelBorderBrush.setStrokeWidth(0.5f * borderSize);

        selectionBrush = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionBrush.setStyle(Paint.Style.FILL);
        selectionBrush.setColor(selectedColor);

        selectionBorderBrush = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionBorderBrush.setColor(selectionBorderColor);
        selectionBorderBrush.setStyle(Paint.Style.STROKE);
        selectionBorderBrush.setStrokeWidth(0.5f * selectionBorderSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH)
    {
        super.onSizeChanged(w, h, oldW, oldH);

        int size = Math.min(w, h) - borderSize;
        hueWheel = generateHueWheel(size);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());

        canvas.drawBitmap(hueWheel, 0.5f * (getWidth() - size + borderSize), 0.5f * (getHeight() - size + borderSize), null);
        canvas.drawCircle(0.5f * getWidth(), 0.5f * getHeight(), 0.5f * size - 0.25f * borderSize - 4, hueWheelBorderBrush);

        if (selectedColor != NO_COLOR)
        {
            Point point = getColorCoordinates(selectedColor);
            canvas.drawCircle(point.x, point.y, selectionRadius, selectionBrush);
            canvas.drawCircle(point.x, point.y, selectionRadius, selectionBorderBrush);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean result = false;

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                int color = getColorAtCoordinates(new Point((int)event.getX(), (int)event.getY()));

                if (color != NO_COLOR)
                {
                    setSelectedColor(color);
                    result = true;
                }
                break;
        }

        return result;
    }

    //endregion


    //region Get/Set Methods

    /** Gets the picker's selected color.
     * @return The selected color.
     */
    public int getSelectedColor()
    {
        return selectedColor;
    }

    /** Sets the picker's selected color.
     * @param color The color to set.
     */
    public void setSelectedColor(int color)
    {
        // The hue wheel colors always have their V component set to 1, so set the incoming color appropriately.
        float[] hsv = new float[3];

        Color.colorToHSV(color, hsv);
        hsv[2] = 1;

        selectedColor = Color.HSVToColor(hsv);
        selectionBrush.setColor(selectedColor);

        if (onSelectedColorChangedListener != null)
            onSelectedColorChangedListener.colorChanged(this, selectedColor);

        invalidate();
    }

    /** Gets the border color of the hue wheel.
     * @return The border color.
     */
    public int getBorderColor()
    {
        return borderColor;
    }

    /** Gets the border color of the circle the picker displays the selected color in.
     * @param color The border color.
     */
    public void setBorderColor(int color)
    {
        borderColor = color;
        hueWheelBorderBrush.setColor(borderColor);

        invalidate();
    }

    /** Gets the size of the border in pixels of the hue wheel.
     * @return The border size in pixels.
     */
    public int getBorderSize()
    {
        return borderSize;
    }

    /** Sets the border size in pixels of the hue wheel.
     * @param size The border size, in pixels.
     */
    public void setBorderSize(int size)
    {
        borderSize = size;
        hueWheelBorderBrush.setStrokeWidth(0.5f * borderSize);
        invalidate();
    }

    /** Gets the radius in pixels of the circle the picker displays the selected color in.
     * @return The selection radius in pixels.
     */
    public int getSelectionRadius()
    {
        return selectionRadius;
    }

    /** Sets the radius in pixels of the circle the picker displays the selected color in.
     * @param radius The radius, in pixels.
     */
    public void setSelectionRadius(int radius)
    {
        selectionRadius = radius;
        invalidate();
    }

    /** Gets the border color of the circle the picker displays the selected color in.
     * @return The selection border color.
     */
    public int getSelectionBorderColor()
    {
        return selectionBorderColor;
    }

    /** Gets the border color of the circle the picker displays the selected color in.
     * @param color The border color.
     */
    public void setSelectionBorderColor(int color)
    {
        selectionBorderColor = color;
        selectionBorderBrush.setColor(selectionBorderColor);

        invalidate();
    }

    /** Gets the size of the border in pixels of the circle the picker displays the selected color in.
     * @return The border size in pixels.
     */
    public int getSelectionBorderSize()
    {
        return selectionBorderSize;
    }

    /** Sets the border size in pixels of the circle the picker displays the selected color in.
     * @param size The border size, in pixels.
     */
    public void setSelectionBorderSize(int size)
    {
        selectionBorderSize = size;
        selectionBorderBrush.setStrokeWidth(0.5f * selectionBorderSize);
        invalidate();
    }

    /** Sets the listener that receives callbaks when the color picker's selected color has changed.
     * @param listener The listener.
     */
    public void setOnSelectedColorChangedListener(OnSelectedColorChangedListener listener)
    {
        onSelectedColorChangedListener = listener;
    }

    //endregion


    //region Hue Wheel Methods

    private Bitmap generateHueWheel(int size)
    {
        int[] pixels = new int[size * size];

        // Computing some important and useful values here to reduce compute time.
        double R = 0.5 * size;
        double invR = 1.0 / R;
        double radToDeg = 180.0 / Math.PI;

        for (int i = 0; i < size; i++)
        {
            for (int j = 0; j < size; j++)
            {
                int index = j * size + i;

                double x = R - i;
                double y = R - j;

                // In a hue wheel, a color is determined by the polar coordinates of the pixel.  The normalized radius becomes the saturation and the angle (in degrees) becomes the hue.
                double sat = invR * Math.sqrt(x * x + y * y);
                double hue =  radToDeg * (Math.atan2(y, x) + Math.PI);

                if (sat > 1)
                    pixels[index] = 0;
                else
                    pixels[index] = Color.HSVToColor(new float[]{ (float)hue, (float)sat, 1.0f });
            }
        }

        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
    }

    /** Gets the view space coordinates for the specified color from a hue wheel.
     * @param color The ARGB color to get coordinates for.
     * @return The view space coordinates.
     */
    private Point getColorCoordinates(int color)
    {
        float[] hsv = new float[3];

        Color.colorToHSV(color, hsv);

        // Hue is the angle of the polar coordinates, and saturation is the radius.
        int size = Math.min(getWidth(), getHeight()) - borderSize;
        double R = 0.5 * size;

        double r = R * hsv[1];
        double theta = Math.PI * hsv[0] / 180.0;

        double x = r * Math.cos(theta);
        double y = r * Math.sin(theta);

        // At this point, x and y are with respect to the center of the circle.  Need to translate back into top-left coordinates.
        return new Point((int)(x + 0.5 * getWidth()), (int)(y + 0.5 * getHeight()));
    }

    /** Gets the color within the hue wheel at the specified view space coordinates.  If the coordinates are not in the hue wheel, returns <code>NO_COLOR</code>.
     * @param coordinates The view space coordinates.
     * @return The color at the coordinates, or <code>NO_COLOR</code> if the coordinates are not in the hue wheel.
     */
    private int getColorAtCoordinates(Point coordinates)
    {
        // We must first check to see if the coordinates are in the hue wheel.  If not, return NO_COLOR.
        int size = Math.min(getWidth(), getHeight()) - borderSize;

        double x = 0.5 * getWidth() - coordinates.x;
        double y = 0.5 * getHeight() - coordinates.y;

        double R = 0.5 * size;
        double r = Math.sqrt(x * x + y * y);

        if (R < r)
            return NO_COLOR;

        // The normalized radius becomes the saturation and the angle becomes the hue.

        double sat = r / R;
        double hue = (180.0 / Math.PI) * (Math.atan2(y, x) + Math.PI);

        return Color.HSVToColor(new float[]{ (float)hue, (float)sat, 1.0f });
    }

    //endregion


    //region Callback interfaces

    /** A listener that reports when the selected color of the color picker has changed. */
    public interface OnSelectedColorChangedListener
    {
        /** Raised when the selected color has changed.
         * @param view The view that raised the event.
         * @param newColor The new selected color.
         */
        public void colorChanged(View view, int newColor);
    }

    //endregion
}
