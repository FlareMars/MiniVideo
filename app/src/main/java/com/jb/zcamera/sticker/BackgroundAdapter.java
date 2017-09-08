package com.jb.zcamera.sticker;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pixelslab.stickerpe.R;

import java.util.List;

/**
 *
 * 背景框的Adapter
 *
 */
public class BackgroundAdapter extends RecyclerView.Adapter<BackgroundAdapter.ViewHolder> {

	public static final int ORIGINAL_POSITION = 0;
	private int mSelectedPosition = 0;

	/**
	 * 需要显示的数据
	 */
	private List<LocalBackgroundBO> mData;
	private OnItemClickListener mListener;
	private LayoutInflater mInfl;

	public BackgroundAdapter(Context context, List<LocalBackgroundBO> data) {
		this.mData = data;
		this.mInfl = ((Activity)context).getLayoutInflater();
	}

	public interface OnItemClickListener {
		void onItemClick(LocalBackgroundBO item, int position);
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

	public LocalBackgroundBO getSelectedBackgroundItem() {
		return mData.get(mSelectedPosition);
	}

	public LocalBackgroundBO getItem(int position) {
		return mData.get(position);
	}

	public void setData(List<LocalBackgroundBO> data){
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
		final LocalBackgroundBO item = mData.get(position);
		holder.itemView.setSelected(mSelectedPosition == position);
		if (ORIGINAL_POSITION == position) {
			holder.backgroundTextView.setText(item.getName());
			holder.backgroundImageView.setImageResource(R.drawable.filter_original);
		} else {
			holder.backgroundTextView.setText("");
			holder.backgroundImageView.setImageResource(item.getResourceId());
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
		private ImageView backgroundImageView;
		private TextView backgroundTextView;

		public ViewHolder(View itemView) {
			super(itemView);
			this.itemView = itemView;
			backgroundImageView = itemView.findViewById(R.id.filter_item_image);
			backgroundImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
			backgroundTextView = itemView.findViewById(R.id.filter_item_text);
		}
	}
}
