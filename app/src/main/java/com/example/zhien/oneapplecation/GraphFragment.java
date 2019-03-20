package com.example.zhien.oneapplecation;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

import static com.example.zhien.oneapplecation.BLEnoBC.ZAxis;

public class GraphFragment extends Fragment {

    private static final String TAG = GraphFragment.class.getSimpleName();
    private LineChart lineChart;
    private static BLEnoBC mBlenoBC;

    public static GraphFragment newInstance() {
        Bundle args = new Bundle();
        GraphFragment fragment = new GraphFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_graph, container, false);
        lineChart = view.findViewById(R.id.linearChartGraph1);
        ArrayList<String> xAxis = new ArrayList<>();
        ArrayList<Entry> XAxisAverageSecond = new ArrayList<>();
        ArrayList<Entry> YAxisAverageSecond = new ArrayList<>();
        ArrayList<Entry> ZAxisAverageSecond = new ArrayList<>();
        ArrayList<Entry> Test = new ArrayList<>();

        for (int i = 0; i < mBlenoBC.XAxis.size(); i++) {
            float averageCoordinate = Float.parseFloat(String.valueOf(mBlenoBC.XAxis.get(i)));
            XAxisAverageSecond.add(new Entry(averageCoordinate, i));
            xAxis.add(i, String.valueOf((i)));
        }
        for (int i = 0; i < mBlenoBC.YAxis.size(); i++) {
            float averageCoordinate = Float.parseFloat(String.valueOf(mBlenoBC.YAxis.get(i)));
            YAxisAverageSecond.add(new Entry(averageCoordinate, i));
        }
        for (int i = 0; i < mBlenoBC.ZAxis.size(); i++) {
            float averageCoordinate = Float.parseFloat(String.valueOf(mBlenoBC.ZAxis.get(i)));
            ZAxisAverageSecond.add(new Entry(averageCoordinate, i));
        }


        // в классе тест добавлять вручную записи, указывая полученные результат, + номер сессии

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();
        LineDataSet lineDataSet1 = new LineDataSet(XAxisAverageSecond, "X");
        lineDataSet1.setDrawCircles(false);
        lineDataSet1.setColor(Color.RED);
        lineDataSet1.setDrawValues(false);
        lineDataSets.add(lineDataSet1);
        LineDataSet lineDataSet2 = new LineDataSet(YAxisAverageSecond, "Y");
        lineDataSet2.setDrawCircles(false);
        lineDataSet2.setColor(Color.BLUE);
        lineDataSet2.setDrawValues(false);
        lineDataSets.add(lineDataSet2);
        LineDataSet lineDataSet3 = new LineDataSet(ZAxisAverageSecond, "Z");
        lineDataSet3.setDrawCircles(false);
        lineDataSet3.setColor(Color.BLACK);
        lineDataSet3.setDrawValues(false);
        lineDataSets.add(lineDataSet3);
        XAxis xl = lineChart.getXAxis();
        //  xl.setDrawLabels(false);
        YAxis yl = lineChart.getAxisLeft();
        yl.setDrawLabels(false);
        YAxis y2 = lineChart.getAxisRight();
        y2.setShowOnlyMinMax(true);
        y2.setDrawLabels(false);
        lineDataSet1.setDrawCubic(true);
        lineDataSet2.setDrawCubic(true);
        lineDataSet3.setDrawCubic(true);
        lineChart.setData(new LineData(xAxis, lineDataSets));
        lineChart.setVisibleXRangeMaximum(600);
        lineChart.setVisibleXRangeMinimum(10);
        lineChart.setScaleYEnabled(false);

        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        return view;
    }
}
