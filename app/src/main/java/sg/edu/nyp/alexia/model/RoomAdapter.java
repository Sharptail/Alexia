package sg.edu.nyp.alexia.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import sg.edu.nyp.alexia.R;

/**
     * Created by Jeffry on 24/1/17.
     */

    public class RoomAdapter extends BaseAdapter implements Filterable {
        Context c;
        ArrayList<Room> rooms;
        CustomFilter filter;
        ArrayList<Room> filterList;

    public RoomAdapter(Context c, ArrayList<Room> rooms){
        this.c = c;
        this.rooms = rooms;
        this.filterList = rooms;
    }

    @Override
    public int getCount() {
        return rooms.size();
    }

    @Override
    public Object getItem(int i) {
        return rooms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return rooms.indexOf(getItem(i));
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater=(LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(view==null)
        {
            view=inflater.inflate(R.layout.room_list_model, null);
        }
        TextView nameTxt=(TextView) view.findViewById(R.id.nameTv);
        ImageView img=(ImageView) view.findViewById(R.id.imageView1);
        //SET DATA TO THEM
        nameTxt.setText(rooms.get(i).getName());
//        img.setImageResource(rooms.get(i).getImg());

        return view;
    }

    public ArrayList<Room> getFilteredRooms(){
        return rooms;
    }

    @Override
    public Filter getFilter() {
        if(filter == null)
        {
            filter=new CustomFilter();
        }
        return filter;
    }

    class CustomFilter extends Filter{

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {

            FilterResults results = new FilterResults();

            if(charSequence != null && charSequence.length() > 0){
                charSequence = charSequence.toString().toUpperCase();
                ArrayList<Room> filters = new ArrayList<Room>();

                for(int i = 0; i<filterList.size(); i++){
                    if(filterList.get(i).getName().toUpperCase().contains(charSequence)){
                        Room room = filterList.get(i);

                        filters.add(room);
                    }
                }

                results.count = filters.size();
                results.values = filters;
            }else{
                results.count = filterList.size();
                results.values = filterList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            rooms=(ArrayList<Room>) filterResults.values;
            notifyDataSetChanged();
        }
    }
}
