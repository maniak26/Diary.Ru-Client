package adonai.diary_browser.entities;

import java.util.List;

import workaround.PatchedTextView;

import adonai.diary_browser.R;
import adonai.diary_browser.Utils;
import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListAdapter;
import android.widget.TextView;

public class CommentListArrayAdapter implements ListAdapter
{
    private final DataSetObservable mDataSetObservable = new DataSetObservable();
    private Context ctx;
    private DiaryPage mObjects;

    public CommentListArrayAdapter(Context context, DiaryPage objects)
    {
        ctx = context;
        mObjects = objects;
    }
    
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        View view;
        Post post = getItem(pos);
        if (convertView == null)
            view = View.inflate(ctx, R.layout.post_list_item, null);
        else
            view = convertView;
        
        /* ImageButton delete = (ImageButton)view.findViewById(R.id.p_delete); */
        TextView title = (TextView) view.findViewById(R.id.post_title);
        title.setText(post.get_title());
        TextView author = (TextView) view.findViewById(R.id.post_author);
        author.setText(post.get_author());
        TextView post_date = (TextView) view.findViewById(R.id.post_date);
        post_date.setText(post.get_date());
        //PatchedTextView post_content = (PatchedTextView) view.findViewById(R.id.post_content);
        //post_content.setText(post.get_text());
        //post_content.setMovementMethod(LinkMovementMethod.getInstance());
        WebView post_contents = (WebView) view.findViewById(R.id.post_contents);
        post_contents.setWebViewClient(new Utils.DiaryWebView());
        WebSettings settings = post_contents.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptEnabled(true);
        settings.setRenderPriority(RenderPriority.HIGH);
        settings.setBlockNetworkImage(true);
        post_contents.loadDataWithBaseURL(mObjects.get_diary_URL(), post.get_content().html(), "text/html", "", null);
        
        return view;
    }
    
    public Post getItem(int pos)
    {
        return mObjects.get(pos);
    }

    public void registerDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer)
    {
        mDataSetObservable.unregisterObserver(observer);
    }

    public int getCount()
    {
        return mObjects.size();
    }

    public long getItemId(int position)
    {
    	if(mObjects.get(position).get_ID() != "")
    		return Integer.valueOf(mObjects.get(position).get_ID());
    	else
    		return -1; // комментарий
    }

    public boolean hasStableIds()
    {
        return false;
    }

    public int getItemViewType(int position)
    {
        return 0;
    }

    public int getViewTypeCount()
    {
        return 1;
    }

    public boolean isEmpty()
    {
        return mObjects.isEmpty();
    }

    public boolean areAllItemsEnabled()
    {
        return true;
    }

    public boolean isEnabled(int position)
    {
        return !mObjects.get(position).get_URL().equals("");
    }
    
    public void notifyDataSetChanged() 
    {
        mDataSetObservable.notifyChanged();
    }
    
    public void notifyDataSetInvalidated() 
    {
        mDataSetObservable.notifyInvalidated();
    }
}