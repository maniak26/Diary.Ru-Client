package adonai.diary_browser.entities;

import java.util.List;

import adonai.diary_browser.R;
import adonai.diary_browser.R.id;
import adonai.diary_browser.R.layout;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DiaryListArrayAdapter extends ArrayAdapter<Diary>
{
    
    public DiaryListArrayAdapter(Context context, int textViewResourceId, List<Diary> objects)
    {
        super(context, textViewResourceId, objects);
    }
    
    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        View view;
        Diary diary = getItem(pos);
        if (convertView == null)
            view = View.inflate(getContext(), R.layout.diary_list_item, null);
        else
            view = convertView;
        
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(diary.getTitle());
        title.setOnClickListener((OnClickListener) getContext());
        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(diary.getAuthor());
        author.setOnClickListener((OnClickListener) getContext());
        TextView last_post = (TextView) view.findViewById(R.id.last_post);
        last_post.setText(diary.getLastPost());
        last_post.setOnClickListener((OnClickListener) getContext());
        
        return view;
    }
    
}