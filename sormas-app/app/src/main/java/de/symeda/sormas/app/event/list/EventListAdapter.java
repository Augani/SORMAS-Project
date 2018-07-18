package de.symeda.sormas.app.event.list;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import de.symeda.sormas.api.event.EventType;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.event.Event;
import de.symeda.sormas.app.core.adapter.databinding.DataBoundAdapter;
import de.symeda.sormas.app.core.adapter.databinding.DataBoundViewHolder;
import de.symeda.sormas.app.core.adapter.databinding.ISetOnListItemClickListener;
import de.symeda.sormas.app.core.adapter.databinding.OnListItemClickListener;
import de.symeda.sormas.app.databinding.RowEventListItemLayoutBinding;

public class EventListAdapter extends DataBoundAdapter<RowEventListItemLayoutBinding> implements ISetOnListItemClickListener {

    private static final String TAG = EventListAdapter.class.getSimpleName();

    private List<Event> data;
    private OnListItemClickListener mOnListItemClickListener;

    private LayerDrawable backgroundRowItem;
    private Drawable unreadListItemIndicator;

    public EventListAdapter(int rowLayout, OnListItemClickListener onListItemClickListener, List<Event> data) {
        super(rowLayout);
        this.mOnListItemClickListener = onListItemClickListener;

        if (data == null)
            this.data = new ArrayList<>();
        else
            this.data = new ArrayList<>(data);
    }

    @Override
    protected void bindItem(DataBoundViewHolder<RowEventListItemLayoutBinding> holder,
                            int position, List<Object> payloads) {

        Event record = data.get(position);
        holder.setData(record);
        holder.setOnListItemClickListener(this.mOnListItemClickListener);

        indicateEventType(holder.binding.imgEventTypeIcon, record);

        //Sync Icon
        if (record.isModifiedOrChildModified()) {
            holder.binding.imgSyncIcon.setVisibility(View.VISIBLE);
            holder.binding.imgSyncIcon.setImageResource(R.drawable.ic_sync_blue_24dp);
        } else {
            holder.binding.imgSyncIcon.setVisibility(View.GONE);
        }

        updateUnreadIndicator(holder, record);

    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateUnreadIndicator(DataBoundViewHolder<RowEventListItemLayoutBinding> holder, Event item) {
        backgroundRowItem = (LayerDrawable) ContextCompat.getDrawable(holder.context, R.drawable.background_list_activity_row);
        unreadListItemIndicator = backgroundRowItem.findDrawableByLayerId(R.id.unreadListItemIndicator);

        if (item != null) {
            if (item.isUnreadOrChildUnread()) {
                unreadListItemIndicator.setTint(holder.context.getResources().getColor(R.color.unreadIcon));
            } else {
                unreadListItemIndicator.setTint(holder.context.getResources().getColor(android.R.color.transparent));
            }
        }
    }

    public void indicateEventType(ImageView imgEventTypeIcon, Event eventRecord) {
        Resources resources = imgEventTypeIcon.getContext().getResources();
        Drawable drw = (Drawable) ContextCompat.getDrawable(imgEventTypeIcon.getContext(), R.drawable.indicator_status_circle);
        if (eventRecord.getEventType() == EventType.RUMOR) {
            drw.setColorFilter(resources.getColor(R.color.indicatorRumorEvent), PorterDuff.Mode.SRC_OVER);
        } else if (eventRecord.getEventType() == EventType.OUTBREAK) {
            drw.setColorFilter(resources.getColor(R.color.indicatorOutbreakEvent), PorterDuff.Mode.SRC_OVER);
        }

        imgEventTypeIcon.setBackground(drw);
    }

    public Event getEvent(int position) {
        if (position < 0)
            return null;

        if (position >= this.data.size())
            return null;

        return (Event) this.data.get(position);
    }

    public void addAll(List<Event> data) {
        if (data == null)
            return;

        this.data.addAll(data);
    }

    public void replaceAll(List<Event> data) {
        if (data == null)
            return;

        this.data.clear();
        this.data.addAll(data);
    }

    public void clear() {
        if (this.data == null)
            return;

        this.data.clear();
    }

    @Override
    public void setOnListItemClickListener(OnListItemClickListener onListItemClickListener) {
        this.mOnListItemClickListener = onListItemClickListener;
    }
}
