package com.example.kimsaekwang.myapplication;

import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by 장규 on 2017-05-15.
 */

public class ViewPagerAdapter extends PagerAdapter {

    private LayoutInflater inflater;

    public ViewPagerAdapter(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View view = null;
        if (position == 0)
            view = inflater.inflate(R.layout.activity_main_stat, null);
        else if (position == 1)
            view = inflater.inflate(R.layout.activity_info, null);
        else
            view = null;

        container.addView(view);

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {

        container.removeView((View) object);
    }


    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
