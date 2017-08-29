/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jb.zcamera.imagefilter.filter;

/**
 * 实时美颜滤镜（磨皮、美白、红润）
 * 
 * @author oujingwen
 *
 */
public class GPUImageRealTimeBeautyFilter extends GPUImageFilterGroup {
    GPUImageBilateralFilter bilateralFilter;
    GPUImageWhiteBalanceFilter whiteBalanceFilter;
    GPUImageToneCurveFilter toneCurveFilter;

    /**
     * Setup and Tear down
     */
    public GPUImageRealTimeBeautyFilter() {
        bilateralFilter = new GPUImageBilateralFilter();
        addFilter(bilateralFilter);
        
        toneCurveFilter = new GPUImageToneCurveFilter();
        addFilter(toneCurveFilter);
        
        whiteBalanceFilter = new GPUImageWhiteBalanceFilter(4900, 0.0f);
        addFilter(whiteBalanceFilter);
    }
    
    public void setTemperature(final float temperature) {
        whiteBalanceFilter.setTemperature(temperature);
    }
    
    public void setTint(final float tint) {
        whiteBalanceFilter.setTint(tint);
    }
    
    public void setCurve(final float curve){
    	toneCurveFilter.setCurve(curve);
    }
    

}
