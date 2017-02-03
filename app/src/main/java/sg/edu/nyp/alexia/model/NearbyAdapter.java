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

public class NearbyAdapter extends BaseAdapter implements Filterable {
    Context c;
    ArrayList<Nearby> nearbies;
    CustomFilter filter;
    ArrayList<Nearby> filterList;

    public NearbyAdapter(Context c, ArrayList<Nearby> nearbies){
        this.c = c;
        this.nearbies = nearbies;
        this.filterList = nearbies;
    }

    @Override
    public int getCount() {
        return nearbies.size();
    }

    @Override
    public Object getItem(int i) {
        return nearbies.get(i);
    }

    @Override
    public long getItemId(int i) {
        return nearbies.indexOf(getItem(i));
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
        nameTxt.setText(nearbies.get(i).getName());
        img.setImageResource(nearbies.get(i).getIcon());

        return view;
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
                ArrayList<Nearby> filters = new ArrayList<Nearby>();

                for(int i = 0; i<filterList.size(); i++){
                    if(filterList.get(i).getName().toUpperCase().contains(charSequence)){
                        Nearby nearby = filterList.get(i);

                        filters.add(nearby);
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
            nearbies=(ArrayList<Nearby>) filterResults.values;
            notifyDataSetChanged();
        }
    }
}
