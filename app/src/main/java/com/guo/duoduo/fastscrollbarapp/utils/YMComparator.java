package com.guo.duoduo.fastscrollbarapp.utils;


import java.util.Comparator;

import com.guo.duoduo.fastscrollbarapp.entity.GridItem;


public class YMComparator implements Comparator<GridItem>
{

    @Override
    public int compare(GridItem o1, GridItem o2)
    {
        return o1.getTime().compareTo(o2.getTime());
    }

}
