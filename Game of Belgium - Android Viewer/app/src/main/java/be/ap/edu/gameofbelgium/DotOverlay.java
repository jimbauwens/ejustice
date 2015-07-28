package be.ap.edu.gameofbelgium;

import java.util.ArrayList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.MotionEvent;

/*
 * DotOverlay, an extension for OSMDroid that allows you to visualize dots
 * Much much faster than using markers
 *
 * (C) 2014 Jim Bauwens
 */

public class DotOverlay extends Overlay {
    final int ALPHA_LEVEL = 100;
    final int SPOT_SIZE = 5;

    private ArrayList<Spot> mPoints;
    private int mPointsPrecomputed;
    protected Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Rect lastClipBounds;
    private boolean isBeingTouched;

    public DotOverlay(ResourceProxy resourceProxy, MapView mapView) {
        super(resourceProxy);

        mPaint.setStyle(Paint.Style.FILL);
        mPoints = new ArrayList<Spot>();

        isBeingTouched = false;
    }

    public void addPoint(IGeoPoint aPoint, int color) {
        addPoint(aPoint.getLatitudeE6(), aPoint.getLongitudeE6(), color);
    }

    public void addPoint(int aLatitudeE6, int aLongitudeE6, int color) {
        mPoints.add(new Spot(new Point(aLatitudeE6, aLongitudeE6), color));
    }

    @Override
    protected void draw(Canvas canvas,  MapView mapView, boolean shadow) {
        boolean doNotRender = mapView.isAnimating() || isBeingTouched || !mapView.getScroller().isFinished();

        Projection currentProjection = mapView.getProjection();
        BoundingBoxE6 boundingBox = currentProjection.getBoundingBox();
        Point topLeft = currentProjection.toProjectedPixels(boundingBox.getLatNorthE6(), boundingBox.getLonWestE6(), null);
        Point bottomRight = currentProjection.toProjectedPixels(boundingBox.getLatSouthE6(), boundingBox.getLonEastE6(), null);
        Rect clipBounds = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);

        if (!doNotRender) {

            int last = this.mPointsPrecomputed;
            int width = canvas.getWidth(), height = canvas.getHeight();

            if (mBitmap == null || mBitmap.getWidth() != width) {
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);

                last = 0;
            }

            int size = this.mPoints.size();


            while (this.mPointsPrecomputed < size) {
                Point pt = this.mPoints.get(this.mPointsPrecomputed).getPoint();
                currentProjection.toProjectedPixels(pt.x, pt.y, pt);
                this.mPointsPrecomputed++;
            }

            if (lastClipBounds != null && !lastClipBounds.equals(clipBounds)) {
                last = 0;
                mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }

            lastClipBounds = clipBounds;

            for (int i = last; i < size; i++) {
                Spot spot = this.mPoints.get(i);
                Point p = spot.getPoint();
                Point point = currentProjection.toPixelsFromProjected(p, null);

                if (clipBounds.contains(p.x, p.y)) {
                    mPaint.setColor(spot.getColor());
                    mPaint.setAlpha(ALPHA_LEVEL);
                    mCanvas.drawCircle(point.x, point.y, SPOT_SIZE, mPaint);
                }
            }
        }

        Point lastPoint = new Point(lastClipBounds.left , lastClipBounds.top );
        Point newPoint = new Point(clipBounds.left, clipBounds.top);
        Point lastPointPixels = currentProjection.toPixelsFromProjected(lastPoint, null);
        Point newPointPixels = currentProjection.toPixelsFromProjected(newPoint, null);

        canvas.drawBitmap(mBitmap, lastPointPixels.x - newPointPixels.x, lastPointPixels.y - newPointPixels.x, mPaint);

    }


    @Override
    public boolean onTouchEvent(final MotionEvent event, final MapView mapView) {
        isBeingTouched = !(event.getAction() == MotionEvent.ACTION_UP);

        mapView.invalidate();
        return false;
    }

    private class Spot {
        private Point mPoint;
        private int mColor;

        public Spot(Point point, int color) {
            mPoint = point;
            mColor = color;
        }

        public int getColor() {
            return mColor;
        }

        public Point getPoint() {
            return mPoint;
        }
    }
}