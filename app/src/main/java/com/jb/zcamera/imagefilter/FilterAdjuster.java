package com.jb.zcamera.imagefilter;

import com.jb.zcamera.imagefilter.filter.GPUImageFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageHueFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageToneCurveFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageWhiteBalanceAndToneCurveFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageWhiteBalanceFilter;

/**
 * Created by chenfangyi on 17-4-6.
 */

public class FilterAdjuster {
    private final FilterAdjuster.Adjuster<? extends GPUImageFilter> adjuster;

    public FilterAdjuster(final GPUImageFilter filter) {
        if (filter instanceof GPUImageWhiteBalanceAndToneCurveFilter) {
            adjuster = new FilterAdjuster.BeautyAdjuster().filter(filter);
        } else if(filter instanceof GPUImageWhiteBalanceFilter){
            adjuster = new FilterAdjuster.WhiteBalanceAdjuster().filter(filter);
        } else if(filter instanceof GPUImageToneCurveFilter){
            adjuster = new FilterAdjuster.ToneCurveAdjuster().filter(filter);
        } else if(filter instanceof GPUImageHueFilter){
            adjuster = new FilterAdjuster.HueAdjust().filter(filter);
        } else{
            adjuster = null;
        }
    }

    public void whitening(final int percentage) {
        if (adjuster != null) {
            adjuster.whitening(percentage);
        }
    }

    public void adjustSkinColor(final int percentage) {
        if (adjuster != null) {
            adjuster.adjustSkinColor(percentage);
        }
    }

    public void adjust(final int percentage){
        if (adjuster != null) {
            adjuster.adjust(percentage);
        }
    }

    private abstract class Adjuster<T extends GPUImageFilter> {
        private T filter;

        @SuppressWarnings("unchecked")
        public FilterAdjuster.Adjuster<T> filter(final GPUImageFilter filter) {
            this.filter = (T) filter;
            return this;
        }

        public T getFilter() {
            return filter;
        }

        public abstract void whitening(int percentage);

        public abstract void adjustSkinColor(int percentage);

        /**
         * 普通的adjust方法
         * @param percentage
         */
        public abstract void adjust(int percentage);

        protected float range(final int percentage, final float start, final float end) {
            return (end - start) * percentage / 100.0f + start;
        }
    }

    private class BeautyAdjuster extends FilterAdjuster.Adjuster<GPUImageWhiteBalanceAndToneCurveFilter> {

        @Override
        public void whitening(int percentage) {
            getFilter().setTemperature(range(percentage, 4700.0f, 5700.0f));

        }

        @Override
        public void adjustSkinColor(int percentage) {
            getFilter().setCurve(range(percentage, 0.0f, 0.3f));
        }

        @Override
        public void adjust(int percentage) {

        }
    }

    private class WhiteBalanceAdjuster extends FilterAdjuster.Adjuster<GPUImageWhiteBalanceFilter> {

        @Override
        public void whitening(int percentage) {
            getFilter().setTemperature(range(percentage, 4700.0f, 5700.0f));

        }

        @Override
        public void adjustSkinColor(int percentage) {
        }

        @Override
        public void adjust(int percentage) {

        }
    }

    private class ToneCurveAdjuster extends FilterAdjuster.Adjuster<GPUImageToneCurveFilter> {

        @Override
        public void whitening(int percentage) {
        }

        @Override
        public void adjustSkinColor(int percentage) {
            getFilter().setCurve(range(percentage, 0.0f, 0.3f));
        }

        @Override
        public void adjust(int percentage) {

        }
    }

    private class HueAdjust extends FilterAdjuster.Adjuster<GPUImageHueFilter> {

        @Override
        public void whitening(int percentage) {
        }

        @Override
        public void adjustSkinColor(int percentage) {
        }

        @Override
        public void adjust(int percentage) {
            getFilter().setHue(range(percentage, 0, 360));
        }
    }
}
