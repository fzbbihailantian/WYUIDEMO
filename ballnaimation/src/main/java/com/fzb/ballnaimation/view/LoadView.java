package com.fzb.ballnaimation.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import com.fzb.ballnaimation.R;

public class LoadView extends SurfaceView implements SurfaceHolder.Callback,Runnable {
    private enum LoadingState {
        DOWN, UP, FREE
    }

    private LoadingState loadingState = LoadingState.DOWN;
    private int ballColor;//小球颜色
    private int ballRadius;//小球半径
    private int lineColor;//连线颜色
    private int lineWidth;//连线长度
    private int strokeWidth;//绘制线宽
    private float downDistance = 0;//水平位置下降的距离
    private float maxDownDistance;//水平位置下降的距离（最低点）
    private float upDistance = 0;//从底部上弹的距离
    private float freeDownDistance = 0;//自由落体的距离
    private float maxFreeDownDistance;//自由落体的距离(最高点)
    private ValueAnimator downControl;
    private ValueAnimator upControl;
    private ValueAnimator freeDownControl;
    private AnimatorSet animatorSet;
    private boolean isAnimationShowing;
    private SurfaceHolder holder;
    private Canvas canvas;
    private Paint paint;
    private Path path;
    private boolean isRunning;//标志新线程是否在运行

    public LoadView(Context context) {
        this(context,null);
    }

    public LoadView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public LoadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initattrs(context,attrs);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidth);
        path = new Path();
        holder = getHolder();
        //geiSurfaceHolder添加回调
        holder.addCallback(this);
        //初始化动作控制
        initContorl();
    }

    private void initContorl() {
        downControl = ValueAnimator.ofFloat(0, maxDownDistance);
        downControl.setDuration(500);
        downControl.setInterpolator(new DecelerateInterpolator());
        downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                downDistance = (float) animation.getAnimatedValue();
            }
        });
        downControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.DOWN;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        upControl = ValueAnimator.ofFloat(0, maxDownDistance);
        upControl.setDuration(500);
        upControl.setInterpolator(new ShockInterpolator());
        upControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                upDistance = (float) animation.getAnimatedValue();
                if (upDistance >= maxDownDistance && freeDownControl != null && !freeDownControl.isRunning() &&
                        !freeDownControl.isStarted()) {
                    freeDownControl.start();
                }
            }
        });
        upControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.UP;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });
        freeDownControl = ValueAnimator.ofFloat(0, (float) (2 * Math.sqrt(maxFreeDownDistance / 5)));
        freeDownControl.setDuration(500);
        freeDownControl.setInterpolator(new AccelerateInterpolator());
        freeDownControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();
                //v0t-1/2gt^2
                //v0=10*Math.sqrt(maxFreeDownDistance / 5)
                freeDownDistance = (float) (10 * Math.sqrt(maxFreeDownDistance / 5) * t - 5 * t * t);
            }
        });
        freeDownControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.FREE;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationShowing = false;

                //重新开启动画
                startAllAnimator();
            }
        });
        animatorSet = new AnimatorSet();
        animatorSet.play(downControl).before(upControl);
    }

    public void startAllAnimator() {
        if (isAnimationShowing) {
            return;
        }
        if (animatorSet.isRunning()) {
            animatorSet.end();
            animatorSet.cancel();
        }
        loadingState = LoadingState.DOWN;
        isRunning=true;
        new Thread(this).start();//绘制线程开启
        //动画开启
        animatorSet.start();
    }


    private void initattrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadView);
        ballColor = typedArray.getColor(R.styleable.LoadView_ballcolor, Color.BLUE);
        lineColor = typedArray.getColor(R.styleable.LoadView_linecolor, Color.BLUE);
        lineWidth = typedArray.getDimensionPixelOffset(R.styleable.LoadView_linelenght, 200);
        strokeWidth = typedArray.getDimensionPixelOffset(R.styleable.LoadView_linewidth, 4);
        maxDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadView_maxdown, 50);
        maxFreeDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadView_maxup, 50);
        ballRadius = typedArray.getDimensionPixelSize(R.styleable.LoadView_bollradius, 10);
        typedArray.recycle();//用完回收
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isRunning = true;
        drawView();//绘制
    }



    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void run() {
        while (isRunning) {
            drawView();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawView() {
        try {
            if (holder != null) {
                canvas = holder.lockCanvas();
//                canvas.drawColor(0, PorterDuff.Mode.CLEAR);//清空屏幕
                canvas.drawColor(0xFFFFFFFF);
                paint.setColor(lineColor);
                path.reset();
                path.moveTo(getWidth() / 2f - lineWidth / 2f, getHeight() / 2f);
                if (loadingState == LoadingState.DOWN) {
                    //小球在绳子上下降
                    /**        t=0.5;
                     *          cp[1].x=(cp[0].x+cp[2].x)/2;即连线中点
                     *         float c0 = (1 - t) * (1 - t);    0.25
                     *         float c1 = 2 * t * (1 - t);      0.5
                     *         float c2 = t * t;                0.25
                     *         growX = c0 * cp[0].x + c1 * cp[1].x + c2 * cp[2].x;
                     *         growY = c0 * cp[0].y + c1 * cp[1].y + c2 * cp[2].y;
                     *         cp[1].y=(growY-0.5cp[0].y)*2
                     */
                    path.rQuadTo(lineWidth / 2f, 2 * downDistance, lineWidth, 0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(getWidth() / 2f, getHeight() / 2 + downDistance - ballRadius - strokeWidth / 2,
                            ballRadius, paint);
                } else {
                    //上升 或 自由落体过程
                    path.rQuadTo(lineWidth / 2f, 2 * (maxDownDistance - upDistance),
                            lineWidth, 0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    if (loadingState == LoadingState.FREE) {
                        //自由落体过程
                        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f - freeDownDistance
                                        - ballRadius - strokeWidth / 2f,
                                ballRadius, paint);
                    } else {
                        //上升
                        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f + (maxDownDistance - upDistance)
                                        - ballRadius - strokeWidth / 2f,
                                ballRadius, paint);
                    }
                }
                paint.setColor(ballColor);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(getWidth() / 2f - lineWidth / 2f,
                        getHeight() / 2f, ballRadius, paint);
                canvas.drawCircle(getWidth() / 2f + lineWidth / 2f,
                        getHeight() / 2f, ballRadius, paint);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }

    class ShockInterpolator implements Interpolator {

        @Override
        public float getInterpolation(float input) {
            float value = (float) (1 - Math.exp(-3 * input) * Math.cos(10 * input));
//            Log.e("input", input + "  " + value);

            return value;
        }
    }
}
