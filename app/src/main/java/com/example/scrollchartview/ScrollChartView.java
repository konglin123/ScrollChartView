package com.example.scrollchartview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.util.List;

public class ScrollChartView extends View{
    private int width;
    private int height;
    /** 坐标轴线宽度 */
    private int coordinateAxisWidth;
    /** x轴下方文字大小 */
    private int groupNameTextSize;
    /** 柱状图组与组之间的间距 */
    private int groupInterval;
    /** 柱状图上方文字大小 */
    private int histogramValueTextSize;
    /** 柱状图和顶部的距离 */
    private int chartPaddingTop;
    /** x轴下方文字距离x轴的间距 */
    private int distanceFormGroupNameToAxis;
    /** 柱状图上方文字距离柱状图的距离 */
    private int distanceFromValueToHistogram;
    /** 柱状图最大高度 */
    private int maxHistogramHeight;
    /** 轴线画笔 */
    private Paint coordinateAxisPaint;
    /** 柱子画笔 */
    private Paint histogramPaint;
    /** 柱子上方文字画笔 */
    private Paint histogramValuePaint;
    /** x轴下方文字画笔 */
    private Paint groupNamePaint;
    /** 直方图绘制区域 */
    private Rect histogramPaintRect;
    private int coordinateAxisColor;

    private List<MultiGroupHistogramGroupData> dataList;
    /** 一组分为两个柱子，两个柱子分别为百分比和分数，这个是用来记录最大百分比和最大分数的*/
    private SparseArray<Float> childMaxValueArray;//SparseArray就是HashMap

    /** 存储组内直方图shader color，例如，每组有3个直方图，该SparseArray就存储3个相对应的shader color */
    private SparseArray<int[]> histogramShaderColorArray;

    /** 直方图表视图总宽度（第一个柱状图到最后一个柱状图的距离）*/
    private int histogramContentWidth;
    /** 一根柱子的宽度 */
    private int histogramHistogramWidth;
    /** 组内子直方图间距 */
    private int histogramInterval;
    /** 第一根柱子距离控件内左边距 */
    private int histogramPaddingStart;
    /** 最后一根柱子距离控件内右边距 */
    private int histogramPaddingEnd;
    /** 小数点位数 */
    private int histogramValueDecimalCount;
    private int histogramValueTextColor;
    private Paint.FontMetrics histogramValueFontMetrics;
    private int groupNameTextColor;
    private Paint.FontMetrics groupNameFontMetrics;

    private VelocityTracker velocityTracker;
    private Scroller scroller;
    private int minimumVelocity;
    private int maximumVelocity;
    private float lastX;

    public void setDataList(List<MultiGroupHistogramGroupData> dataList) {
        this.dataList = dataList;
        if(childMaxValueArray==null){
            childMaxValueArray=new SparseArray<>();
        }else{
            childMaxValueArray.clear();
        }
        histogramContentWidth = 0;
        for (MultiGroupHistogramGroupData groupData: dataList
             ) {
            List<MultiGroupHistogramChildData> childDataList = groupData.getChildDataList();
            if(childDataList!=null&&childDataList.size()>0){
                for (int i = 0; i < childDataList.size(); i++) {
                    histogramContentWidth+=histogramHistogramWidth+histogramInterval;
                    Float childMaxValue = childMaxValueArray.get(i);
                    if(childMaxValue==null||childMaxValue<childDataList.get(i).getValue()){
                        childMaxValueArray.put(i,childDataList.get(i).getValue());
                    }
                }
                histogramContentWidth+=groupInterval-histogramInterval;
            } 
        }
        histogramContentWidth-=groupInterval;
    }

    /**
     * 设置组内直方图颜色
     */
    public void setHistogramColor(int[]... colors) {
        if (colors != null && colors.length > 0) {
            if (histogramShaderColorArray == null) {
                histogramShaderColorArray = new SparseArray<>();
            } else {
                histogramShaderColorArray.clear();
            }
            for (int i = 0; i < colors.length; i++) {
                histogramShaderColorArray.put(i, colors[i]);
            }
        }
    }

    public ScrollChartView(Context context) {
        this(context,null);
    }

    public ScrollChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public ScrollChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray=context.obtainStyledAttributes(attrs,R.styleable.ScrollChartView);
        coordinateAxisWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_coordinateAxisWidth, DisplayUtil.dp2px(2));
        // 坐标轴线颜色
        coordinateAxisColor = typedArray.getColor(R.styleable.ScrollChartView_coordinateAxisColor, Color.parseColor("#434343"));
        // x轴下方文字颜色
        groupNameTextColor = typedArray.getColor(R.styleable.ScrollChartView_groupNameTextColor, Color.parseColor("#CC202332"));
        groupNameTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_groupNameTextSize, DisplayUtil.dp2px(15));
        groupInterval = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_groupInterval, DisplayUtil.dp2px(30));
        // 直方图数值文本颜色
        histogramValueTextSize = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_histogramValueTextSize, DisplayUtil.dp2px(12));
        chartPaddingTop = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_chartPaddingTop, DisplayUtil.dp2px(10));
        distanceFormGroupNameToAxis = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_distanceFormGroupNameToAxis, DisplayUtil.dp2px(15));
        distanceFromValueToHistogram = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_distanceFromValueToHistogram, DisplayUtil.dp2px(10));
        histogramInterval = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_histogramInterval, DisplayUtil.dp2px(10));
        histogramHistogramWidth = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_histogramHistogramWidth, DisplayUtil.dp2px(20));
        histogramValueDecimalCount = typedArray.getInt(R.styleable.ScrollChartView_histogramValueDecimalCount, 0);
        histogramPaddingStart = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_histogramPaddingStart, DisplayUtil.dp2px(15));
        histogramPaddingEnd = typedArray.getDimensionPixelSize(R.styleable.ScrollChartView_histogramPaddingEnd, DisplayUtil.dp2px(15));
        // 直方图数值文本颜色
        histogramValueTextColor = typedArray.getColor(R.styleable.ScrollChartView_histogramValueTextColor, Color.parseColor("#CC202332"));
        // 底部小组名称字体颜色
        typedArray.recycle();
        initPaint();
        init();

    }

    private void init() {
        histogramPaintRect = new Rect();
        scroller = new Scroller(getContext(), new LinearInterpolator());
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        minimumVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    private void initPaint() {
        coordinateAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coordinateAxisPaint.setStyle(Paint.Style.FILL);
        coordinateAxisPaint.setStrokeWidth(coordinateAxisWidth);
        coordinateAxisPaint.setColor(coordinateAxisColor);

        histogramPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        histogramValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        histogramValuePaint.setTextSize(histogramValueTextSize);
        histogramValuePaint.setColor(histogramValueTextColor);
        histogramValueFontMetrics = histogramValuePaint.getFontMetrics();

        groupNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groupNamePaint.setTextSize(groupNameTextSize);
        groupNamePaint.setColor(groupNameTextColor);
        groupNameFontMetrics = groupNamePaint.getFontMetrics();

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width=getMeasuredWidth();
        height=getMeasuredHeight();
        maxHistogramHeight=height-coordinateAxisWidth-groupNameTextSize
                -histogramValueTextSize-chartPaddingTop-distanceFormGroupNameToAxis
                -distanceFromValueToHistogram;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(width==0||height==0){
            return;
        }
        /** 注意这里要加上getScrollX是因为在滑动过程中使坐标轴看起来不变，其实原先的坐标轴已经被滑走了*/
        int scrollX=getScrollX();
        int axisBottom=height-groupNameTextSize-distanceFormGroupNameToAxis-coordinateAxisWidth/2;
        /** 这里不直接从0开始画，是因为从0开始画轴线画笔宽度的一半会看不见，所以从coordinateAxisWidth/2开始画全部能看见 */
        //画y轴
        canvas.drawLine(coordinateAxisWidth/2+scrollX,0,coordinateAxisWidth/2+scrollX,axisBottom,coordinateAxisPaint);
        //画x轴
        canvas.drawLine(scrollX,axisBottom,scrollX+width,axisBottom,coordinateAxisPaint);
        //画柱子
        if(dataList!=null&&dataList.size()>0){
           int xAxisOffset=histogramPaddingStart;
            for (MultiGroupHistogramGroupData groupData: dataList
                 ) {
                 List<MultiGroupHistogramChildData> childDataList = groupData.getChildDataList();
                if (childDataList != null && childDataList.size() > 0) {
                    int groupWidth=0;
                    for (int i = 0; i < childDataList.size(); i++) {
                        MultiGroupHistogramChildData childData = childDataList.get(i);
                        histogramPaintRect.left=xAxisOffset;
                        histogramPaintRect.right=histogramPaintRect.left+histogramHistogramWidth;
                        int childHistogramHeight;
                        if(childData.getValue()<=0||childMaxValueArray.get(i)<=0){
                             childHistogramHeight=0;
                        }else{
                            childHistogramHeight = (int) (childData.getValue() / childMaxValueArray.get(i) * maxHistogramHeight);
                        }
                        histogramPaintRect.top=height-childHistogramHeight-coordinateAxisWidth-groupNameTextSize-distanceFormGroupNameToAxis;
                        histogramPaintRect.bottom=histogramPaintRect.top+childHistogramHeight;
                        //设置颜色（渐变色）
                        int[] histogramShaderColor = histogramShaderColorArray.get(i);
                        LinearGradient shader=null;
                        if(histogramShaderColor!=null&&histogramShaderColor.length>0){
                            shader=getHistogramShader(histogramPaintRect.left,histogramPaintRect.right,histogramPaintRect.top,histogramPaintRect.bottom,histogramShaderColor);
                        }
                        histogramPaint.setShader(shader);
                        canvas.drawRect(histogramPaintRect,histogramPaint);
                        //画柱状图上方文字
                        String childHistogramHeightValue = StringUtil.NumericScaleByFloor(String.valueOf(childData.getValue()), histogramValueDecimalCount) + childData.getSuffix();
                        float valueTextX=xAxisOffset+(histogramHistogramWidth-histogramValuePaint.measureText(childHistogramHeightValue))/2;
                        float valueTextY=histogramPaintRect.top-distanceFromValueToHistogram+(histogramValueFontMetrics.bottom) / 2;
                        canvas.drawText(childHistogramHeightValue,valueTextX,valueTextY,histogramValuePaint);
                        int deltaX = i < childDataList.size() - 1 ? histogramHistogramWidth + histogramInterval : histogramHistogramWidth;
                        groupWidth += deltaX;
                        xAxisOffset += i == childDataList.size() - 1 ? deltaX + groupInterval : deltaX;
                    }

                    //画x轴下方文字
                    String groupName = groupData.getGroupName();
                    float groupNameTextWidth=groupNamePaint.measureText(groupName);
                    float groupNameTextX=xAxisOffset-groupWidth-groupInterval+(groupWidth-groupNameTextWidth)/2;
                    float groupNameTextY=(height - groupNameFontMetrics.bottom / 2);
                    canvas.drawText(groupName,groupNameTextX,groupNameTextY,groupNamePaint);
                }
            }
        }

    }

    private LinearGradient getHistogramShader(float x0, float y0, float x1, float y1, int[] colors) {
        return new LinearGradient(x0, y0, x1, y1, colors, null, Shader.TileMode.CLAMP);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        velocityTracker.addMovement(event);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                //惯性滑动过程中点击停止滑动
                if(!scroller.isFinished()){
                    scroller.abortAnimation();
                }
                lastX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                 int distanceX= (int) (event.getX()-lastX);
                 lastX=event.getX();
                 if(distanceX>0&&canScrollHorizontally(-1)){
                      //右滑并且可以水平滑动
                     scrollBy(-Math.min(getMaxCanScrollX(-1),distanceX),0);
                 }else if(distanceX<0&&canScrollHorizontally(1)){
                     //左滑并且可以水平滑动
                     scrollBy(Math.min(getMaxCanScrollX(1),-distanceX),0);
                 }

                break;
            case MotionEvent.ACTION_UP:
                //松开手指后惯性滑动
                 velocityTracker.computeCurrentVelocity(1000,maximumVelocity);
                int xVelocity = (int) velocityTracker.getXVelocity();
                fling(xVelocity);
                break;

            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                break;
        }
        return true;
    }

    private void fling(int xVelocity) {
        if (Math.abs(xVelocity) > minimumVelocity) {
            /**
             * 这段写的有问题，Math.abs(velocityX)不可能比maximumVelocity大
             * 因为前面velocityTracker.computeCurrentVelocity(1000, maximumVelocity)
             */
            if (Math.abs(xVelocity) > maximumVelocity) {
                xVelocity = maximumVelocity * xVelocity / Math.abs(xVelocity);
            }
            scroller.fling(getScrollX(), getScrollY(), -xVelocity, 0, 0, histogramContentWidth + histogramPaddingStart - width, 0, 0);
        }
    }

    @Override
    public void computeScroll() {
        if(scroller.computeScrollOffset()){
            scrollTo(scroller.getCurrX(),0);
        }
    }

    private void initVelocityTrackerIfNotExists() {
        //初始化速度追踪器
        if(velocityTracker==null){
            velocityTracker=VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    /**
     * 判断是否可以水平方向滑动
     * @param derection  正数左滑，负数右滑
     * @return
     */
    public boolean canScrollHorizontally(int derection){
        if(derection>0){
            return histogramContentWidth+histogramPaddingStart+histogramPaddingEnd-width-getScrollX()>0;
        }else{
            return getScrollX()>0;
        }
    }

    /**
     * 根据滑动方向获取最大可滑动距离
     * @param dereation 正数左滑，负数右滑
     * @return
     */
    public int getMaxCanScrollX(int dereation){
         if(dereation>0){
             return histogramContentWidth+histogramPaddingStart+histogramPaddingEnd-width-getScrollX()>0?histogramContentWidth+histogramPaddingStart+histogramPaddingEnd-width-getScrollX():0;
         }else if(dereation<0){
             return getScrollX();
         }
         return 0;
    }
}
