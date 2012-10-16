package adonai.diary_browser.tags;

import java.util.ArrayList;

import android.text.Spanned;

public class MoreTag extends ArrayList<Spanned>
{
    private static final long serialVersionUID = 100000L;
    
    private MoreTag parent = null;
    private ArrayList<MoreTag> children = new ArrayList<MoreTag>();
    
    public MoreTag()
    {
        super();
    }
    
    public MoreTag(MoreTag parent)
    {
        super();
        this.parent = parent;
    }
    
    public void addChild(MoreTag child)
    {
        children.add(child);
    }
    
    public MoreTag popChild()
    {
        if(children != null && !children.isEmpty())
            return children.remove(0);
        
        return null;
    }
    
    public Spanned pop()
    {
        if(!isEmpty())
            return remove(0);
        
        return null;
    }
    
    public ArrayList<MoreTag> getChildren()
    {
        return children;
    }
}