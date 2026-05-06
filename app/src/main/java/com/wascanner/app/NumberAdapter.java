package com.wascanner.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class NumberAdapter extends BaseAdapter {

    private final Context ctx;
    private final List<String> data;

    public NumberAdapter(Context ctx, List<String> data) {
        this.ctx = ctx;
        this.data = data;
    }

    @Override public int getCount() { return data.size(); }
    @Override public Object getItem(int p) { return data.get(p); }
    @Override public long getItemId(int p) { return p; }

    @Override
    public View getView(int pos, View convert, ViewGroup parent) {
        ViewHolder h;
        if (convert == null) {
            convert = LayoutInflater.from(ctx).inflate(R.layout.item_number, parent, false);
            h = new ViewHolder();
            h.tvNum = convert.findViewById(R.id.tvNumber);
            h.tvIdx = convert.findViewById(R.id.tvSource);
            convert.setTag(h);
        } else h = (ViewHolder) convert.getTag();

        h.tvNum.setText(data.get(pos));
        h.tvIdx.setText("#" + (pos + 1));
        return convert;
    }

    public void refresh(List<String> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    static class ViewHolder { TextView tvNum, tvIdx; }
}
