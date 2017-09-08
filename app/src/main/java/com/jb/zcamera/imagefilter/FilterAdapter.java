package com.jb.zcamera.imagefilter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.pixelslab.stickerpe.R;

import java.util.List;

import static com.jb.zcamera.imagefilter.util.ImageFilterTools.FILTER_NAME_TO_DRAWABLE_RESOURCE;

/**
 * 
 * @author chenfangyi
 * 滤镜的Adapter
 *
 */
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

	public static final int ORIGINAL_FILTER_POSITION = 0;
	private int mSelectedPosition = 0;

	/**
	 * 需要显示的数据
	 */
	private List<LocalFilterBO> mData;
	private OnItemClickListener mListener;
	private LayoutInflater mInfl;
	
	public static final int MAIN_FILTER_TYPE = 1;

	public FilterAdapter(Context context, List<LocalFilterBO> data, int type) {
		this.mData = data;
		this.mInfl = ((Activity)context).getLayoutInflater();
	}

	public interface OnItemClickListener {
		void onItemClick(LocalFilterBO item, int position);
	}

	public boolean setSelectItemPosition(int selectedPosition) {
		if (selectedPosition < mData.size()) {
			if (mSelectedPosition != selectedPosition) {
				notifyItemChanged(mSelectedPosition);
				mSelectedPosition = selectedPosition;
				notifyItemChanged(selectedPosition);
				return true;
			}
		}
		return false;
	}

	public LocalFilterBO getSelectedFilterItem() {
		return mData.get(mSelectedPosition);
	}

	public LocalFilterBO getItem(int filterId) {
		return mData.get(filterId);
	}

	public void setData(List<LocalFilterBO> data){
		this.mData = data;
		notifyDataSetChanged();
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mListener = listener;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new ViewHolder(mInfl.inflate(R.layout.item_filter, null));
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, final int position) {
		final LocalFilterBO item = mData.get(position);
		holder.itemView.setSelected(mSelectedPosition == position);
		holder.filterTextView.setText(item.getName());
		if (ORIGINAL_FILTER_POSITION == position) {
			holder.filterImageView.setImageResource(R.drawable.filter_original);
		} else {
			holder.filterImageView.setImageResource(FILTER_NAME_TO_DRAWABLE_RESOURCE.get(item.getName()));
		}
		holder.itemView.setTag(position);
		holder.itemView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mListener != null) {
					mListener.onItemClick(item, (Integer)view.getTag());
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return mData.size();
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		private View itemView;
		private ImageView filterImageView;
		private TextView filterTextView;

		public ViewHolder(View itemView) {
			super(itemView);
			this.itemView = itemView;
			filterImageView = itemView.findViewById(R.id.filter_item_image);
			filterTextView = itemView.findViewById(R.id.filter_item_text);
		}
	}
}
