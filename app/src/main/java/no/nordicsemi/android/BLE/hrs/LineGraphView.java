/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.BLE.hrs;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;

/**
 * This class uses external library AChartEngine to show dynamic real time line graph for HR values
 */
public class LineGraphView {
	//TimeSeries will hold the data in x,y format for single chart  
	private XYSeries mSeries = new XYSeries("Heart Rate");
	//XYSeriesRenderer is used to set the properties like chart color, style of each point, etc. of single chart
	private XYSeriesRenderer mRenderer = new XYSeriesRenderer();
	//XYMultipleSeriesDataset will contain all the TimeSeries 
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	//XYMultipleSeriesRenderer will contain all XYSeriesRenderer and it can be used to set the properties of whole Graph
	private XYMultipleSeriesRenderer mMultiRenderer = new XYMultipleSeriesRenderer();

	private static LineGraphView mInstance = null;

	/**
	 * singleton implementation of LineGraphView class
	 */
	public static synchronized LineGraphView getLineGraphView() {
		if (mInstance == null) {
			mInstance = new LineGraphView();
		}
		return mInstance;
	}

	/**
	 * This constructor will set some properties of single chart and some properties of whole graph
	 */
	public LineGraphView() {
		//add single line chart mSeries
		mDataset.addSeries(mSeries);
		//set line chart color to Black
		mRenderer.setColor(Color.RED);
		//set line chart style to square points
		mRenderer.setPointStyle(PointStyle.POINT);
		mRenderer.setFillPoints(true);
		mRenderer.setLineWidth(2.5f);
		final XYMultipleSeriesRenderer renderer = mMultiRenderer;
		//set whole graph background color to transparent color
		renderer.setBackgroundColor(Color.TRANSPARENT);
		renderer.setMargins(new int[]{20, 65, 40, 20}); // top, left, bottom, right
		renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
		renderer.setAxesColor(Color.BLACK);
		renderer.setAxisTitleTextSize(22);
		renderer.setShowGrid(true);
		renderer.setGridColor(Color.LTGRAY);
		renderer.setLabelsColor(Color.BLACK);
		renderer.setYLabelsColor(0, Color.DKGRAY);
		renderer.setYLabelsAlign(Align.RIGHT);
		renderer.setYLabelsPadding(4.0f);
		renderer.setXLabelsColor(Color.DKGRAY);
		renderer.setLabelsTextSize(20);		//标签大小
		renderer.setLegendTextSize(20);
//		renderer.setYLabels(30);			//坐标轴标签点数
//		renderer.setXLabels(20);
		renderer.setYAxisMax(3.3f);
		renderer.setYAxisMin(0);
		renderer.setXAxisMax(1000f);		//X0-200
		renderer.setXAxisMin(0);
		renderer.setPointSize(2f);			//点粗细


		//Disable zoom
		//renderer.setExternalZoomEnabled(true);//设置是否可以缩放
		renderer.setZoomInLimitY(1.5);//设置Y轴最大缩放限
		renderer.setZoomInLimitX(1.5);//设置X轴最大缩放限
		renderer.setPanLimits(new double[] {-200,3000,-5,5});
		renderer.setZoomEnabled(true, true);//设置缩放
		renderer.setPanEnabled(true, true);//设置滑动,这边是横向可以滑动,竖向滑动
		//set title to x-axis and y-axis
		renderer.setYLabels(10);
		renderer.setXLabels(10);
		renderer.setXTitle("        Time (ms)");
		renderer.setYTitle("        Volt（v）");
		renderer.addSeriesRenderer(mRenderer);
	}

	/**
	 * return graph view to activity
	 */
	public GraphicalView getView(Context context) {
		final GraphicalView graphView = ChartFactory.getLineChartView(context, mDataset, mMultiRenderer);
		return graphView;
	}

	/**
	 * add new x,y value to chart
	 */
	public void addValue(double x,double y) {
		mSeries.add(x, y);
	}

	/**
	 * clear all previous values of chart
	 */
	public void clearGraph() {
		mSeries.clear();
	}

	public double getSeriesX(int i) {
		return mSeries.getX(i);
	}

	public double getSeriesY(int i) {
		return mSeries.getY(i);
	}

	public int getXYcount() {
		return mSeries.getItemCount();
	}
}

