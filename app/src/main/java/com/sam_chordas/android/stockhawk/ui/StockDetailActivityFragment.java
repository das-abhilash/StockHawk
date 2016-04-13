package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class StockDetailActivityFragment extends Fragment {

    public static final String ARG_ITEM_ID = "symbol";

    public StockDetailActivityFragment() {
    }

    TextView test;
    String symbol;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stock_detail, container, false);
        test = (TextView) view.findViewById(R.id.test);
        TextView test = (TextView) view.findViewById(R.id.test);
        Bundle b = getActivity().getIntent().getExtras();
        symbol = b.getString("symbol");

        Cursor c = getActivity().getContentResolver().query(QuoteProvider.Quotes.withSymbol(symbol),
                new String[]{QuoteColumns.GRAPHVALUES},
                null, null, null);

        if (c != null) {
            c.moveToFirst();
        }

        String graphValues = c.getString(0);

        String[] GraphDate = new String[300];
        String[] GraphBid = new String[300];
        try {

            JSONArray JAquote = new JSONArray(graphValues);
            int JAquoteLength = JAquote.length();
            for (int i = 0; i < JAquoteLength; i++) {
                JSONObject bid = JAquote.getJSONObject(i);
                String Close = bid.getString("Close");
                String Date = bid.getString("Date");
                GraphBid[i] = Close;
                GraphDate[i] = Date;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        test.setText(getString(R.string.stock_performance, symbol));

        Toast.makeText(getActivity(), getString(R.string.graphDetail), Toast.LENGTH_SHORT).show();

        final GraphView line_graph = (GraphView) view.findViewById(R.id.graph);
        //  line_graph.setTitle("1 year Stock performance of: " + symbol);
       /* float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics());
        line_graph.setTitleTextSize( pixels );*/
        LineGraphSeries<DataPoint> line_series =
                null;
        DataPoint[] points = new DataPoint[250];

        for (int i = 249; i >= 0; i--) {

            points[249 - i] = new DataPoint(StringtoDate(GraphDate[i]), Double.parseDouble(GraphBid[i]));
        }

        line_series = new LineGraphSeries<DataPoint>(points);
        line_graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
        line_graph.getViewport().setScrollable(true);
        line_graph.getViewport().setScalable(true);
        line_graph.addSeries(line_series);
        line_graph.getGridLabelRenderer().setNumHorizontalLabels(3);
        line_graph.getViewport().setXAxisBoundsManual(true);
        line_series.setDrawDataPoints(false);
        line_series.setDataPointsRadius(6);
        line_graph.getViewport().setMinX(StringtoDate(GraphDate[249]).getTime());
        line_graph.getViewport().setMaxX(StringtoDate(GraphDate[0]).getTime());

        line_series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(getActivity(), getString(R.string.DataPointClicked, dataPoint), Toast.LENGTH_SHORT).show();
            }
        });
        return view;
    }

    private Date StringtoDate(String dateString) {
        SimpleDateFormat curFormater = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date dateObj = null;

        try {
            dateObj = curFormater.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateObj;
    }
}
