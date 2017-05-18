package com.example.kimsaekwang.myapplication;

import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by 장규 on 2017-05-15.
 */

public class ViewPagerAdapter extends PagerAdapter {

    private static final String VIEWPAGERADAPTER = "ViewPagerAdapter";

    private LayoutInflater inflater;
    private ImageView pagerNum;

    private View header,header2;

    public ViewPagerAdapter(LayoutInflater inflater, ImageView pagerNum) {
        this.inflater = inflater;
        this.pagerNum = pagerNum;
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View view = null;
        if (position == 0) {
            view = header;
        }
        else if (position == 1) {
            view = header2;
        }
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

    public void setView(View view, int position){
        if(position == 0){
            header = view;
        }
        else header2 = view;

    }
}
