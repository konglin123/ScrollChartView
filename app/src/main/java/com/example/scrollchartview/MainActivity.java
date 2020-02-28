package com.example.scrollchartview;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ScrollChartView scrollChartView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scrollChartView=findViewById(R.id.chart_view);
        initMultiGroupHistogramView();
    }


    private void initMultiGroupHistogramView() {
        Random random = new Random();
        int groupSize = random.nextInt(10) + 10;
        List<MultiGroupHistogramGroupData> groupDataList = new ArrayList<>();
        for (int i = 0; i < groupSize; i++) {
            List<MultiGroupHistogramChildData> childDataList = new ArrayList<>();
            MultiGroupHistogramGroupData groupData = new MultiGroupHistogramGroupData();
            groupData.setGroupName("第" + (i + 1) + "组");
            MultiGroupHistogramChildData childData1 = new MultiGroupHistogramChildData();
            childData1.setSuffix("%");
            childData1.setValue(random.nextInt(50) + 51);
            childDataList.add(childData1);

            MultiGroupHistogramChildData childData2 = new MultiGroupHistogramChildData();
            childData2.setSuffix("分");
            childData2.setValue(random.nextInt(50) + 51);
            childDataList.add(childData2);
            groupData.setChildDataList(childDataList);
            groupDataList.add(groupData);
        }
        scrollChartView.setDataList(groupDataList);
        int[] color1 = new int[]{Color.parseColor("#FFD100"), Color.parseColor("#FF3300")};

        int[] color2 = new int[]{Color.parseColor("#1DB890"), Color.parseColor("#4576F9")};
        scrollChartView.setHistogramColor(color1, color2);
    }
}
