package com.jb.zcamera.sticker;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gomo.minivideo.R;
import com.jb.zcamera.filterstore.bo.LocalFilterBO;

import java.util.List;

/**
 *
 * 贴纸的Adapter
 *
 */
public class StickerAdapter extends RecyclerView.Adapter<StickerAdapter.ViewHolder> {

	private List<LocalStickerBO> mData;
	private OnItemClickListener mListener;
	private LayoutInflater mInfl;

	public StickerAdapter(Context context, List<LocalStickerBO> data) {
		this.mData = data;
		this.mInfl = ((Activity)context).getLayoutInflater();
	}

	public interface OnItemClickListener {
		void onItemClick(LocalStickerBO item, int position);
	}

	public LocalStickerBO getItem(int position) {
		return mData.get(position);
	}

	public void setData(List<LocalStickerBO> data){
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
		final LocalStickerBO item = mData.get(position);
		holder.stickerImageView.setImageResource(item.getResourceId());
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
		private ImageView stickerImageView;
		private TextView stickerTextView;

		public ViewHolder(View itemView) {
			super(itemView);
			this.itemView = itemView;
			stickerImageView = itemView.findViewById(R.id.filter_item_image);
			stickerImageView.setScaleType(ImageView.ScaleType.FIT_XY);
			stickerTextView = itemView.findViewById(R.id.filter_item_text);
			stickerTextView.setVisibility(View.INVISIBLE);
		}
	}
}
